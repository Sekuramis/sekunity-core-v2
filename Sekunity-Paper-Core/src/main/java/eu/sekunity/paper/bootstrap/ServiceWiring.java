package eu.sekunity.paper.bootstrap;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.retrooper.packetevents.PacketEvents;

import eu.sekunity.api.database.Database;
import eu.sekunity.api.service.nick.MojangSkinImporter;
import eu.sekunity.api.service.nick.NickCache;
import eu.sekunity.api.service.nick.NickPoolImporter;
import eu.sekunity.api.service.nick.NickPoolRepository;
import eu.sekunity.api.service.nick.NickRepository;
import eu.sekunity.api.service.nick.NickService;
import eu.sekunity.paper.commands.CommandNick;
import eu.sekunity.paper.commands.admin.AdminCommandNickPool;
import eu.sekunity.paper.infra.async.AsyncExecutor;
import eu.sekunity.paper.infra.async.AsyncExecutorImpl;
import eu.sekunity.paper.infra.config.CoreConfig;
import eu.sekunity.paper.infra.config.CoreConfigLoader;
import eu.sekunity.paper.infra.database.DatabaseFactory;
import eu.sekunity.paper.infra.database.Migrations;
import eu.sekunity.paper.infra.shutdown.ShutdownHooks;
import eu.sekunity.paper.integration.luckperms.LuckPermsAdapterImpl;
import eu.sekunity.paper.integration.luckperms.LuckPermsTagMetaProvider;
import eu.sekunity.paper.integration.luckperms.LuckPermsUpdateListener;
import eu.sekunity.paper.integration.luckperms.TagMetaProvider;
import eu.sekunity.paper.integration.luckperms.TagSuffixProvider;
import eu.sekunity.paper.integration.nick.NickRefresher;
import eu.sekunity.paper.listener.PlayerChatListener;
import eu.sekunity.paper.listener.PlayerJoinListener;
import eu.sekunity.paper.service.nick.NickPacketListener;
import eu.sekunity.paper.service.nick.NickVisibility;
import eu.sekunity.paper.service.profile.ProfileRepository;
import eu.sekunity.paper.service.profile.ProfileServiceImpl;
import eu.sekunity.paper.service.scoreboard.TagScoreboardService;

/**
 * © Copyright 11.01.2026 - 17:14 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ServiceWiring
{
	private ServiceWiring()
	{
	}

	public static WiringResult create(JavaPlugin plugin)
	{
		CoreConfig config = CoreConfigLoader.load(plugin);

		AsyncExecutor async = new AsyncExecutorImpl("sekunity-core-async", config.async().threads());

		var dbOpt = DatabaseFactory.create(config, async.executor());
		runMigrations(dbOpt);

		registerPacketEvents();

		NickPoolRepository nickPoolRepo = new NickPoolRepository(dbOpt);
		NickRepository nickRepo = new NickRepository(dbOpt);

		NickCache nickCache = new NickCache();
		NickService nickService = new NickService(nickRepo, nickCache);

		LuckPermsAdapterImpl luckPerms = new LuckPermsAdapterImpl(async.executor());
		NickVisibility visibility = new NickVisibility(luckPerms);

		TagScoreboardService scoreboard = new TagScoreboardService(plugin);
		TagSuffixProvider suffixProvider = uuid -> CompletableFuture.completedFuture("");
		TagMetaProvider metaProvider = new LuckPermsTagMetaProvider(luckPerms);

		registerNickPacketListener(plugin, nickCache, visibility);
		registerPlayerListeners(plugin, nickService, scoreboard, metaProvider, suffixProvider);
		new LuckPermsUpdateListener(plugin, luckPerms, scoreboard, metaProvider, suffixProvider, nickService).register();

		NickPoolImporter poolImporter = createNickPoolImporter(async, nickPoolRepo);
		NickRefresher nickRefresher = new NickRefresher(plugin);

		var profileService = createProfileService(dbOpt, async);

		var services = new SekunityServicesImpl(nickService, profileService);
		var provider = new SekunityPaperProvider(services, new PaperApiImpl(plugin));

		var shutdown = new ShutdownHooks(async, dbOpt, nickCache);

		registerNickCommands(plugin, poolImporter, nickService, nickRefresher, scoreboard, metaProvider, suffixProvider);

		return new WiringResult(provider, shutdown);
	}

	private static void runMigrations(Optional<Database> dbOpt)
	{
		Migrations.run(dbOpt).join();
	}

	private static void registerPacketEvents()
	{
		PacketEvents.getAPI();
	}

	private static void registerNickPacketListener(JavaPlugin plugin, NickCache cache, NickVisibility visibility)
	{
		PacketEvents.getAPI().getEventManager().registerListener(new NickPacketListener(plugin, cache, visibility));
	}

	private static void registerPlayerListeners(
			JavaPlugin plugin,
			NickService service,
			TagScoreboardService scoreboard,
			TagMetaProvider metaProvider,
			TagSuffixProvider suffixProvider
	)
	{
		Bukkit.getPluginManager().registerEvents(
				new PlayerJoinListener(service, plugin, scoreboard, metaProvider, suffixProvider),
				plugin
		);

		Bukkit.getPluginManager().registerEvents(
				new PlayerChatListener(plugin, service, metaProvider, suffixProvider),
				plugin
		);
	}

	private static NickPoolImporter createNickPoolImporter(AsyncExecutor async, NickPoolRepository nickPoolRepo)
	{
		MojangSkinImporter skinImporter = new MojangSkinImporter(async.executor());
		return new NickPoolImporter(skinImporter, nickPoolRepo, async.executor());
	}

	private static void registerNickCommands(
			JavaPlugin plugin,
			NickPoolImporter poolImporter,
			NickService nickService,
			NickRefresher nickRefresher,
			TagScoreboardService scoreboardService,
			TagMetaProvider metaProvider,
			TagSuffixProvider suffixProvider
	)
	{
		var poolCmd = plugin.getCommand("nickpool");
		if (poolCmd != null)
		{
			var handler = new AdminCommandNickPool(plugin, poolImporter);
			poolCmd.setExecutor(handler);
			poolCmd.setTabCompleter(handler);
		}

		var nickCmd = plugin.getCommand("nick");
		if (nickCmd != null)
		{
			var handler = new CommandNick(plugin, nickService, nickRefresher, scoreboardService, metaProvider, suffixProvider);
			nickCmd.setExecutor(handler);
		}
		else
		{
			plugin.getLogger().severe("Command /nick nicht gefunden (plugin.yml?)");
		}
	}

	private static ProfileServiceImpl createProfileService(Optional<Database> dbOpt, AsyncExecutor async)
	{
		ProfileRepository profileRepo = new ProfileRepository(dbOpt, async.executor());
		return new ProfileServiceImpl(profileRepo);
	}
}
