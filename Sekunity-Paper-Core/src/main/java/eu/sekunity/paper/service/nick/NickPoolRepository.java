package eu.sekunity.paper.service.nick;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import eu.sekunity.api.database.Database;

/**
 * © Copyright 11.01.2026 - 19:18 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickPoolRepository
{
	private final Optional<Database> db;

	public NickPoolRepository(Optional<Database> db)
	{
		this.db = db;
	}

	public CompletableFuture<Optional<NickPoolEntry>> findById(long id)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		return db.get().queryOneOptional(
				"SELECT id, fake_uuid, fake_name, skin_value, skin_signature FROM nick_pool WHERE id=?",
				rs -> new NickPoolEntry(
						rs.getLong("id"),
						UUID.fromString(rs.getString("fake_uuid")),
						rs.getString("fake_name"),
						rs.getString("skin_value"),
						rs.getString("skin_signature")
				),
				id
		);
	}

	public CompletableFuture<Optional<NickPoolEntry>> claim(UUID realUuid, long now, long leaseMillis)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		long until = now + leaseMillis
				;

		return tryClaim(realUuid, now, until, 5);
	}

	private CompletableFuture<Optional<NickPoolEntry>> tryClaim(UUID realUuid, long now, long until, int attemptsLeft)
	{
		if (attemptsLeft <= 0)
			return CompletableFuture.completedFuture(Optional.empty());

		return db.get().queryOneOptional(
				"SELECT id FROM nick_pool " +
						"WHERE taken_by IS NULL OR taken_until < ? " +
						"ORDER BY id ASC LIMIT 1",
				rs -> rs.getLong("id"),
				now
		).thenCompose(idOpt -> {
			if (idOpt.isEmpty())
				return CompletableFuture.completedFuture(Optional.empty());

			long poolId = idOpt.get();

			return db.get().update(
					"UPDATE nick_pool SET taken_by=?, taken_until=? " +
							"WHERE id=? AND (taken_by IS NULL OR taken_until < ?)",
					realUuid.toString(),
					until,
					poolId,
					now
			).thenCompose(updated -> {
				if (updated != 1)
					return tryClaim(realUuid, now, until, attemptsLeft - 1);

				return findById(poolId);
			});
		});
	}

	public CompletableFuture<Void> refreshLease(UUID realUuid, long now, long leaseMillis)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		long until = now + leaseMillis;

		return db.get().update(
				"UPDATE nick_pool SET taken_until=? WHERE taken_by=?",
				until,
				realUuid.toString()
		).thenApply(x -> null);
	}

	public CompletableFuture<Void> release(UUID realUuid)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		return db.get().update(
				"UPDATE nick_pool SET taken_by=NULL, taken_until=NULL WHERE taken_by=?",
				realUuid.toString()
		).thenApply(x -> null);
	}

	public CompletableFuture<Long> insertPoolEntry(UUID fakeUuid, String fakeName, String skinValue, String skinSignature, long now)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(-1L);

		return db.get().update(
				"INSERT INTO nick_pool " +
						"(fake_uuid, fake_name, skin_value, skin_signature, taken_by, taken_until, created_at, updated_at) " +
						"VALUES (?,?,?,?,NULL,NULL,?,?)",
				fakeUuid.toString(),
				fakeName,
				skinValue,
				skinSignature,
				now,
				now
		).thenCompose(v ->
				db.get().queryOne(
						"SELECT id FROM nick_pool WHERE fake_uuid=?",
						rs -> rs.getLong(1),
						fakeUuid.toString()
				)
		);
	}
}