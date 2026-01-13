package eu.sekunity.api.database;

/**
 * © Copyright 11.01.2026 - 17:43 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class Sql
{
	private Sql() {}

	public static String placeholders(int count)
	{
		if (count <= 0) throw new IllegalArgumentException("count");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++)
		{
			if (i > 0) sb.append(',');
			sb.append('?');
		}
		return sb.toString();
	}
}
