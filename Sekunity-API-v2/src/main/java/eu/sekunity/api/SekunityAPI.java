package eu.sekunity.api;

import eu.sekunity.api.platform.PaperAPI;
import eu.sekunity.api.platform.VelocityAPI;
import eu.sekunity.api.service.NickService;
import eu.sekunity.api.service.ProfileService;

/**
 * © Copyright 11.01.2026 - 16:31 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class SekunityAPI
{
	private static volatile SekunityProvider provider;

	private SekunityAPI()
	{
	}

	public static void install(SekunityProvider p)
	{
		if (p == null)
			throw new IllegalArgumentException("provider");

		if (provider != null)
			throw new IllegalStateException("SekunityAPI already installed");

		provider = p;
	}

	private static SekunityProvider provider()
	{
		SekunityProvider p = provider;
		if (p == null)
			throw new IllegalStateException("SekunityAPI not installed yet");

		return p;
	}

	/* ===== Common ===== */

	public static SekunityPlatform platform()
	{
		return provider().platform();
	}

	public static NickService nickService()
	{
		return provider().services().nickService();
	}

	public static ProfileService profileService()
	{
		return provider().services().profileService();
	}

	/* ===== Paper ===== */

	public static PaperAPI paper()
	{
		PaperAPI api = provider().paper();
		if (api == null)
			throw new IllegalStateException("Not running on Paper");

		return api;
	}

	/* ===== Velocity ===== */

	public static VelocityAPI proxy()
	{
		VelocityAPI api = provider().proxy();
		if (api == null)
			throw new IllegalStateException("Not running on Velocity");

		return api;
	}
}
