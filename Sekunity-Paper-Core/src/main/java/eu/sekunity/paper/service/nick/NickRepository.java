package eu.sekunity.paper.service.nick;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import eu.sekunity.api.database.Database;
import eu.sekunity.api.service.NickIdentity;

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
	private final Optional<Database> db;

	public NickRepository(Optional<Database> db)
	{
		this.db = db;
	}

	public CompletableFuture<Optional<NickIdentity>> findByRealUuid(UUID realUuid)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		return db.get().queryOneOptional("SELECT i.real_uuid, i.enabled, i.pool_id, i.updated_at AS i_updated_at, "
						+ "p.fake_uuid, p.fake_name, p.skin_value, p.skin_signature " + "FROM nick_identity i "
						+ "JOIN nick_pool p ON p.id = i.pool_id " + "WHERE i.real_uuid=?",
				rs -> new NickIdentity(UUID.fromString(rs.getString("real_uuid")), rs.getInt("enabled") == 1,
						UUID.fromString(rs.getString("fake_uuid")), rs.getString("fake_name"),
						rs.getString("skin_value"), rs.getString("skin_signature"), rs.getLong("i_updated_at"),
						rs.getLong("pool_id")), realUuid.toString());
	}

	public CompletableFuture<Optional<NickPoolEntry>> claimFreePoolEntry(UUID realUuid)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		long now = System.currentTimeMillis();

		return db.get().queryOneOptional("SELECT id, fake_uuid, fake_name, skin_value, skin_signature "
								+ "FROM nick_pool WHERE taken=0 ORDER BY id ASC LIMIT 1",
						rs -> new NickPoolEntry(rs.getLong("id"), UUID.fromString(rs.getString("fake_uuid")),
								rs.getString("fake_name"), rs.getString("skin_value"), rs.getString("skin_signature")))
				.thenCompose(opt ->
				{
					if (opt.isEmpty())
						return CompletableFuture.completedFuture(Optional.empty());

					NickPoolEntry e = opt.get();

					return db.get()
							.update("UPDATE nick_pool SET taken=1, updated_at=? WHERE id=? AND taken=0", now, e.id())
							.thenCompose(rows ->
							{
								if (rows == 0)
									return CompletableFuture.completedFuture(Optional.empty());

								return db.get()
										.update("INSERT INTO nick_identity(real_uuid, enabled, pool_id, updated_at) "
														+ "VALUES(?,0,?,?) "
														+ "ON DUPLICATE KEY UPDATE pool_id=VALUES(pool_id), updated_at=VALUES(updated_at)",
												realUuid.toString(), e.id(), now).thenApply(v -> Optional.of(e));
							});
				});
	}

	public CompletableFuture<Void> setEnabled(UUID realUuid, boolean enabled, long time)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		return db.get()
				.update("UPDATE nick_identity SET enabled=?, updated_at=? WHERE real_uuid=?", enabled ? 1 : 0, time,
						realUuid.toString()).thenApply(v -> null);
	}

	public CompletableFuture<Void> release(UUID realUuid)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		return findByRealUuid(realUuid).thenCompose(opt ->
		{
			if (opt.isEmpty())
				return CompletableFuture.completedFuture(null);

			long poolId = opt.get().poolId();
			long now = System.currentTimeMillis();

			return db.get().update("DELETE FROM nick_identity WHERE real_uuid=?", realUuid.toString()).thenCompose(
							v -> db.get().update("UPDATE nick_pool SET taken=0, updated_at=? WHERE id=?", now, poolId))
					.thenApply(v -> null);
		});
	}

	public CompletableFuture<Void> upsertIdentity(UUID realUuid, boolean enabled, NickPoolEntry entry, long now)
	{
		if (db.isEmpty())
			return CompletableFuture.completedFuture(null);

		return db.get().update("INSERT INTO nick_identity "
								+ "(real_uuid, enabled, pool_id, fake_uuid, fake_name, skin_value, skin_signature, updated_at) "
								+ "VALUES (?,?,?,?,?,?,?,?) " + "ON DUPLICATE KEY UPDATE " + "enabled=VALUES(enabled), "
								+ "pool_id=VALUES(pool_id), " + "fake_uuid=VALUES(fake_uuid), " + "fake_name=VALUES(fake_name), "
								+ "skin_value=VALUES(skin_value), " + "skin_signature=VALUES(skin_signature), "
								+ "updated_at=VALUES(updated_at)", realUuid.toString(), enabled ? 1 : 0, entry.id(),
						entry.fakeUuid().toString(), entry.fakeName(), entry.skinValue(), entry.skinSignature(), now)
				.thenApply(x -> null);
	}
}
