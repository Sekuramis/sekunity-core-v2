package eu.sekunity.api.service.nick;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import eu.sekunity.api.database.Database;
import eu.sekunity.api.database.ResultSetMapper;

/**
 * © Copyright 14.01.2026 - 19:43 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public interface TransactionalDatabase extends Database
{
	<T> CompletableFuture<T> tx(Function<Database, CompletableFuture<T>> work);

	static Optional<TransactionalDatabase> adapt(Optional<Database> db)
	{
		if (db.isEmpty())
			return Optional.empty();

		if (db.get() instanceof TransactionalDatabase tdb)
			return Optional.of(tdb);

		return Optional.of(new TransactionalDatabase()
		{
			@Override
			public <T> CompletableFuture<T> tx(Function<Database, CompletableFuture<T>> work)
			{
				return work.apply(this);
			}

			@Override
			public CompletableFuture<Integer> update(String sql, Object... params)
			{
				return db.get().update(sql, params);
			}

			@Override
			public <T> CompletableFuture<T> queryOne(String sql, ResultSetMapper<T> mapper, Object... params)
			{
				return db.get().queryOne(sql, mapper, params);
			}

			@Override
			public <T> CompletableFuture<List<T>> query(String sql, ResultSetMapper<T> mapper, Object... params)
			{
				return db.get().query(sql, mapper, params);
			}

			@Override
			public void close()
			{
				db.get().close();
			}

			@Override
			public <T> CompletableFuture<Optional<T>> queryOneOptional(String sql, ResultSetMapper<T> mapper, Object... params)
			{
				return db.get().queryOneOptional(sql, mapper, params);
			}
		});
	}
}
