package eu.sekunity.paper;

import org.bukkit.plugin.java.JavaPlugin;

import eu.sekunity.api.SekunityAPI;
import eu.sekunity.paper.bootstrap.ServiceWiring;
import eu.sekunity.paper.infra.shutdown.ShutdownHooks;

/**
 * © Copyright 11.01.2026 - 17:12 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class SekunityPaperCore extends JavaPlugin
{
	private ShutdownHooks shutdown;

	@Override
	public void onEnable()
	{
		var wiring = ServiceWiring.create(this);

		SekunityAPI.install(wiring.provider());
		this.shutdown = wiring.shutdown();

		getLogger().info("SekunityAPI installed for Paper");
	}

	@Override
	public void onDisable()
	{
		if (shutdown != null)
			shutdown.shutdown();
	}

}
