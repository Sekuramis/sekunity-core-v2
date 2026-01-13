package eu.sekunity.paper.infra.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * © Copyright 11.01.2026 - 17:15 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class CoreConfigLoader
{
	private CoreConfigLoader() {}

	public static CoreConfig load(JavaPlugin plugin)
	{
		plugin.saveDefaultConfig();
		FileConfiguration c = plugin.getConfig();

		boolean dbEnabled = c.getBoolean("database.enabled", false);
		String jdbcUrl = c.getString("database.jdbcUrl", "");
		String user = c.getString("database.username", "");
		String pass = c.getString("database.password", "");
		int poolSize = c.getInt("database.poolSize", 10);

		int threads = c.getInt("async.threads", 4);

		return new CoreConfig(
				new CoreConfig.DatabaseConfig(dbEnabled, jdbcUrl, user, pass, poolSize),
				new CoreConfig.AsyncConfig(threads)
		);
	}
}
