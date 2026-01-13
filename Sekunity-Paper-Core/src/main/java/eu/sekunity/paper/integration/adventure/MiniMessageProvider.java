package eu.sekunity.paper.integration.adventure;

import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * © Copyright 11.01.2026 - 17:16 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class MiniMessageProvider
{
	private final MiniMessage miniMessage;

	private MiniMessageProvider(MiniMessage miniMessage)
	{
		this.miniMessage = miniMessage;
	}

	public static MiniMessageProvider create()
	{
		return new MiniMessageProvider(MiniMessage.miniMessage());
	}

	public MiniMessage miniMessage()
	{
		return miniMessage;
	}
}
