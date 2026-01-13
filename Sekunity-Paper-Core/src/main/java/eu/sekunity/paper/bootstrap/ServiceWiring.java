package eu.sekunity.paper.bootstrap;

import java.util.Optional;

import org.bukkit.plugin.java.JavaPlugin;

import com.github.retrooper.packetevents.PacketEvents;

import eu.sekunity.api.database.Database;
import eu.sekunity.api.service.NickService;
import eu.sekunity.paper.commands.admin.AdminCommandNickPool;
import eu.sekunity.paper.infra.async.AsyncExecutor;
import eu.sekunity.paper.infra.async.AsyncExecutorImpl;
import eu.sekunity.paper.infra.config.CoreConfig;
import eu.sekunity.paper.infra.config.CoreConfigLoader;
import eu.sekunity.paper.infra.database.DatabaseFactory;
import eu.sekunity.paper.infra.database.Migrations;
import eu.sekunity.paper.infra.shutdown.ShutdownHooks;
import eu.sekunity.paper.integration.luckperms.LuckPermsAdapter;
import eu.sekunity.paper.integration.luckperms.LuckPermsAdapterImpl;
import eu.sekunity.paper.integration.nick.NickRefresher;
import eu.sekunity.paper.service.nick.MojangSkinImporter;
import eu.sekunity.paper.service.nick.NickCache;
import eu.sekunity.paper.service.nick.NickPacketListener;
import eu.sekunity.paper.service.nick.NickPoolImporter;
import eu.sekunity.paper.service.nick.NickPoolRepository;
import eu.sekunity.paper.service.nick.NickRepository;
import eu.sekunity.paper.service.nick.NickServiceImpl;
import eu.sekunity.paper.service.nick.NickVisibility;
import eu.sekunity.paper.service.profile.ProfileRepository;
import eu.sekunity.paper.service.profile.ProfileServiceImpl;

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
	private static final long NICK_LEASE_MILLIS = 10 * 60 * 1000L;

	private ServiceWiring() {}

	public static WiringResult create(JavaPlugin plugin)
	{
		CoreConfig config = CoreConfigLoader.load(plugin);

		AsyncExecutor async = new AsyncExecutorImpl("sekunity-core-async", config.async().threads());

		var dbOpt = DatabaseFactory.create(config, async.executor());
		runMigrations(dbOpt);

		registerPacketEvents();

		NickCache nickCache = new NickCache();
		registerNickPacketListener(nickCache, plugin, async);

		NickPoolRepository nickPoolRepo = new NickPoolRepository(dbOpt);
		NickRepository nickRepo = new NickRepository(dbOpt);

		NickRefresher nickRefresher = new NickRefresher(plugin);

		NickService nickService = new NickServiceImpl(
				nickRepo,
				nickPoolRepo,
				nickCache,
				NICK_LEASE_MILLIS,
				nickRefresher
		);

		NickPoolImporter poolImporter = createNickPoolImporter(async, nickPoolRepo);
		registerNickPoolCommand(plugin, poolImporter);

		var profileService = createProfileService(dbOpt, async);

		var services = new SekunityServicesImpl(nickService, profileService);

		var provider = new SekunityPaperProvider(services, new PaperApiImpl(plugin));

		var shutdown = new ShutdownHooks(async, dbOpt, nickCache);

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

	private static void registerNickPacketListener(NickCache nickCache, JavaPlugin plugin, AsyncExecutor async)
	{
		LuckPermsAdapter luckPerms = new LuckPermsAdapterImpl(async.executor());
		NickVisibility visibility = new NickVisibility(luckPerms);

		PacketEvents.getAPI().getEventManager().registerListener(new NickPacketListener(plugin, nickCache, visibility));
	}

	private static NickPoolImporter createNickPoolImporter(AsyncExecutor async, NickPoolRepository nickPoolRepo)
	{
		MojangSkinImporter skinImporter = new MojangSkinImporter(async.executor());
		return new NickPoolImporter(skinImporter, nickPoolRepo, async.executor());
	}

	private static void registerNickPoolCommand(JavaPlugin plugin, NickPoolImporter poolImporter)
	{
		var cmd = plugin.getCommand("nickpool");
		if (cmd == null)
			return;

		var handler = new AdminCommandNickPool(plugin, poolImporter);
		cmd.setExecutor(handler);
		cmd.setTabCompleter(handler);
	}

	private static ProfileServiceImpl createProfileService(Optional<Database> dbOpt, AsyncExecutor async)
	{
		ProfileRepository profileRepo = new ProfileRepository(dbOpt, async.executor());
		return new ProfileServiceImpl(profileRepo);
	}
}


