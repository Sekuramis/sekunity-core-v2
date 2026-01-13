package eu.sekunity.test;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import eu.sekunity.api.SekunityAPI;
import eu.sekunity.api.service.NickIdentity;
/**
 * © Copyright 11.01.2026 - 18:47 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class SekunityTestPlugin extends JavaPlugin
{
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!(sender instanceof Player player))
		{
			sender.sendMessage("Nur ingame.");
			return true;
		}

		return switch (label.toLowerCase())
		{
			case "nickme" -> nickMe(player);
			case "mynick" -> myNick(player);
			case "unnickme" -> unNickMe(player);
			default -> false;
		};
	}

	private boolean nickMe(Player player)
	{
		UUID uuid = player.getUniqueId();

		SekunityAPI.nickService().nickPlayer(uuid).thenAccept(opt -> {
			SekunityAPI.paper().syncExecutor().execute(() -> {
				if (opt.isEmpty())
				{
					player.sendMessage("Nick fehlgeschlagen: Kein Pool-Eintrag frei.");
					return;
				}

				NickIdentity id = opt.get();
				player.sendMessage("§5Dein Nickname ist: §4" + id.fakeName());
				player.sendMessage("§5Deine FakeUUID ist: §4" + id.fakeUuid());
			});
		}).exceptionally(ex -> {
			SekunityAPI.paper().syncExecutor().execute(() ->
					player.sendMessage("§cNick fehlgeschlagen: " + ex.getMessage())
			);
			return null;
		});

		player.sendMessage("§7Nick wird gesetzt...");
		return true;
	}

	private boolean myNick(Player player)
	{
		UUID uuid = player.getUniqueId();

		SekunityAPI.nickService().identity(uuid).thenAccept(opt -> {
			SekunityAPI.paper().syncExecutor().execute(() -> {
				if (opt.isEmpty())
				{
					player.sendMessage("§cKein Nick-Eintrag.");
					return;
				}

				NickIdentity id = opt.get();

				if (!id.enabled())
				{
					player.sendMessage("§7Nick: §cdeaktiviert");
					player.sendMessage("§5Letzter Nick war: §4" + id.fakeName());
					player.sendMessage("§5FakeUUID: §4" + id.fakeUuid());
					return;
				}

				player.sendMessage("§7Nick: §aaktiv");
				player.sendMessage("§5Name: §4" + id.fakeName());
				player.sendMessage("§5FakeUUID: §7" + id.fakeUuid());
			});
		}).exceptionally(ex -> {
			SekunityAPI.paper().syncExecutor().execute(() ->
					player.sendMessage("§c§lAbfrage fehlgeschlagen: " + ex.getMessage())
			);
			return null;
		});

		return true;
	}

	private boolean unNickMe(Player player)
	{
		UUID uuid = player.getUniqueId();

		SekunityAPI.nickService().unnickPlayer(uuid).thenRun(() -> {
			SekunityAPI.paper().syncExecutor().execute(() ->
					player.sendMessage("§7Unnick: §aOK")
			);
		}).exceptionally(ex -> {
			SekunityAPI.paper().syncExecutor().execute(() ->
					player.sendMessage("§cUnnick fehlgeschlagen: §4" + ex.getMessage())
			);
			return null;
		});

		player.sendMessage("§7Unnick läuft...");
		return true;
	}
}

