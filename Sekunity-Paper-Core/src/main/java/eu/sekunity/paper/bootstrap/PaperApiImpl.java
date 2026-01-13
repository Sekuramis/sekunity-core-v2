package eu.sekunity.paper.bootstrap;

import java.util.concurrent.Executor;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import eu.sekunity.api.platform.PaperAPI;

/**
 * © Copyright 11.01.2026 - 17:13 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class PaperApiImpl implements PaperAPI
{
	private final Plugin plugin;

	public PaperApiImpl(Plugin plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public Plugin plugin()
	{
		return plugin;
	}

	@Override
	public Executor syncExecutor()
	{
		return r -> Bukkit.getScheduler().runTask(plugin, r);
	}
}
