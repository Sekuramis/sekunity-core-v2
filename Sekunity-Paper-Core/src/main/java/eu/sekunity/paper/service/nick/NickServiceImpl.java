package eu.sekunity.paper.service.nick;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import eu.sekunity.api.SekunityAPI;
import eu.sekunity.api.service.NickIdentity;
import eu.sekunity.api.service.NickService;
import eu.sekunity.paper.integration.nick.NickRefresher;

/**
 * © Copyright 11.01.2026 - 17:17 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickServiceImpl implements NickService
{
	private final NickRepository repo;
	private final NickPoolRepository pool;
	private final NickCache cache;
	private final long leaseMillis;
	private final NickRefresher refresher;

	public NickServiceImpl(
			NickRepository repo,
			NickPoolRepository pool,
			NickCache cache,
			long leaseMillis,
			NickRefresher refresher
	)
	{
		this.repo = repo;
		this.pool = pool;
		this.cache = cache;
		this.leaseMillis = leaseMillis;
		this.refresher = refresher;
	}

	@Override
	public CompletableFuture<Optional<NickIdentity>> identity(UUID realUuid)
	{
		return repo.findByRealUuid(realUuid);
	}

	@Override
	public CompletableFuture<Optional<NickIdentity>> nickPlayer(UUID realUuid)
	{
		long now = System.currentTimeMillis();

		return repo.findByRealUuid(realUuid).thenCompose(existing -> {
			if (existing.isPresent())
			{
				NickIdentity id = existing.get();
				if (id.enabled())
					return CompletableFuture.completedFuture(existing);

				return repo.setEnabled(realUuid, true, now)
						.thenCompose(v -> pool.refreshLease(realUuid, now, leaseMillis))
						.thenCompose(v -> repo.findByRealUuid(realUuid));
			}

			return pool.claim(realUuid, now, leaseMillis).thenCompose(entryOpt -> {
				if (entryOpt.isEmpty())
					return CompletableFuture.completedFuture(Optional.empty());

				NickPoolEntry entry = entryOpt.get();

				return repo.upsertIdentity(realUuid, true, entry, now)
						.thenCompose(v -> repo.findByRealUuid(realUuid));
			});

		}).thenApply(opt -> {
			if (opt.isEmpty())
				return Optional.empty();

			NickIdentity id = opt.get();
			if (!id.enabled())
				return opt;

			SekunityAPI.paper().syncExecutor().execute(() -> {
				cache.setEnabled(id);

				Player player = Bukkit.getPlayer(realUuid);
				if (player != null)
					refresher.applyNick(player, id);
			});

			return opt;
		});
	}

	@Override
	public CompletableFuture<Void> unnickPlayer(UUID realUuid)
	{
		long now = System.currentTimeMillis();

		return repo.findByRealUuid(realUuid).thenCompose(existing -> {
			if (existing.isEmpty())
			{
				cache.remove(realUuid);
				return CompletableFuture.completedFuture(null);
			}

			NickIdentity current = existing.get();

			return repo.setEnabled(realUuid, false, now)
					.thenCompose(v -> pool.release(realUuid))
					.thenRun(() -> {
						cache.remove(realUuid);

						Player player = Bukkit.getPlayer(realUuid);
						if (player != null)
							refresher.removeNick(player, current);
					});
		});
	}

}