package eu.sekunity.api.database;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * © Copyright 11.01.2026 - 17:39 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */

/**
 * Asynchronous database abstraction used by Sekunity.
 * <p>
 * All operations are executed asynchronously and must never block the caller thread.
 */
public interface Database
{
	/**
	 * Executes an update/insert/delete SQL statement.
	 *
	 * @param sql    the SQL statement
	 * @param params parameters bound to the prepared statement
	 * @return future containing the number of affected rows
	 */
	CompletableFuture<Integer> update(String sql, Object... params);

	/**
	 * Executes a query expected to return exactly one row.
	 *
	 * @param sql    the SQL statement
	 * @param mapper mapper converting the {@link java.sql.ResultSet} to the target type
	 * @param params parameters bound to the prepared statement
	 * @param <T>    mapped result type
	 * @return future containing the mapped result
	 * @throws IllegalStateException if no row is returned
	 */
	<T> CompletableFuture<T> queryOne(String sql, ResultSetMapper<T> mapper, Object... params);

	/**
	 * Executes a query returning zero or one row.
	 *
	 * @param sql    the SQL statement
	 * @param mapper mapper converting the {@link java.sql.ResultSet} to the target type
	 * @param params parameters bound to the prepared statement
	 * @param <T>    mapped result type
	 * @return future containing an optional result
	 */
	<T> CompletableFuture<Optional<T>> queryOneOptional(String sql, ResultSetMapper<T> mapper, Object... params);

	/**
	 * Executes a query returning multiple rows.
	 *
	 * @param sql    the SQL statement
	 * @param mapper mapper converting each {@link java.sql.ResultSet} row
	 * @param params parameters bound to the prepared statement
	 * @param <T>    mapped result type
	 * @return future containing the list of mapped results
	 */
	<T> CompletableFuture<java.util.List<T>> query(String sql, ResultSetMapper<T> mapper, Object... params);

	/**
	 * Closes the database and releases all underlying resources.
	 * <p>
	 * Must be called during shutdown.
	 */
	void close();
}
