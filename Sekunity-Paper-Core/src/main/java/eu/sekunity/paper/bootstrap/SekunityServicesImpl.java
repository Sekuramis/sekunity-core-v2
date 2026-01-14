package eu.sekunity.paper.bootstrap;

import eu.sekunity.api.SekunityServices;
import eu.sekunity.api.service.nick.NickService;
import eu.sekunity.api.service.ProfileService;

/**
 * © Copyright 11.01.2026 - 17:14 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class SekunityServicesImpl implements SekunityServices
{
	private final NickService nickService;
	private final ProfileService profileService;

	public SekunityServicesImpl(NickService nickService, ProfileService profileService)
	{
		this.nickService = nickService;
		this.profileService = profileService;
	}

	@Override
	public NickService nickService()
	{
		return nickService;
	}

	@Override
	public ProfileService profileService()
	{
		return profileService;
	}
}
