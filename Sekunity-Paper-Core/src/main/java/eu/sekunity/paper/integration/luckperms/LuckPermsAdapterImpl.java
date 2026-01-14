package eu.sekunity.paper.integration.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

/**
 * © Copyright 11.01.2026 - 17:16 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class LuckPermsAdapterImpl implements LuckPermsAdapter
{
	private final Executor executor;
	private final ConcurrentHashMap<UUID, Integer> weightCache = new ConcurrentHashMap<>();

	public LuckPermsAdapterImpl(Executor executor)
	{
		this.executor = executor;
	}

	@Override
	public CompletableFuture<String> prefix(UUID uuid)
	{
		return CompletableFuture.supplyAsync(() -> {
			LuckPerms lp = LuckPermsProvider.get();
			var user = lp.getUserManager().getUser(uuid);
			if (user == null) return "";
			var meta = user.getCachedData().getMetaData();
			String p = meta.getPrefix();
			return p == null ? "" : p;
		}, executor);
	}

	@Override
	public CompletableFuture<String> suffix(UUID uuid)
	{
		return CompletableFuture.supplyAsync(() -> {
			LuckPerms lp = LuckPermsProvider.get();
			var user = lp.getUserManager().getUser(uuid);
			if (user == null) return "";
			var meta = user.getCachedData().getMetaData();
			String s = meta.getSuffix();
			return s == null ? "" : s;
		}, executor);
	}

	@Override
	public CompletableFuture<String> primaryGroup(UUID uuid)
	{
		return CompletableFuture.supplyAsync(() -> {
			LuckPerms lp = LuckPermsProvider.get();
			var user = lp.getUserManager().getUser(uuid);
			if (user == null) return "";
			return user.getPrimaryGroup();
		}, executor);
	}

	@Override
	public int weight(UUID playerUuid)
	{
		Integer cached = weightCache.get(playerUuid);
		if (cached != null)
			return cached;

		weightCache.put(playerUuid, 0);

		java.util.concurrent.CompletableFuture.runAsync(() -> {
			try
			{
				LuckPerms lp = LuckPermsProvider.get();
				User u = lp.getUserManager().getUser(playerUuid);
				if (u == null)
					return;

				String primary = u.getPrimaryGroup();
				Group g = lp.getGroupManager().getGroup(primary);

				int w = 0;
				if (g != null && g.getWeight().isPresent())
					w = g.getWeight().getAsInt();

				weightCache.put(playerUuid, w);
			}
			catch (Throwable ignored) {}
		}, executor);

		return 0;
	}

	@Override
	public CompletableFuture<Integer> weightAsync(UUID uuid)
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				LuckPerms lp = LuckPermsProvider.get();
				var user = lp.getUserManager().getUser(uuid);
				if (user == null)
					return 0;

				String primary = user.getPrimaryGroup();
				Group g = lp.getGroupManager().getGroup(primary);

				int w = 0;
				if (g != null && g.getWeight().isPresent())
					w = g.getWeight().getAsInt();

				weightCache.put(uuid, w);
				return w;
			}
			catch (Throwable t)
			{
				return 0;
			}
		}, executor);
	}

	public void invalidate(UUID playerUuid)
	{
		weightCache.remove(playerUuid);
	}
}
