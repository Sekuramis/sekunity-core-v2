package eu.sekunity.paper.commands.admin;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import eu.sekunity.api.service.nick.NickPoolImporter;

/**
 * © Copyright 12.01.2026 - 19:13 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class AdminCommandNickPool implements CommandExecutor, TabCompleter
{
	private final JavaPlugin plugin;
	private final NickPoolImporter importer;

	public AdminCommandNickPool(JavaPlugin plugin, NickPoolImporter importer)
	{
		this.plugin = plugin;
		this.importer = importer;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!sender.hasPermission("sekunity.nickpool.admin"))
		{
			sender.sendMessage("Keine Rechte.");
			return true;
		}

		if (args.length < 1)
		{
			sender.sendMessage("Usage: /nickpool import <uuid> [fakeName]");
			return true;
		}

		String sub = args[0].toLowerCase();
		if (!sub.equals("import"))
		{
			sender.sendMessage("Unbekannt. Usage: /nickpool import <uuid> [fakeName]");
			return true;
		}

		if (args.length < 2)
		{
			sender.sendMessage("Usage: /nickpool import <uuid> [fakeName]");
			return true;
		}

		UUID uuid;
		try
		{
			uuid = UUID.fromString(args[1]);
		}
		catch (Exception e)
		{
			sender.sendMessage("Ungültige UUID: " + args[1]);
			return true;
		}

		String fakeName = args.length >= 3 ? args[2] : "";

		sender.sendMessage("Import läuft...");

		importer.importFromUuid(uuid, fakeName).thenAccept(id -> {
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (id == null || id <= 0)
				{
					sender.sendMessage("Import fehlgeschlagen (kein Pool-Eintrag erstellt).");
					return;
				}
				sender.sendMessage("Import OK. Pool-ID: " + id);
			});
		}).exceptionally(ex -> {
			Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("Import Fehler: " + ex.getMessage()));
			return null;
		});

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
	{
		if (!sender.hasPermission("sekunity.nickpool.admin"))
			return List.of();

		if (args.length == 1)
			return List.of("import");

		if (args.length == 2 && "import".equalsIgnoreCase(args[0]))
			return List.of("<uuid>");

		if (args.length == 3 && "import".equalsIgnoreCase(args[0]))
			return List.of("[fakeName]");

		return List.of();
	}
}
