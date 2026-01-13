package eu.sekunity.paper.service.profile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import eu.sekunity.api.database.Database;

/**
 * © Copyright 11.01.2026 - 17:17 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ProfileRepository
{
	private final Optional<Database> database;
	private final Executor executor;

	public ProfileRepository(Optional<Database> database, Executor executor)
	{
		this.database = database;
		this.executor = executor;
	}

	public CompletableFuture<Void> touch(UUID playerId, String lastKnownName, long now)
	{
		if (database.isEmpty())
			return CompletableFuture.completedFuture(null);

		String uuid = playerId.toString();

		return database.get().update(
				"INSERT INTO player_profile(uuid, last_name, first_join, last_seen) " +
						"VALUES (?,?,?,?) " +
						"ON DUPLICATE KEY UPDATE last_name=VALUES(last_name), last_seen=VALUES(last_seen)",
				uuid, lastKnownName, now, now
		).thenApply(rows -> null);
	}

	public CompletableFuture<Optional<String>> lastKnownName(UUID playerId)
	{
		if (database.isEmpty())
			return CompletableFuture.completedFuture(Optional.empty());

		return database.get().queryOneOptional(
				"SELECT last_name FROM player_profile WHERE uuid=?",
				rs -> rs.getString("last_name"),
				playerId.toString()
		);
	}
}
