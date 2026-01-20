package eu.sekunity.paper.replay.npc;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * © Copyright 17.01.2026 - 16:15 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NpcPackets {

	private NpcPackets() {}

	public static WrapperPlayServerPlayerInfo playerInfoAdd(ReplayNpc npc) {
		UserProfile profile = profile(npc); // MUSS textures drin haben (siehe unten!)

		WrapperPlayServerPlayerInfo.PlayerData data =
				new WrapperPlayServerPlayerInfo.PlayerData(
						Component.text(npc.getName()), // displayName optional
						profile,
						GameMode.SURVIVAL,             // !!! PacketEvents GameMode
						0                              // ping
				);

		return new WrapperPlayServerPlayerInfo(
				WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
				List.of(data)
		);
	}

	public static WrapperPlayServerPlayerInfo playerInfoRemove(ReplayNpc npc) {
		UserProfile profile = new UserProfile(npc.getUuid(), npc.getName());

		WrapperPlayServerPlayerInfo.PlayerData data =
				new WrapperPlayServerPlayerInfo.PlayerData(
						null,
						profile,
						null,
						0
				);

		return new WrapperPlayServerPlayerInfo(
				WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER,
				List.of(data)
		);
	}

	public static WrapperPlayServerSpawnPlayer namedEntitySpawn(ReplayNpc npc, Location loc) {
		return new WrapperPlayServerSpawnPlayer(
				npc.getEntityId(),
				npc.getUuid(),
				new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
				loc.getYaw(),
				loc.getPitch(),
				List.of() // entity metadata (optional)
		);
	}

	public static WrapperPlayServerEntityTeleport entityTeleport(ReplayNpc npc, Location loc) {
		return new WrapperPlayServerEntityTeleport(
				npc.getEntityId(),
				new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
				loc.getYaw(),
				loc.getPitch(),
				false
		);
	}

	public static WrapperPlayServerEntityHeadLook entityHeadLook(ReplayNpc npc, Location loc) {
		return new WrapperPlayServerEntityHeadLook(npc.getEntityId(), loc.getYaw());
	}

	public static WrapperPlayServerEntityRotation entityRotation(ReplayNpc npc, Location loc) {
		return new WrapperPlayServerEntityRotation(npc.getEntityId(), loc.getYaw(), loc.getPitch(), false);
	}

	private static UserProfile profile(ReplayNpc npc) {
		UserProfile p = new UserProfile(npc.getUuid(), npc.getName());
		MojangSkinResolver.Skin skin = npc.getSkin();
		if (skin != null) {
			p.getTextureProperties().add(new TextureProperty("textures", skin.value(), skin.signature()));
		}
		return p;
	}

	// ===== Teams: create/update + add/remove entry =====
	public static WrapperPlayServerTeams teamCreate(String teamName,
			Component displayName,
			Component prefix,
			Component suffix,
			NamedTextColor color) {

		var info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
				displayName != null ? displayName : Component.text(teamName),
				prefix != null ? prefix : Component.empty(),
				suffix != null ? suffix : Component.empty(),
				WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
				WrapperPlayServerTeams.CollisionRule.NEVER,
				color != null ? color : NamedTextColor.WHITE,
				WrapperPlayServerTeams.OptionData.NONE
		);

		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.CREATE,
				info,
				Collections.emptyList()
		);
	}

	public static WrapperPlayServerTeams teamUpdate(
			String teamName,
			Component displayName,
			Component prefix,
			Component suffix,
			NamedTextColor color
	) {
		WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
				displayName != null ? displayName : Component.text(teamName),
				prefix != null ? prefix : Component.empty(),
				suffix != null ? suffix : Component.empty(),
				WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
				WrapperPlayServerTeams.CollisionRule.NEVER,
				color != null ? color : NamedTextColor.WHITE,
				WrapperPlayServerTeams.OptionData.NONE
		);

		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.UPDATE,
				info,
				Collections.emptyList()
		);
	}

	public static WrapperPlayServerTeams teamAddEntry(String teamName, String entry) {
		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				entry
		);
	}

	public static WrapperPlayServerTeams teamRemoveEntry(String teamName, String entry) {
		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				entry
		);
	}

	public static WrapperPlayServerTeams teamRemove(String teamName) {
		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.REMOVE,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				new String[0]
		);
	}


	public static WrapperPlayServerTeams teamCreateOrUpdate(
			String teamName,
			Component displayName,
			Component prefix,
			Component suffix,
			NamedTextColor color
	) {
		WrapperPlayServerTeams.ScoreBoardTeamInfo info =
				new WrapperPlayServerTeams.ScoreBoardTeamInfo(
						displayName,
						prefix,
						suffix,
						WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
						WrapperPlayServerTeams.CollisionRule.NEVER,
						color != null ? color : NamedTextColor.WHITE,
						WrapperPlayServerTeams.OptionData.NONE
				);

		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.CREATE, // oder UPDATE wenn du sicher bist, dass es existiert
				info,
				Collections.emptyList()
		);
	}

	public static WrapperPlayServerDestroyEntities destroyEntity(ReplayNpc npc) {
		return new WrapperPlayServerDestroyEntities(npc.getEntityId());
	}

	public static WrapperPlayServerEntityMetadata entityMetadataSkinLayers(ReplayNpc npc) {
		byte flags = (byte) 0x00;          // invisible bit (0x20) darf NICHT gesetzt sein
		byte layers = (byte) 0x7F;         // alle skin layers

		EntityData d0 = new EntityData(0, EntityDataTypes.BYTE, flags);
		EntityData d17 = new EntityData(17, EntityDataTypes.BYTE, layers);

		return new WrapperPlayServerEntityMetadata(npc.getEntityId(), List.of(d0, d17));
	}


	// ===== helpers =====
	public static String teamNameFor(ReplayNpc npc) {
		// <= 16 chars
		String base = "r" + npc.getEntityId();
		return base.length() <= 16 ? base : base.substring(0, 16);
	}

	public static String entryName(ReplayNpc npc) {
		// Team entry ist ein String (Wrapper liest bis 40), ABER: in Tab/ADD_PLAYER ist Name 16.
		// Ich nutze hier denselben 16er Name, damit alles konsistent ist.
		return tabName(npc);
	}

	private static String tabName(ReplayNpc npc) {
		String n = npc.getName();
		if (n == null || n.isBlank()) n = "Replay";
		if (n.length() <= 16) return n;
		int h = Math.abs(npc.getUuid().hashCode());
		String suf = Integer.toHexString(h);
		if (suf.length() > 3) suf = suf.substring(0, 3);
		return n.substring(0, 12) + "_" + suf;
	}
}
