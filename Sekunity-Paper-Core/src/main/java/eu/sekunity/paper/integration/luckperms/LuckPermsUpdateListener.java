package eu.sekunity.paper.integration.luckperms;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.format.NamedTextColor;

import eu.sekunity.api.service.nick.NickIdentity;
import eu.sekunity.api.service.nick.NickService;
import eu.sekunity.paper.service.scoreboard.TagScoreboardService;

/**
 * © Copyright 14.01.2026 - 21:31 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class LuckPermsUpdateListener
{
	private final JavaPlugin plugin;
	private final LuckPermsAdapterImpl lpAdapter;
	private final TagScoreboardService scoreboardService;
	private final TagMetaProvider metaProvider;
	private final TagSuffixProvider suffixProvider;
	private final NickService nickService;


	public LuckPermsUpdateListener(
			JavaPlugin plugin,
			LuckPermsAdapterImpl lpAdapter,
			TagScoreboardService scoreboardService,
			TagMetaProvider metaProvider,
			TagSuffixProvider suffixProvider,
			NickService nickService
	)
	{
		this.plugin = plugin;
		this.lpAdapter = lpAdapter;
		this.scoreboardService = scoreboardService;
		this.metaProvider = metaProvider;
		this.suffixProvider = suffixProvider;
		this.nickService = nickService;
	}

	public void register()
	{
		var lp = net.luckperms.api.LuckPermsProvider.get();
		var bus = lp.getEventBus();

		bus.subscribe(plugin, net.luckperms.api.event.user.UserDataRecalculateEvent.class, event -> {
			UUID uuid = event.getUser().getUniqueId();
			lpAdapter.invalidate(uuid);

			Player p = Bukkit.getPlayer(uuid);
			if (p == null)
				return;

			updatePlayer(p);
		});

		bus.subscribe(plugin, net.luckperms.api.event.group.GroupDataRecalculateEvent.class, event -> {
			for (Player p : Bukkit.getOnlinePlayers())
			{
				lpAdapter.invalidate(p.getUniqueId());
				updatePlayer(p);
			}
		});
	}

	private void updatePlayer(Player player)
	{
		UUID uuid = player.getUniqueId();

		nickService.get(uuid).thenAccept(optNick -> {

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

				Bukkit.getScheduler().runTask(plugin, () ->
						scoreboardService.apply(player, id.fakeName(), 0, prefix, "", color)
				);

				return;
			}

			metaProvider.get(uuid)
					.thenCombine(suffixProvider.suffixMm(uuid), (meta, suffix) -> new Object[]{meta, suffix})
					.thenAccept(arr -> Bukkit.getScheduler().runTask(plugin, () -> {
						TagMeta meta = (TagMeta) arr[0];
						String suffix = (String) arr[1];
						if (suffix == null)
							suffix = "";

						scoreboardService.apply(player, player.getName(), meta.weight(), meta.prefixMm(), suffix, meta.nameColor());

						plugin.getLogger().info("TAB update " + player.getName()
								+ " weight=" + meta.weight()
								+ " prefix='" + meta.prefixMm() + "'"
								+ " suffix='" + suffix + "'"
								+ " nameColor='" + meta.nameColor() + "'");
					}))
					.exceptionally(ex -> {
						plugin.getLogger().severe("Failed to update TAB tag for " + player.getName() + ": " + ex.getClass().getSimpleName() + " " + ex.getMessage());
						return null;
					});
		});
	}

}
