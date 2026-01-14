package eu.sekunity.paper.listener;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import eu.sekunity.api.service.nick.NickService;
import eu.sekunity.paper.integration.luckperms.TagMeta;
import eu.sekunity.paper.integration.luckperms.TagMetaProvider;
import eu.sekunity.paper.integration.luckperms.TagSuffixProvider;

/**
 * © Copyright 14.01.2026 - 21:25 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class PlayerChatListener implements Listener
{
	private static final MiniMessage MM = MiniMessage.miniMessage();

	private static final String SEP = " <dark_gray>»</dark_gray> <white>";

	private static final String NICK_PLAYER_PREFIX = "<gray>";
	private static final NamedTextColor NICK_PLAYER_COLOR = NamedTextColor.GRAY;

	private static final String NICK_PREMIUM_PREFIX = "<gold>";
	private static final NamedTextColor NICK_PREMIUM_COLOR = NamedTextColor.GOLD;

	private final JavaPlugin plugin;
	private final NickService nickService;
	private final TagMetaProvider metaProvider;
	private final TagSuffixProvider suffixProvider;

	public PlayerChatListener(JavaPlugin plugin, NickService nickService, TagMetaProvider metaProvider, TagSuffixProvider suffixProvider)
	{
		this.plugin = plugin;
		this.nickService = nickService;
		this.metaProvider = metaProvider;
		this.suffixProvider = suffixProvider;
	}

	@EventHandler
	public void onChat(AsyncChatEvent event)
	{
		event.setCancelled(true);

		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		Component msg = event.message();

		nickService.get(uuid).thenAccept(optNick -> {

			boolean nicked = optNick.isPresent() && optNick.get().enabled();
			String displayName = nicked ? optNick.get().fakeName() : player.getName();

			if (nicked)
			{
				boolean premium = player.hasPermission("sekunity.nick.premium");

				String prefix = premium ? NICK_PREMIUM_PREFIX : NICK_PLAYER_PREFIX;
				NamedTextColor color = premium ? NICK_PREMIUM_COLOR : NICK_PLAYER_COLOR;

				suffixProvider.suffixMm(uuid).thenAccept(suffix ->
						Bukkit.getScheduler().runTask(plugin, () -> {
							String sfx = suffix == null ? "" : suffix;

							Component out = Component.empty()
									.append(MM.deserialize(prefix))
									.append(Component.text(displayName, color))
									.append(sfx.isEmpty() ? Component.empty() : MM.deserialize(sfx))
									.append(MM.deserialize(SEP))
									.append(msg);

							Bukkit.broadcast(out);
						})
				);

				return;
			}

			metaProvider.get(uuid)
					.thenCombine(suffixProvider.suffixMm(uuid), (meta, suffix) -> new Object[]{meta, suffix})
					.thenAccept(arr -> Bukkit.getScheduler().runTask(plugin, () -> {
						TagMeta meta = (TagMeta) arr[0];
						String suffix = (String) arr[1];
						if (suffix == null) suffix = "";

						Component out = Component.empty()
								.append(MM.deserialize(meta.prefixMm() == null ? "" : meta.prefixMm()))
								.append(Component.text(displayName, meta.nameColor()))
								.append(suffix.isEmpty() ? Component.empty() : MM.deserialize(suffix))
								.append(MM.deserialize(SEP))
								.append(msg);

						Bukkit.broadcast(out);
					}));
		});
	}
}
