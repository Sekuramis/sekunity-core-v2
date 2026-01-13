package eu.sekunity.paper.infra.database;


import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import eu.sekunity.api.database.Database;

/**
 * © Copyright 11.01.2026 - 17:15 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class Migrations
{
	private static final int LATEST = 3;

	private Migrations() {}

	public static CompletableFuture<Void> run(Optional<Database> dbOpt)
	{
		if (dbOpt.isEmpty())
			return CompletableFuture.completedFuture(null);

		Database database = dbOpt.get();

		return ensureSchemaTable(database)
				.thenCompose(v -> getVersion(database))
				.thenCompose(ver -> migrate(database, ver))
				.thenApply(v -> null);
	}

	private static CompletableFuture<Void> ensureSchemaTable(Database database)
	{
		return database.update(
				"CREATE TABLE IF NOT EXISTS sekunity_schema (" +
						"  id INT NOT NULL PRIMARY KEY," +
						"  version INT NOT NULL" +
						")"
		).thenCompose(v -> database.update(
				"INSERT INTO sekunity_schema(id, version) VALUES (1, 0) " +
						"ON DUPLICATE KEY UPDATE version=version"
		)).thenApply(v -> null);
	}

	private static CompletableFuture<Integer> getVersion(Database database)
	{
		return database.queryOneOptional(
				"SELECT version FROM sekunity_schema WHERE id=1",
				rs -> rs.getInt("version")
		).thenApply(opt -> opt.orElse(0));
	}

	private static CompletableFuture<Void> setVersion(Database database, int version)
	{
		return database.update(
				"UPDATE sekunity_schema SET version=? WHERE id=1",
				version
		).thenApply(v -> null);
	}

	private static CompletableFuture<Void> migrate(Database database, int current)
	{
		if (current >= LATEST)
			return CompletableFuture.completedFuture(null);

		CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

		for (int v = current + 1; v <= LATEST; v++)
		{
			int target = v;
			chain = chain.thenCompose(x -> apply(database, target).thenCompose(y -> setVersion(database, target)));
		}

		return chain;
	}

	private static CompletableFuture<Void> apply(Database database, int version)
	{
		return switch (version)
		{
			case 1 -> v1(database);
			case 2 -> v2(database);
			case 3 -> v3(database);
			default -> CompletableFuture.failedFuture(new IllegalStateException("Unknown migration version: " + version));
		};
	}

	private static CompletableFuture<Void> v1(Database database)
	{
		return database.update(
				"CREATE TABLE IF NOT EXISTS nick_data (" +
						"  uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
						"  nick_name VARCHAR(32) NOT NULL" +
						") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
		).thenCompose(v -> database.update(
				"CREATE TABLE IF NOT EXISTS player_profile (" +
						"  uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
						"  last_name VARCHAR(16) NOT NULL," +
						"  first_join BIGINT NOT NULL," +
						"  last_seen BIGINT NOT NULL" +
						") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
		)).thenApply(v -> null);
	}

	private static CompletableFuture<Void> v2(Database database)
	{
		return database.update(
				"CREATE TABLE IF NOT EXISTS nick_identity (" +
						"  real_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
						"  enabled TINYINT(1) NOT NULL," +
						"  fake_uuid VARCHAR(36) NOT NULL," +
						"  fake_name VARCHAR(16) NOT NULL," +
						"  skin_value MEDIUMTEXT NOT NULL," +
						"  skin_signature MEDIUMTEXT NOT NULL," +
						"  updated_at BIGINT NOT NULL," +
						"  UNIQUE KEY uq_fake_uuid (fake_uuid)," +
						"  UNIQUE KEY uq_fake_name (fake_name)" +
						") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
		).thenApply(v -> null);
	}


	private static CompletableFuture<Void> v3(Database database)
	{
		return database.update(
				"CREATE TABLE IF NOT EXISTS nick_pool (" +
						"  id BIGINT NOT NULL AUTO_INCREMENT," +
						"  fake_uuid VARCHAR(36) NOT NULL," +
						"  fake_name VARCHAR(16) NOT NULL," +
						"  skin_value MEDIUMTEXT NOT NULL," +
						"  skin_signature MEDIUMTEXT NOT NULL," +
						"  taken_by VARCHAR(36) NULL," +
						"  taken_until BIGINT NULL," +
						"  PRIMARY KEY (id)," +
						"  UNIQUE KEY uq_pool_fake_uuid (fake_uuid)," +
						"  UNIQUE KEY uq_pool_fake_name (fake_name)," +
						"  UNIQUE KEY uq_pool_taken_by (taken_by)" +
						") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
		).thenCompose(v -> database.update(
				"ALTER TABLE nick_identity " +
						"ADD COLUMN pool_id BIGINT NULL"
		).exceptionally(ex -> 0)).thenCompose(v -> database.update(
				"ALTER TABLE nick_identity " +
						"ADD UNIQUE KEY uq_nick_identity_pool_id (pool_id)"
		).exceptionally(ex -> 0)).thenApply(v -> null);
	}

}
