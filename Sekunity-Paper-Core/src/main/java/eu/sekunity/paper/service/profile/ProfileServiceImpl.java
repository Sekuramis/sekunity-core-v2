package eu.sekunity.paper.service.profile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import eu.sekunity.api.service.ProfileService;

/**
 * © Copyright 11.01.2026 - 17:17 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class ProfileServiceImpl implements ProfileService
{
	private ProfileRepository profileRepo;

	public ProfileServiceImpl(ProfileRepository profileRepo)
	{
		this.profileRepo = profileRepo;
	}

	@Override
	public CompletableFuture<Void> touch(UUID playerId, String lastKnownName, long now)
	{
		return profileRepo.touch(playerId, lastKnownName, now);
	}

	@Override
	public CompletableFuture<Optional<String>> lastKnownName(UUID playerId)
	{
		return profileRepo.lastKnownName(playerId);
	}
}
