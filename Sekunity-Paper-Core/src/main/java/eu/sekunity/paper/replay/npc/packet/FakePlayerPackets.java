package eu.sekunity.paper.replay.npc.packet;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * © Copyright 17.01.2026 - 15:51 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class FakePlayerPackets {
	private FakePlayerPackets() {}

	public static void spawnFakePlayer(JavaPlugin plugin, Player viewer, Object npcStateObj) {
		var npc = (ReplayNpcManagerAccessor) npcStateObj;
		try {
			// --- (A) Tablist add ---
			// TODO: Passe die Wrapper an deine konkrete PE 2.11.1 API an, falls nötig.
			// Beispiel-Namen (häufig):
			// new WrapperPlayServerPlayerInfoUpdate(Action.ADD_PLAYER, List.of(...))

			var profile = Bukkit.createProfile(npc.uuid(), npc.entryName());
			// optional: profile.complete(true) async – wenn du sicher Skin willst:
			// Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> profile.complete(true));

			// Wenn du sofort skin willst und online-player verfügbar:
			// Player online = Bukkit.getPlayer(npc.uuid()); if (online!=null) profile = online.getPlayerProfile();

			// -> hier würdest du die Profile/Textures in den PlayerInfo packet schreiben

		} catch (Throwable t) {
			plugin.getLogger().severe("[ReplayNPC] tab add failed: " + t.getMessage());
		}

		// --- (B) Spawn entity + meta ---
		try {
			// -> Spawn packet senden (entityId, uuid, pos, yaw/pitch)
			// -> Metadata senden (skin layers enabled)
		} catch (Throwable t) {
			plugin.getLogger().severe("[ReplayNPC] spawn/meta failed: " + t.getMessage());
		}
	}

	public static void teleportFakePlayer(Player viewer, Object npcStateObj, double x, double y, double z, float yaw, float pitch) {
		var npc = (ReplayNpcManagerAccessor) npcStateObj;

		// Teleport/Look Packets (Wrapper-Namen je nach Version)
		try {
			// Beispiele:
			// WrapperPlayServerEntityTeleport(entityId, x, y, z, yaw, pitch, onGround)
			// WrapperPlayServerEntityHeadLook(entityId, yawByte)
		} catch (Throwable ignored) {}
	}

	public static void despawnFakePlayer(Player viewer, Object npcStateObj) {
		var npc = (ReplayNpcManagerAccessor) npcStateObj;

		try {
			// Destroy entity packet (entityId)
		} catch (Throwable ignored) {}

		try {
			// Tab remove packet (PlayerInfo REMOVE)
		} catch (Throwable ignored) {}
	}

	/**
	 * Damit die private inner class nicht exposed werden muss:
	 * Erstelle optional ein public record NpcDescriptor(...) und nutze das hier stattdessen.
	 */
	public interface ReplayNpcManagerAccessor {
		int entityId();
		UUID uuid();
		String entryName();
	}
}
