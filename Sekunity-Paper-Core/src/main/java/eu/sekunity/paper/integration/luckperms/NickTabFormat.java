package eu.sekunity.paper.integration.luckperms;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * © Copyright 14.01.2026 - 21:41 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickTabFormat
{
	public static String prefix(Player p)
	{
		if (p.hasPermission("sekunity.nick.premium"))
			return "<gold>";
		return "<gray>";
	}

	public static NamedTextColor nameColor(Player p)
	{
		if (p.hasPermission("sekunity.nick.premium"))
			return NamedTextColor.GOLD;
		return NamedTextColor.GRAY;
	}
}
