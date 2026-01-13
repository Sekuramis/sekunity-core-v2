package eu.sekunity.paper.bootstrap;

import eu.sekunity.api.SekunityPlatform;
import eu.sekunity.api.SekunityProvider;
import eu.sekunity.api.SekunityServices;
import eu.sekunity.api.platform.PaperAPI;
import eu.sekunity.api.platform.VelocityAPI;

/**
 * © Copyright 11.01.2026 - 17:13 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class SekunityPaperProvider implements SekunityProvider
{
	private final SekunityServices services;
	private final PaperAPI paper;

	public SekunityPaperProvider(SekunityServices services, PaperAPI paper)
	{
		this.services = services;
		this.paper = paper;
	}

	@Override public SekunityPlatform platform() { return SekunityPlatform.PAPER; }
	@Override public SekunityServices services() { return services; }
	@Override public PaperAPI paper() { return paper; }
	@Override public VelocityAPI proxy() { return null; }
}
