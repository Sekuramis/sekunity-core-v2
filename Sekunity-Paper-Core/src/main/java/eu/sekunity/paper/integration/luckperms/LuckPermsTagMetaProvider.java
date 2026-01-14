package eu.sekunity.paper.integration.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * © Copyright 14.01.2026 - 20:55 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class LuckPermsTagMetaProvider implements TagMetaProvider
{
	private final LuckPermsAdapter lp;

	public LuckPermsTagMetaProvider(LuckPermsAdapter lp)
	{
		this.lp = lp;
	}

	@Override
	public CompletableFuture<TagMeta> get(UUID uuid)
	{
		return lp.primaryGroup(uuid).thenCombine(lp.prefix(uuid), (group, prefix) -> new Object[]{group, prefix})
				.thenCombine(lp.weightAsync(uuid), (arr, weight) -> {
					String group = (String) arr[0];
					String prefix = (String) arr[1];

					NamedTextColor color = mapColor(group);
					return new TagMeta(weight, prefix == null ? "" : prefix, color);
				});
	}

	private static NamedTextColor mapColor(String group)
	{
		if (group == null) return NamedTextColor.GRAY;

		return switch (group.toLowerCase())
		{
			case "admin" -> NamedTextColor.DARK_RED;
			case "srdev", "dev" -> NamedTextColor.AQUA;
			case "srmod", "mod", "srsup", "sup" -> NamedTextColor.RED;
			case "srcontent", "content" -> NamedTextColor.DARK_AQUA;
			case "srbuild", "build" -> NamedTextColor.DARK_GREEN;
			case "media" -> NamedTextColor.DARK_PURPLE;
			case "friend" -> NamedTextColor.GREEN;
			case "premium+" -> NamedTextColor.YELLOW;
			case "premium" -> NamedTextColor.GOLD;
			default -> NamedTextColor.GRAY;
		};
	}
}
