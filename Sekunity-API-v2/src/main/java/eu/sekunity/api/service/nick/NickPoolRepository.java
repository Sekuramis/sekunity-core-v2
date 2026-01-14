package eu.sekunity.api.service.nick;

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

	public CompletableFuture<Optional<NickPoolEntry>> claimRandom(UUID realUuid)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		return db.get().update(
				"UPDATE nick_pool SET taken_by=?, taken_at=NOW() " +
						"WHERE taken_by IS NULL " +
						"ORDER BY RAND() " +
						"LIMIT 1",
				realUuid.toString()
		).thenCompose(affected -> {
			if (affected == null || affected <= 0)
				return CompletableFuture.completedFuture(Optional.empty());

			return db.get().queryOneOptional(
					"SELECT id, fake_uuid, fake_name, skin_value, skin_signature " +
							"FROM nick_pool WHERE taken_by=? LIMIT 1",
					rs -> new NickPoolEntry(
							rs.getLong("id"),
							UUID.fromString(rs.getString("fake_uuid")),
							rs.getString("fake_name"),
							rs.getString("skin_value"),
							rs.getString("skin_signature")
					),
					realUuid.toString()
			);
		});
	}

	public CompletableFuture<Long> insertPoolEntry(UUID fakeUuid, String fakeName, String skinValue, String skinSignature, long now)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(-1L);

		return db.get().update(
				"INSERT IGNORE INTO nick_pool (fake_uuid, fake_name, skin_value, skin_signature, taken_by, taken_at) VALUES (?, ?, ?, ?, NULL, NULL)",
				fakeUuid.toString(), fakeName, skinValue, skinSignature
		).thenCompose(affected -> {
			if (affected != null && affected > 0)
			{
				return db.get().queryOne(
						"SELECT id FROM nick_pool WHERE fake_uuid=? LIMIT 1",
						rs -> rs.getLong("id"),
						fakeUuid.toString()
				);
			}

			return db.get().queryOne(
					"SELECT id FROM nick_pool WHERE fake_name=? LIMIT 1",
					rs -> rs.getLong("id"),
					fakeName
			);
		});
	}

	public CompletableFuture<Optional<Long>> findIdByFakeName(String fakeName)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		return db.get().queryOneOptional(
				"SELECT id FROM nick_pool WHERE fake_name=? LIMIT 1",
				rs -> rs.getLong("id"),
				fakeName
		);
	}
}