package eu.sekunity.paper.service.nick;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import eu.sekunity.api.service.nick.NickCache;
import eu.sekunity.api.service.nick.NickIdentity;

/**
 * © Copyright 11.01.2026 - 20:05 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */

/**
 * Rewrites outgoing PLAYER_INFO_UPDATE packets so that viewers see fake UUID, fake name and fake skin for nicked
 * players.
 *
 * Reads ONLY from NickCache (no DB access).
 */
public final class NickPacketListener extends PacketListenerAbstract
{
	private static final boolean DEBUG = false;

	private final NickCache cache;
	private final Plugin plugin;
	private final NickVisibility visibility;

	public NickPacketListener(Plugin plugin, NickCache cache, NickVisibility visibility)
	{
		super(PacketListenerPriority.HIGHEST);
		this.plugin = plugin;
		this.cache = cache;
		this.visibility = visibility;
	}

	@Override
	public void onPacketSend(PacketSendEvent event)
	{
		var pt = event.getPacketType();

		if (pt == PacketType.Play.Server.PLAYER_INFO_UPDATE)
		{
			handlePlayerInfoUpdate(event);
			return;
		}

		if (pt == PacketType.Play.Server.PLAYER_INFO_REMOVE)
		{
			handlePlayerInfoRemove(event);
			return;
		}

		if (pt == PacketType.Play.Server.SPAWN_ENTITY)
		{
			handleSpawnEntity(event);
			return;
		}

		if (pt == PacketType.Play.Server.DESTROY_ENTITIES)
		{
			log(event, "SEEN DESTROY_ENTITIES");
		}
	}

	private void handlePlayerInfoUpdate(PacketSendEvent event)
	{
		UUID viewer = viewerUuid(event);

		WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
		List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> entries = wrapper.getEntries();
		if (entries == null || entries.isEmpty())
			return;

		boolean changed = false;
		List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> out = new ArrayList<>(entries.size());

		for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo e : entries)
		{
			UUID realUuid = e.getProfileId();
			Optional<NickIdentity> opt = cache.getIfEnabled(realUuid);

			if (opt.isEmpty())
			{
				out.add(e);
				continue;
			}

			if (viewer != null && !visibility.shouldSeeNick(viewer, realUuid))
			{
				log(event, "PLAYER_INFO_UPDATE keep REAL viewer=" + viewer + " target=" + realUuid);
				out.add(e);
				continue;
			}

			NickIdentity ni = opt.get();
			if (ni.fakeUuid() == null || ni.fakeName() == null || ni.skinValue() == null || ni.skinSignature() == null)
			{
				out.add(e);
				continue;
			}

			log(event, "PLAYER_INFO_UPDATE rewrite viewer=" + viewer + " real=" + realUuid + " -> fake=" + ni.fakeUuid()
					+ " fakeName=" + ni.fakeName());

			UserProfile fakeProfile = new UserProfile(ni.fakeUuid(), ni.fakeName(),
					List.of(new TextureProperty("textures", ni.skinValue(), ni.skinSignature())));

			WrapperPlayServerPlayerInfoUpdate.PlayerInfo replaced = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
					fakeProfile, e.isListed(), e.getLatency(), e.getGameMode(), e.getDisplayName(), e.getChatSession(),
					e.getListOrder(), e.isShowHat());

			out.add(replaced);
			changed = true;
		}

		if (!changed)
			return;

		wrapper.setEntries(out);
		wrapper.write();
	}

	private void handlePlayerInfoRemove(PacketSendEvent event)
	{
		UUID viewer = viewerUuid(event);

		WrapperPlayServerPlayerInfoRemove wrapper = new WrapperPlayServerPlayerInfoRemove(event);
		List<UUID> uuids = wrapper.getProfileIds();
		if (uuids == null || uuids.isEmpty())
			return;

		boolean changed = false;
		List<UUID> out = new ArrayList<>(uuids.size() * 2);

		for (UUID id : uuids)
		{
			Optional<NickIdentity> byReal = cache.getIfEnabled(id);
			if (byReal.isPresent())
			{
				NickIdentity ni = byReal.get();

				if (viewer != null && !visibility.shouldSeeNick(viewer, ni.realUuid()))
				{
					log(event, "PLAYER_INFO_REMOVE keep REAL viewer=" + viewer + " id=" + id);
					out.add(id);
					continue;
				}

				if (ni.fakeUuid() == null)
				{
					out.add(id);
					continue;
				}

				log(event, "PLAYER_INFO_REMOVE expand viewer=" + viewer + " id=" + id + " -> remove(real+fake) real="
						+ ni.realUuid() + " fake=" + ni.fakeUuid());
				out.add(ni.realUuid());
				out.add(ni.fakeUuid());
				changed = true;
				continue;
			}

			Optional<NickIdentity> byFake = cache.getIfEnabledByFake(id);
			if (byFake.isPresent())
			{
				NickIdentity ni = byFake.get();

				if (viewer != null && !visibility.shouldSeeNick(viewer, ni.realUuid()))
				{
					log(event, "PLAYER_INFO_REMOVE keep REAL viewer=" + viewer + " id=" + id);
					out.add(id);
					continue;
				}

				if (ni.fakeUuid() == null)
				{
					out.add(id);
					continue;
				}

				log(event, "PLAYER_INFO_REMOVE expand viewer=" + viewer + " id=" + id + " -> remove(real+fake) real="
						+ ni.realUuid() + " fake=" + ni.fakeUuid());
				out.add(ni.realUuid());
				out.add(ni.fakeUuid());
				changed = true;
				continue;
			}

			out.add(id);
		}

		if (!changed)
			return;

		wrapper.setProfileIds(out);
		wrapper.write();
	}

	private void handleSpawnEntity(PacketSendEvent event)
	{
		UUID viewer = viewerUuid(event);

		WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);

		EntityType type;
		try
		{
			type = wrapper.getEntityType();
		} catch (Throwable t)
		{
			log(event, "SPAWN_ENTITY failed read type: " + t.getClass().getSimpleName() + " " + t.getMessage());
			return;
		}

		String typeName;
		try
		{
			typeName = type.getName().toString();
		} catch (Throwable ignored)
		{
			typeName = String.valueOf(type);
		}

		Optional<UUID> optUuid;
		try
		{
			optUuid = wrapper.getUUID();
		} catch (Throwable t)
		{
			log(event, "SPAWN_ENTITY failed read uuid: " + t.getClass().getSimpleName() + " " + t.getMessage());
			return;
		}

		if (optUuid.isEmpty())
		{
			if (typeName != null && typeName.toUpperCase().contains("PLAYER"))
				log(event, "SPAWN_ENTITY looks like PLAYER but uuid empty, type=" + typeName);
			return;
		}

		UUID spawnUuid = optUuid.get();

		boolean looksPlayer = typeName != null && typeName.toUpperCase().contains("PLAYER");
		if (!looksPlayer)
			return;

		log(event, "SPAWN_ENTITY PLAYER spawn uuid=" + spawnUuid + " type=" + typeName + " viewer=" + viewer);

		Optional<NickIdentity> optNick = cache.getIfEnabled(spawnUuid);
		if (optNick.isEmpty())
			optNick = cache.getIfEnabledByFake(spawnUuid);

		if (optNick.isEmpty())
		{
			log(event, "SPAWN_ENTITY PLAYER cache MISS for uuid=" + spawnUuid);
			return;
		}

		NickIdentity ni = optNick.get();
		if (ni.fakeUuid() == null)
			return;

		if (viewer != null && !visibility.shouldSeeNick(viewer, ni.realUuid()))
		{
			log(event, "SPAWN_ENTITY PLAYER keep REAL viewer=" + viewer + " target=" + ni.realUuid());
			return;
		}

		log(event, "SPAWN_ENTITY PLAYER rewrite viewer=" + viewer + " uuid " + spawnUuid + " -> " + ni.fakeUuid());

		try
		{
			wrapper.setUUID(Optional.of(ni.fakeUuid()));
			wrapper.write();
		} catch (Throwable t)
		{
			log(event, "SPAWN_ENTITY PLAYER setUUID failed: " + t.getClass().getSimpleName() + " " + t.getMessage());
		}
	}

	private void log(PacketSendEvent event, String msg)
	{
		if (!DEBUG)
			return;

		String receiver = "?";
		try
		{
			var user = event.getUser();
			if (user != null)
			{
				var p = Bukkit.getPlayer(user.getUUID());
				receiver = (p != null) ? p.getName() : user.getUUID().toString();
			}
		} catch (Throwable ignored)
		{
		}

		plugin.getLogger().info("[NickDebug][Packets][to=" + receiver + "] " + msg);
	}

	private UUID viewerUuid(PacketSendEvent event)
	{
		try
		{
			var user = event.getUser();
			if (user != null)
				return user.getUUID();
		} catch (Throwable ignored)
		{
		}
		return null;
	}
}
