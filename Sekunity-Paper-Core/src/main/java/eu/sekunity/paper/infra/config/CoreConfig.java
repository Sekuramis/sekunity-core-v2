package eu.sekunity.paper.infra.config;

/**
 * © Copyright 11.01.2026 - 17:14 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public record CoreConfig(
		DatabaseConfig database,
		AsyncConfig async
)
{
	public record DatabaseConfig(
			boolean enabled,
			String jdbcUrl,
			String username,
			String password,
			int poolSize
	) {}

	public record AsyncConfig(
			int threads
	) {}
}
