package eu.sekunity.paper.infra.database;

import java.util.Optional;
import java.util.concurrent.Executor;

import eu.sekunity.api.database.Database;
import eu.sekunity.paper.infra.config.CoreConfig;

/**
 * © Copyright 11.01.2026 - 17:15 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class DatabaseFactory
{
	private DatabaseFactory() {}

	public static Optional<Database> create(CoreConfig config, Executor async)
	{
		if (!config.database().enabled())
			return Optional.empty();

		return Optional.of(HikariDatabase.create(
				config.database().jdbcUrl(),
				config.database().username(),
				config.database().password(),
				config.database().poolSize(),
				async
		));
	}
}
