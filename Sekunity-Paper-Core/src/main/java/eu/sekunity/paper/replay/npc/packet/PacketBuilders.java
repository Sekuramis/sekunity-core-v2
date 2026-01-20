package eu.sekunity.paper.replay.npc.packet;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import eu.sekunity.paper.replay.npc.ActorProfile;
import eu.sekunity.paper.replay.npc.MojangSkinResolver;
import eu.sekunity.paper.replay.npc.ReplayNpc;

/**
 * © Copyright 17.01.2026 - 15:14 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class PacketBuilders {

	private PacketBuilders() {}

	// =========================
	// PlayerInfo ADD with skin
	// =========================
	public static WrapperPlayServerPlayerInfo playerInfoAdd(UUID uuid, String name, @Nullable ActorProfile prof) {

		UserProfile up = new UserProfile(uuid, name);

		if (prof != null && prof.skin() != null) {
			up.getTextureProperties().add(
					new TextureProperty(
							"textures",
							prof.skin().value(),
							prof.skin().signature()
					)
			);
		}


		WrapperPlayServerPlayerInfo.PlayerData data =
				new WrapperPlayServerPlayerInfo.PlayerData(
						null,               // displayName
						up,                 // UserProfile
						GameMode.SURVIVAL,  // gameMode
						0                   // ping
				);


		return new WrapperPlayServerPlayerInfo(
				WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
				List.of(data)
		);
	}

	// =========================
	// Teams: create + add entry
	// =========================

	public static WrapperPlayServerTeams teamCreate(String teamName, String prefixLegacy) {
		Component display = Component.text(teamName);

		Component prefix = legacyToComponent(prefixLegacy);

		WrapperPlayServerTeams.ScoreBoardTeamInfo info =
				new WrapperPlayServerTeams.ScoreBoardTeamInfo(
						Component.text(teamName),           // displayName
						prefix,                     // prefix
						Component.empty(),                   // suffix
						WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
						WrapperPlayServerTeams.CollisionRule.NEVER,
						NamedTextColor.WHITE,
						WrapperPlayServerTeams.OptionData.ALL // <-- WICHTIG
				);

		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.CREATE,
				info,
				List.of() // entries empty on create
		);
	}

	public static WrapperPlayServerTeams teamAddEntry(String teamName, String entry) {
		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
				Optional.empty(),
				List.of(entry)
		);
	}

	public static WrapperPlayServerTeams teamRemove(String teamName) {
		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.REMOVE,
				Optional.empty(),
				List.of()
		);
	}

	public static UserProfile profile(ReplayNpc npc) {
		UserProfile p = new UserProfile(npc.getUuid(), npc.getName());

		MojangSkinResolver.Skin skin = npc.getSkin();
		if (skin != null && skin.value() != null && !skin.value().isBlank()) {
			// name muss "textures" sein
			p.getTextureProperties().add(new TextureProperty(
					"textures",
					skin.value(),
					(skin.signature() == null || skin.signature().isBlank()) ? null : skin.signature()
			));
		}
		return p;
	}

	// =========================================================
	// LEGACY -> Component (minimal, damit es sofort läuft)
	// =========================================================
	private static Component legacyToComponent(String legacy) {
		if (legacy == null || legacy.isEmpty()) return Component.empty();

		// Minimal: strip §-Farben und setze weiß (MVP)
		// Wenn du echte Farben willst, sag Bescheid, dann mappe ich § codes auf Components.
		String plain = legacy.replaceAll("§[0-9a-fk-or]", "");
		return Component.text(plain);
	}
}
