package eu.sekunity.api.service.nick;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import eu.sekunity.api.database.Database;

/**
 * © Copyright 11.01.2026 - 17:17 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickRepository
{
	private final Optional<TransactionalDatabase> db;

	public NickRepository(Optional<Database> db)
	{
		this.db = TransactionalDatabase.adapt(db);
	}

	public CompletableFuture<Void> registerIfAbsent(UUID realUuid)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		return db.get().update("INSERT IGNORE INTO nick_identity (real_uuid) VALUES (?)", realUuid.toString())
				.thenApply(x -> null);
	}

	public CompletableFuture<Optional<NickIdentity>> findByRealUuid(UUID realUuid)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		String sql = ""
				+ "SELECT i.real_uuid, i.enabled, i.keep_nick, COALESCE(i.pool_id, 0) AS pool_id, "
				+ "p.fake_uuid, p.fake_name, p.skin_value, p.skin_signature "
				+ "FROM nick_identity i "
				+ "LEFT JOIN nick_pool p ON p.id = i.pool_id "
				+ "WHERE i.real_uuid=?";

		return db.get().queryOneOptional(sql, rs -> mapIdentity(rs), realUuid.toString());
	}

	public CompletableFuture<Void> setKeepNick(UUID realUuid, boolean keep)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		return db.get().update("UPDATE nick_identity SET keep_nick=? WHERE real_uuid=?", keep ? 1 : 0, realUuid.toString())
				.thenApply(x -> null);
	}

	public CompletableFuture<Void> setEnabled(UUID realUuid, boolean enabled)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		return db.get().update("UPDATE nick_identity SET enabled=? WHERE real_uuid=?", enabled ? 1 : 0, realUuid.toString())
				.thenApply(x -> null);
	}

	public CompletableFuture<Optional<NickPoolEntry>> getPoolById(long poolId)
	{
		if (db.isEmpty() || poolId <= 0)
			return CompletableFuture.completedFuture(Optional.empty());

		String sql = "SELECT id, fake_uuid, fake_name, skin_value, skin_signature FROM nick_pool WHERE id=?";
		return db.get().queryOneOptional(sql, rs -> mapPool(rs), poolId);
	}

	public CompletableFuture<Optional<NickPoolEntry>> claimRandom(UUID realUuid)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		return db.get().update(
				"UPDATE nick_pool SET taken_by=?, taken_at=NOW() WHERE taken_by IS NULL ORDER BY RAND() LIMIT 1",
				realUuid.toString()
		).thenCompose(affected -> {
			if (affected == null || affected <= 0)
				return CompletableFuture.completedFuture(Optional.empty());

			return db.get().queryOneOptional(
					"SELECT id, fake_uuid, fake_name, skin_value, skin_signature FROM nick_pool WHERE taken_by=? LIMIT 1",
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

	public CompletableFuture<Void> releasePoolId(UUID realUuid, long poolId)
	{
		if (db.isEmpty() || poolId <= 0)
			return CompletableFuture.completedFuture(null);

		return db.get().update("UPDATE nick_pool SET taken_by=NULL, taken_at=NULL WHERE id=? AND taken_by=?", poolId, realUuid.toString())
				.thenCompose(x -> db.get().update("UPDATE nick_identity SET pool_id=NULL WHERE real_uuid=?", realUuid.toString()))
				.thenApply(x -> null);
	}


	public CompletableFuture<Void> setPoolId(UUID realUuid, long poolId)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		return db.get().update("UPDATE nick_identity SET pool_id=? WHERE real_uuid=?", poolId <= 0 ? null : poolId, realUuid.toString())
				.thenApply(x -> null);
	}

	private static NickIdentity mapIdentity(ResultSet rs) throws SQLException
	{
		String real = rs.getString("real_uuid");
		int enabled = rs.getInt("enabled");
		int keep = rs.getInt("keep_nick");
		long poolId = rs.getLong("pool_id");

		String fakeUuidStr = rs.getString("fake_uuid");
		String fakeName = rs.getString("fake_name");
		String skinValue = rs.getString("skin_value");
		String skinSignature = rs.getString("skin_signature");

		UUID fakeUuid = fakeUuidStr == null ? null : UUID.fromString(fakeUuidStr);

		return new NickIdentity(
				UUID.fromString(real),
				enabled == 1,
				keep == 1,
				poolId,
				fakeUuid,
				fakeName,
				skinValue,
				skinSignature
		);
	}

	private static NickPoolEntry mapPool(ResultSet rs) throws SQLException
	{
		return new NickPoolEntry(
				rs.getLong("id"),
				UUID.fromString(rs.getString("fake_uuid")),
				rs.getString("fake_name"),
				rs.getString("skin_value"),
				rs.getString("skin_signature")
		);
	}
}
