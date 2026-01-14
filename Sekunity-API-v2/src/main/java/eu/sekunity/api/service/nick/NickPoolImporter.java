package eu.sekunity.api.service.nick;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * © Copyright 12.01.2026 - 19:15 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickPoolImporter
{
	private final MojangSkinImporter mojang;
	private final NickPoolRepository pool;
	private final Executor async;

	public NickPoolImporter(MojangSkinImporter mojang, NickPoolRepository pool, Executor async)
	{
		this.mojang = mojang;
		this.pool = pool;
		this.async = async;
	}

	public CompletableFuture<Long> importFromUuid(UUID uuid, String overrideFakeName)
	{
		long now = System.currentTimeMillis();

		return mojang.fetch(uuid).thenCompose(profile -> {
			UUID fakeUuid = UUID.randomUUID();
			String fakeName = normalizeName(overrideFakeName != null && !overrideFakeName.isBlank() ? overrideFakeName : profile.username());
			return pool.insertPoolEntry(fakeUuid, fakeName, profile.skinValue(), profile.skinSignature(), now);
		});
	}

	private static String normalizeName(String in)
	{
		String s = in.trim();
		if (s.length() > 16)
			s = s.substring(0, 16);
		return s;
	}
}
