package eu.sekunity.paper.infra.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * © Copyright 11.01.2026 - 17:22 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class AsyncExecutorImpl implements AsyncExecutor
{
	private final ExecutorService pool;

	public AsyncExecutorImpl(String namePrefix, int threads)
	{
		if (threads <= 0)
			throw new IllegalArgumentException("threads");

		var tf = Thread.ofPlatform().name(namePrefix + "-", 0).factory();
		this.pool = Executors.newFixedThreadPool(threads, tf);
	}

	@Override
	public Executor executor()
	{
		return pool;
	}

	@Override
	public void shutdown()
	{
		pool.shutdown();
		try
		{
			if (!pool.awaitTermination(5, TimeUnit.SECONDS))
				pool.shutdownNow();
		}
		catch (InterruptedException e)
		{
			pool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
