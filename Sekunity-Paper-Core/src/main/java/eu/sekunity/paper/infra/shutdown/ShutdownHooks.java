package eu.sekunity.paper.infra.shutdown;

import java.util.Optional;

import eu.sekunity.api.database.Database;
import eu.sekunity.api.service.nick.NickCache;
import eu.sekunity.paper.infra.async.AsyncExecutor;

/**
 * © Copyright 11.01.2026 - 17:15 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ShutdownHooks
{
	private final AsyncExecutor async;
	private final Optional<Database> db;
	private final NickCache nickCache;

	public ShutdownHooks(AsyncExecutor async, Optional<Database> db, NickCache nickCache)
	{
		this.async = async;
		this.db = db;
		this.nickCache = nickCache;
	}

	public void shutdown()
	{
		nickCache.clear();
		db.ifPresent(eu.sekunity.api.database.Database::close);
		async.shutdown();
	}
}
