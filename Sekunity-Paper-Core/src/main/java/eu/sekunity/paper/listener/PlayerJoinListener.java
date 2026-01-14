package eu.sekunity.paper.listener;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.format.NamedTextColor;

import eu.sekunity.api.service.nick.NickIdentity;
import eu.sekunity.api.service.nick.NickService;
import eu.sekunity.paper.integration.luckperms.TagMeta;
import eu.sekunity.paper.integration.luckperms.TagMetaProvider;
import eu.sekunity.paper.integration.luckperms.TagSuffixProvider;
import eu.sekunity.paper.service.scoreboard.TagScoreboardService;

/**
 * © Copyright 14.01.2026 - 18:38 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class PlayerJoinListener implements Listener
{
	private final NickService service;
	private final JavaPlugin plugin;
	private final TagScoreboardService scoreboardService;
	private final TagMetaProvider metaProvider;
	private final TagSuffixProvider suffixProvider;

	public PlayerJoinListener(NickService service, JavaPlugin plugin, TagScoreboardService scoreboardService, TagMetaProvider metaProvider, TagSuffixProvider suffixProvider)
	{
		this.service = service;
		this.plugin = plugin;
		this.scoreboardService = scoreboardService;
		this.metaProvider = metaProvider;
		this.suffixProvider = suffixProvider;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		service.registerIfAbsent(uuid);

		service.get(uuid).thenAccept(optNick -> {

			boolean nicked = optNick.isPresent() && optNick.get().enabled();

			if (nicked)
			{
				NickIdentity id = optNick.get();

				String prefix = player.hasPermission("sekunity.nick.premium")
						? "<gold>"
						: "<gray>";

				NamedTextColor color = player.hasPermission("sekunity.nick.premium")
						? NamedTextColor.GOLD
						: NamedTextColor.GRAY;

				Bukkit.getScheduler().runTask(plugin, () -> {
					scoreboardService.apply(player, id.fakeName(), 0, prefix, "", color);

					plugin.getLogger().info("TAB apply(nicked) " + player.getName()
							+ " fakeName=" + id.fakeName()
							+ " prefix='" + prefix + "'"
							+ " nameColor='" + color + "'");
				});

				return;
			}

			metaProvider.get(uuid)
					.thenCombine(suffixProvider.suffixMm(uuid), JoinTag::new)
					.thenAccept(tag -> Bukkit.getScheduler().runTask(plugin, () -> {
						String suffix = tag.suffix == null ? "" : tag.suffix;

						scoreboardService.apply(player, player.getName(), tag.meta.weight(), tag.meta.prefixMm(), suffix, tag.meta.nameColor());

						plugin.getLogger().info("TAB apply " + player.getName()
								+ " weight=" + tag.meta.weight()
								+ " prefix='" + tag.meta.prefixMm() + "'"
								+ " suffix='" + suffix + "'"
								+ " nameColor='" + tag.meta.nameColor() + "'");
					}))
					.exceptionally(ex -> {
						plugin.getLogger().severe("Failed to apply TAB tag for " + player.getName() + ": " + ex.getClass().getSimpleName() + " " + ex.getMessage());
						return null;
					});
		});

		event.joinMessage(null);
	}


	@EventHandler
	public void onQuit(PlayerQuitEvent event)
	{
		scoreboardService.remove(event.getPlayer());
		event.quitMessage(null);
	}

	private static final class JoinTag
	{
		private final TagMeta meta;
		private final String suffix;

		private JoinTag(TagMeta meta, String suffix)
		{
			this.meta = meta;
			this.suffix = suffix;
		}
	}
}
