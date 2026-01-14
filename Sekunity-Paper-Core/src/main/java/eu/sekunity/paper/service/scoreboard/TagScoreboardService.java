package eu.sekunity.paper.service.scoreboard;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * © Copyright 14.01.2026 - 20:34 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class TagScoreboardService
{
	private static final int MAX_WEIGHT = 9999;

	private final JavaPlugin plugin;
	private final MiniMessage mm = MiniMessage.miniMessage();

	private final java.util.Map<UUID, String> teamNameByPlayer = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<UUID, String> entryByPlayer = new java.util.concurrent.ConcurrentHashMap<>();

	public TagScoreboardService(JavaPlugin plugin)
	{
		this.plugin = plugin;
	}

	public void apply(Player player, String entry, int weight, String prefixMm, String suffixMm, NamedTextColor nameColor)
	{
		Bukkit.getScheduler().runTask(plugin, () -> {
			Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

			String teamName = buildTeamName(weight, player.getUniqueId());
			Team team = board.getTeam(teamName);
			if (team == null)
				team = board.registerNewTeam(teamName);

			team.prefix(mm.deserialize(prefixMm == null ? "" : prefixMm));
			team.suffix(mm.deserialize(suffixMm == null ? "" : suffixMm));
			if (nameColor != null)
				team.color(nameColor);

			String oldTeam = teamNameByPlayer.put(player.getUniqueId(), teamName);
			String oldEntry = entryByPlayer.put(player.getUniqueId(), entry);

			if (oldTeam != null)
			{
				Team t = board.getTeam(oldTeam);
				if (t != null)
				{
					if (oldEntry != null)
						t.removeEntry(oldEntry);

					if (t.getEntries().isEmpty())
						t.unregister();
				}
			}

			if (!team.hasEntry(entry))
				team.addEntry(entry);
		});
	}

	public void applyReal(Player player, int weight, String prefixMm, String suffixMm, NamedTextColor nameColor)
	{
		apply(player, player.getName(), weight, prefixMm, suffixMm, nameColor);
	}

	public void remove(Player player)
	{
		Bukkit.getScheduler().runTask(plugin, () -> {
			Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

			UUID uuid = player.getUniqueId();
			String teamName = teamNameByPlayer.remove(uuid);
			String entry = entryByPlayer.remove(uuid);

			if (teamName == null || entry == null)
				return;

			Team team = board.getTeam(teamName);
			if (team == null)
				return;

			team.removeEntry(entry);

			if (team.getEntries().isEmpty())
				team.unregister();
		});
	}

	private static String buildTeamName(int weight, UUID uuid)
	{
		int w = Math.max(0, Math.min(MAX_WEIGHT, weight));
		int inv = MAX_WEIGHT - w;

		String u = uuid.toString().replace("-", "");
		String shortU = u.substring(0, 12);

		return "T_" + String.format("%04d", inv) + "_" + shortU;
	}
}
