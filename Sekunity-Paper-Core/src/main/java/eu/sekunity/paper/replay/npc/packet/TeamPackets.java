package eu.sekunity.paper.replay.npc.packet;

import java.util.Collection;
import java.util.Collections;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
/**
 * © Copyright 17.01.2026 - 15:23 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class TeamPackets {

	private TeamPackets() {}

	public static WrapperPlayServerTeams teamCreate(
			String teamName,
			WrapperPlayServerTeams.ScoreBoardTeamInfo info,
			Collection<String> initialEntries
	) {
		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.CREATE,
				info,
				initialEntries == null ? Collections.emptyList() : initialEntries
		);
	}

	public static WrapperPlayServerTeams teamUpdate(String teamName, WrapperPlayServerTeams.ScoreBoardTeamInfo info) {
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
				Collections.singletonList(entry)
		);
	}

	public static WrapperPlayServerTeams teamRemoveEntry(String teamName, String entry) {
		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				Collections.singletonList(entry)
		);
	}

	public static WrapperPlayServerTeams teamRemove(String teamName) {
		return new WrapperPlayServerTeams(
				teamName,
				WrapperPlayServerTeams.TeamMode.REMOVE,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				Collections.emptyList()
		);
	}

}
