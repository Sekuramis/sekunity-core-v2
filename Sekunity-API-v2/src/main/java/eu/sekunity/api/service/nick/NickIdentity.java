package eu.sekunity.api.service.nick;

import java.util.UUID;

/**
 * © Copyright 11.01.2026 - 19:06 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public record NickIdentity(
		UUID realUuid,
		boolean enabled,
		boolean keepNick,
		long poolId,
		UUID fakeUuid,
		String fakeName,
		String skinValue,
		String skinSignature
) {}
