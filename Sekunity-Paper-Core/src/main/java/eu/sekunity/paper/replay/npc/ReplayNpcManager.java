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
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * © Copyright 17.01.2026 - 15:49 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ReplayNpcManager {

	private final JavaPlugin plugin;
	private final PlayerManager playerManager;

	private final Map<UUID, ReplayNpc> npcs = new ConcurrentHashMap<>();
	private final Map<UUID, Set<String>> viewerTeams = new ConcurrentHashMap<>();
	private final Map<UUID, Set<UUID>> viewerSpawnedNpcs = new ConcurrentHashMap<>();

	public ReplayNpcManager(JavaPlugin plugin) {
		this.plugin = plugin;
		this.playerManager = PacketEvents.getAPI().getPlayerManager();
	}

	public void clearAll() {
		for (Player viewer : Bukkit.getOnlinePlayers()) {
			despawnAllFor(viewer);
		}
		npcs.clear();
		viewerTeams.clear();
		viewerSpawnedNpcs.clear();
	}

	public ReplayNpc getOrCreateNpc(UUID uuid, String name) {
		return npcs.computeIfAbsent(uuid, u -> new ReplayNpc(u, safeName(name)));
	}

	/** call when a viewer joins replay (or when replay loads) */
	public void initViewer(Player viewer) {
		viewerTeams.computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
		viewerSpawnedNpcs.computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
	}

	/** spawn (if needed) and update position */
	public void spawnOrMove(Player viewer, ReplayNpc npc, Location loc,
			Component prefix, Component suffix, NamedTextColor nameColor) {

		initViewer(viewer);

		// Team / Prefix (Scoreboard team)
		String teamName = teamNameFor(npc);
		ensureTeam(viewer, teamName, prefix, suffix, nameColor);

		// Entry Name muss exakt der Name sein, den der Client als "entity name" kennt (GameProfile name)
		// -> bei NamedEntitySpawn ist das npc.getName()
		ensureTeamEntry(viewer, teamName, npc.getName());

		// Spawn wenn noch nicht gespawned
		if (!isNpcSpawnedFor(viewer, npc)) {
			send(viewer, NpcPackets.playerInfoAdd(npc));                 // 1) tab add
			send(viewer, NpcPackets.namedEntitySpawn(npc, loc));          // 2) spawn
			send(viewer, NpcPackets.entityTeleport(npc, loc));            // 3) hard-set position
			send(viewer, NpcPackets.entityMetadataSkinLayers(npc));       // 4) metadata (flags+layers)

			markNpcSpawned(viewer, npc);

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				try {
					if (viewer.isOnline()) send(viewer, NpcPackets.playerInfoRemove(npc));
				} catch (Exception ignored) {}
			}, 20L); // 1s reicht meistens (20 ticks)
		} else {
			send(viewer, NpcPackets.entityTeleport(npc, loc));
		}

	}

	public void despawnNpcFor(Player viewer, ReplayNpc npc) {
		if (viewer == null || npc == null) return;
		initViewer(viewer);

		if (isNpcSpawnedFor(viewer, npc)) {
			send(viewer, NpcPackets.destroyEntity(npc));
			unmarkNpcSpawned(viewer, npc);
		}

		// optional: entry raus
		String teamName = teamNameFor(npc);
		if (viewerTeams.containsKey(viewer.getUniqueId()) && viewerTeams.get(viewer.getUniqueId()).contains(teamName)) {
			send(viewer, NpcPackets.teamRemoveEntry(teamName, npc.getName()));
		}
	}

	public void despawnAllFor(Player viewer) {
		if (viewer == null) return;
		initViewer(viewer);

		// NPCs destroy
		for (ReplayNpc npc : npcs.values()) {
			try { send(viewer, NpcPackets.destroyEntity(npc)); } catch (Exception ignored) {}
		}

		// Teams entfernen (optional)
		Set<String> teams = viewerTeams.get(viewer.getUniqueId());
		if (teams != null) {
			for (String team : teams) {
				try { send(viewer, NpcPackets.teamRemove(team)); } catch (Exception ignored) {}
			}
			teams.clear();
		}

		Set<UUID> spawned = viewerSpawnedNpcs.get(viewer.getUniqueId());
		if (spawned != null) spawned.clear();
	}

	// ---------------- intern ----------------

	private void ensureTeam(Player viewer, String teamName,
			Component prefix, Component suffix, NamedTextColor nameColor) {

		Set<String> created = viewerTeams.computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
		if (created.add(teamName)) {
			send(viewer, NpcPackets.teamCreateOrUpdate(
					teamName,
					Component.text(teamName),
					prefix,
					suffix,
					nameColor
			));
		} else {
			// optional: wenn du dynamische Prefixe hast -> UPDATE senden
			send(viewer, NpcPackets.teamUpdate(teamName, Component.text(teamName), prefix, suffix, nameColor));
		}
	}

	private void ensureTeamEntry(Player viewer, String teamName, String entry) {
		// du kannst auch entry-tracking bauen – MVP: einfach ADD schicken, ist billig
		send(viewer, NpcPackets.teamAddEntry(teamName, entry));
	}

	private boolean isNpcSpawnedFor(Player viewer, ReplayNpc npc) {
		return viewerSpawnedNpcs
				.computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
				.contains(npc.getUuid());
	}

	private void markNpcSpawned(Player viewer, ReplayNpc npc) {
		viewerSpawnedNpcs
				.computeIfAbsent(viewer.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
				.add(npc.getUuid());
	}

	private void unmarkNpcSpawned(Player viewer, ReplayNpc npc) {
		Set<UUID> set = viewerSpawnedNpcs.get(viewer.getUniqueId());
		if (set != null) set.remove(npc.getUuid());
	}

	private void send(Player viewer, PacketWrapper<?> wrapper) {
		if (viewer == null || wrapper == null) return;
		playerManager.sendPacket(viewer, wrapper);
	}

	private static String teamNameFor(ReplayNpc npc) {
		// max team length ist je nach version groß genug; trotzdem kurz halten
		return "rp_" + npc.getUuid().toString().replace("-", "").substring(0, 12);
	}

	private static String safeName(String name) {
		if (name == null || name.isBlank()) return "Replay";
		// Vanilla limit 16 chars für GameProfile name
		return name.length() > 16 ? name.substring(0, 16) : name;
	}
}
