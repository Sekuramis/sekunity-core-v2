package eu.sekunity.paper.replay.npc;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;

import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * © Copyright 17.01.2026 - 19:07 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ReplayNpcController {

	private final JavaPlugin plugin;
	private final PlayerManager playerManager;

	// viewerUUID -> set of spawned npcUUID
	private final Map<UUID, Set<UUID>> spawned = new ConcurrentHashMap<>();

	public ReplayNpcController(JavaPlugin plugin) {
		this.plugin = plugin;
		this.playerManager = PacketEvents.getAPI().getPlayerManager();
	}

	public void spawnFor(Player viewer, ReplayNpc npc, Location loc) {
		if (viewer == null || !viewer.isOnline()) return;

		spawned.computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
		if (spawned.get(viewer.getUniqueId()).contains(npc.getUuid())) {
			// already spawned, just teleport
			teleportFor(viewer, npc, loc);
			return;
		}

		// 1) Tab add (für skin resolution)
		send(viewer, NpcPackets.playerInfoAdd(npc));

		// 2) Spawn
		send(viewer, NpcPackets.namedEntitySpawn(npc, loc));

		// 3) Metadata skin layers (optional aber empfohlen)
		Object meta = NpcPackets.entityMetadataSkinLayers(npc);
		if (meta != null) send(viewer, meta);

		// 4) Team prefix/suffix + add entry
		String teamName = teamNameFor(npc);
		send(viewer, NpcPackets.teamCreateOrUpdate(
				teamName,
				net.kyori.adventure.text.Component.text(teamName),
				MiniMessage.miniMessage().deserialize(npc.getLastPrefix()),
				MiniMessage.miniMessage().deserialize(npc.getLastSuffix()),
				npc.getLastColor()
		));
		send(viewer, NpcPackets.teamAddEntry(teamName, npc.getName()));

		// 5) nach 40 ticks aus Tab entfernen (NPC bleibt sichtbar)
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (!viewer.isOnline()) return;
			send(viewer, NpcPackets.playerInfoRemove(npc));
		}, 40L);

		spawned.get(viewer.getUniqueId()).add(npc.getUuid());
	}

	public void teleportFor(Player viewer, ReplayNpc npc, Location loc) {
		if (viewer == null || !viewer.isOnline()) return;
		send(viewer, NpcPackets.entityTeleport(npc, loc));
	}

	public void destroyFor(Player viewer, ReplayNpc npc) {
		if (viewer == null || !viewer.isOnline()) return;

		// remove entry + remove team (best effort)
		String teamName = teamNameFor(npc);
		send(viewer, NpcPackets.teamRemoveEntry(teamName, npc.getName()));
		send(viewer, NpcPackets.teamRemove(teamName));

		Object destroy = NpcPackets.destroyEntity(npc);
		if (destroy != null) send(viewer, destroy);

		send(viewer, NpcPackets.playerInfoRemove(npc));

		Set<UUID> set = spawned.get(viewer.getUniqueId());
		if (set != null) set.remove(npc.getUuid());
	}

	public void destroyAllViewers(ReplayNpc npc) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			destroyFor(p, npc);
		}
	}

	public void clearAll() {
		spawned.clear();
	}

	private void send(Player viewer, Object wrapper) {
		if (wrapper instanceof com.github.retrooper.packetevents.wrapper.PacketWrapper<?> pw) {
			playerManager.sendPacket(viewer, pw);
		}
	}

	private static String teamNameFor(ReplayNpc npc) {
		String base = "r_" + npc.getUuid().toString().replace("-", "");
		return base.substring(0, Math.min(16, base.length()));
	}
}
