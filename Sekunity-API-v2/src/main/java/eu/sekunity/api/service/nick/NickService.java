package eu.sekunity.api.service.nick;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * © Copyright 11.01.2026 - 16:32 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickService
{
	private final NickRepository repo;
	private final NickCache cache;

	public NickService(NickRepository repo, NickCache cache)
	{
		this.repo = repo;
		this.cache = cache;
	}

	public CompletableFuture<Void> registerIfAbsent(UUID realUuid)
	{
		return repo.registerIfAbsent(realUuid);
	}

	public CompletableFuture<Optional<NickIdentity>> get(UUID realUuid)
	{
		return repo.findByRealUuid(realUuid);
	}

	public CompletableFuture<Optional<NickIdentity>> enableRandomOrKeep(UUID realUuid)
	{
		return repo.registerIfAbsent(realUuid)
				.thenCompose(v -> repo.findByRealUuid(realUuid))
				.thenCompose(opt -> {
					if (opt.isEmpty())
						return CompletableFuture.completedFuture(Optional.empty());

					NickIdentity cur = opt.get();

					if (cur.keepNick() && cur.poolId() > 0)
					{
						return repo.setEnabled(realUuid, true)
								.thenCompose(x -> repo.findByRealUuid(realUuid));
					}

					return repo.claimRandom(realUuid)
							.thenCompose(claimed -> {
								if (claimed.isEmpty())
									return CompletableFuture.completedFuture(Optional.empty());

								NickPoolEntry p = claimed.get();

								return repo.setPoolId(realUuid, p.id())
										.thenCompose(x -> repo.setEnabled(realUuid, true))
										.thenCompose(x -> repo.findByRealUuid(realUuid));
							});
				})
				.thenApply(opt -> {
					opt.ifPresent(cache::setEnabled);
					return opt;
				});
	}

	public CompletableFuture<Void> disable(UUID realUuid)
	{
		return repo.registerIfAbsent(realUuid)
				.thenCompose(v -> repo.findByRealUuid(realUuid))
				.thenCompose(opt -> {
					if (opt.isEmpty())
						return CompletableFuture.completedFuture(null);

					NickIdentity cur = opt.get();

					return repo.setEnabled(realUuid, false)
							.thenCompose(x -> {
								cache.remove(realUuid);

								if (cur.keepNick())
									return CompletableFuture.completedFuture(null);

								if (cur.poolId() <= 0)
									return CompletableFuture.completedFuture(null);

								return repo.releasePoolId(realUuid, cur.poolId());
							});
				});
	}

	public CompletableFuture<Optional<NickIdentity>> keepNickOn(UUID realUuid)
	{
		return repo.registerIfAbsent(realUuid)
				.thenCompose(v -> repo.findByRealUuid(realUuid))
				.thenCompose(opt -> {
					if (opt.isEmpty())
						return CompletableFuture.completedFuture(Optional.empty());

					NickIdentity id = opt.get();

					CompletableFuture<Optional<NickIdentity>> ensureNick;
					if (id.poolId() > 0)
						ensureNick = CompletableFuture.completedFuture(opt);
					else
						ensureNick = enableRandomOrKeep(realUuid);

					return ensureNick.thenCompose(after -> {
						if (after.isEmpty())
							return CompletableFuture.completedFuture(Optional.empty());

						return repo.setKeepNick(realUuid, true)
								.thenCompose(x -> repo.findByRealUuid(realUuid));
					});
				});
	}

	public CompletableFuture<Void> keepNickOff(UUID realUuid)
	{
		return repo.registerIfAbsent(realUuid)
				.thenCompose(v -> repo.setKeepNick(realUuid, false));
	}
}