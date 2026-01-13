package eu.sekunity.paper.service.nick;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import eu.sekunity.api.service.NickIdentity;

/**
 * © Copyright 11.01.2026 - 19:58 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickCache
{
	private final ConcurrentHashMap<UUID, NickIdentity> byReal = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, UUID> realByFake = new ConcurrentHashMap<>();

	public Optional<NickIdentity> getIfEnabled(UUID realUuid)
	{
		return Optional.ofNullable(byReal.get(realUuid));
	}

	public Optional<NickIdentity> getIfEnabledByFake(UUID fakeUuid)
	{
		UUID real = realByFake.get(fakeUuid);
		if (real == null)
			return Optional.empty();

		return Optional.ofNullable(byReal.get(real));
	}

	public void setEnabled(NickIdentity identity)
	{
		if (!identity.enabled())
			return;

		byReal.put(identity.realUuid(), identity);
		realByFake.put(identity.fakeUuid(), identity.realUuid());
	}

	public void remove(UUID realUuid)
	{
		NickIdentity removed = byReal.remove(realUuid);
		if (removed != null)
			realByFake.remove(removed.fakeUuid());
	}

	public void clear()
	{
		byReal.clear();
		realByFake.clear();
	}
}
