package eu.sekunity.api.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * © Copyright 11.01.2026 - 17:40 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */

/**
 * Maps a {@link ResultSet} row to a Java object.
 *
 * @param <T> target type of the mapped result
 */
@FunctionalInterface
public interface ResultSetMapper<T>
{
	T map(ResultSet rs) throws SQLException;
}
