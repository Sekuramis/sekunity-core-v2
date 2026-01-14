package eu.sekunity.paper.commands;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import eu.sekunity.api.service.nick.NickService;
import eu.sekunity.paper.integration.luckperms.NickTabFormat;
import eu.sekunity.paper.integration.luckperms.TagMeta;
import eu.sekunity.paper.integration.luckperms.TagMetaProvider;
import eu.sekunity.paper.integration.luckperms.TagSuffixProvider;
import eu.sekunity.paper.integration.nick.NickRefresher;
import eu.sekunity.paper.service.scoreboard.TagScoreboardService;

/**
 * © Copyright 14.01.2026 - 19:53 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class CommandNick implements CommandExecutor
{
	private static final MiniMessage MM = MiniMessage.miniMessage();

	private static final String PREFIX = "<gradient:dark_purple:light_purple><bold>NickSystem</bold></gradient> <dark_gray>»</dark_gray> <gray>";

	private final Plugin plugin;
	private final NickService service;
	private final NickRefresher refresher;

	private final TagScoreboardService scoreboardService;
	private final TagMetaProvider metaProvider;
	private final TagSuffixProvider suffixProvider;

	public CommandNick(
			Plugin plugin,
			NickService service,
			NickRefresher refresher,
			TagScoreboardService scoreboardService,
			TagMetaProvider metaProvider,
			TagSuffixProvider suffixProvider
	)
	{
		this.plugin = plugin;
		this.service = service;
		this.refresher = refresher;
		this.scoreboardService = scoreboardService;
		this.metaProvider = metaProvider;
		this.suffixProvider = suffixProvider;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!(sender instanceof Player p))
		{
			send(sender, PREFIX + "<red>Dieser Befehl ist nur für Spieler.</red>");
			return true;
		}

		UUID uuid = p.getUniqueId();

		if (args.length >= 2 && args[0].equalsIgnoreCase("keepnick") && args[1].equalsIgnoreCase("off"))
		{
			service.keepNickOff(uuid)
					.thenRun(() -> send(p, PREFIX + "KeepNick <red>deaktiviert</red><gray>.</gray>"));
			return true;
		}

		if (args.length >= 1 && args[0].equalsIgnoreCase("keepnick"))
		{
			service.keepNickOn(uuid).thenAccept(opt -> {
				if (opt.isEmpty())
				{
					send(p, PREFIX + "<red>Der Nick-Pool ist aktuell leer.</red>");
					return;
				}

				var id = opt.get();

				Bukkit.getScheduler().runTask(plugin, () -> {
					refresher.applyNick(p, id);
					applyNickedScoreboard(p, id.fakeName());

					send(p, PREFIX + "KeepNick <green>aktiviert</green> <dark_gray>(</dark_gray><white>"
							+ escape(id.fakeName()) + "</white><dark_gray>)</dark_gray>");
				});
			});
			return true;
		}

		service.get(uuid).thenAccept(opt -> {

			if (opt.isPresent() && opt.get().enabled())
			{
				service.disable(uuid).thenRun(() ->
						Bukkit.getScheduler().runTask(plugin, () -> {
							refresher.removeNick(p);
							send(p, PREFIX + "Nick <red>deaktiviert</red><gray>.</gray>");
						})
				);

				applyRealScoreboard(p);
				return;
			}

			service.enableRandomOrKeep(uuid).thenAccept(nickOpt -> {
				if (nickOpt.isEmpty())
				{
					send(p, PREFIX + "<red>Der Nick-Pool ist aktuell leer.</red>");
					return;
				}

				var id = nickOpt.get();

				Bukkit.getScheduler().runTask(plugin, () -> {
					refresher.applyNick(p, id);
					applyNickedScoreboard(p, id.fakeName());

					send(p, PREFIX + "Du spielst jetzt als <light_purple>"
							+ escape(id.fakeName()) + "</light_purple><gray>.</gray>");
				});
			});
		});

		return true;
	}

	private void applyNickedScoreboard(Player p, String fakeName)
	{
		String prefix;
		NamedTextColor color;

		if (p.hasPermission("sekunity.nick.premium"))
		{
			prefix = "<gold>";
			color = NamedTextColor.GOLD;
		}
		else
		{
			prefix = "<gray>";
			color = NamedTextColor.GRAY;
		}

		scoreboardService.apply(
				p,
				fakeName,
				0,
				prefix,
				"",
				color
		);
	}

	private void applyRealScoreboard(Player p)
	{
		UUID uuid = p.getUniqueId();

		metaProvider.get(uuid)
				.thenCombine(suffixProvider.suffixMm(uuid), (meta, suffix) -> new Object[]{meta, suffix})
				.thenAccept(arr -> Bukkit.getScheduler().runTask(plugin, () -> {
					TagMeta meta = (TagMeta) arr[0];
					String suffix = (String) arr[1];
					if (suffix == null) suffix = "";

					scoreboardService.apply(
							p,
							p.getName(),
							meta.weight(),
							meta.prefixMm(),
							suffix,
							meta.nameColor()
					);
				}));
	}

	private static void send(CommandSender sender, String mm)
	{
		sender.sendMessage(MM.deserialize(mm));
	}

	private static String escape(String input)
	{
		if (input == null)
			return "";
		return input.replace("<", "\\<").replace(">", "\\>");
	}
}
