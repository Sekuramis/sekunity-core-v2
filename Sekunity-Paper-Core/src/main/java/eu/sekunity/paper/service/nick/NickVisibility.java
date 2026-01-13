package eu.sekunity.paper.service.nick;

import java.util.UUID;

import eu.sekunity.paper.integration.luckperms.LuckPermsAdapter;

/**
 * © Copyright 13.01.2026 - 18:20 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickVisibility
{
	private final LuckPermsAdapter luckPerms;

	public NickVisibility(LuckPermsAdapter luckPerms)
	{
		this.luckPerms = luckPerms;
	}

	public boolean shouldSeeNick(UUID viewerUuid, UUID targetUuid)
	{
		int vw = luckPerms.weight(viewerUuid);
		int tw = luckPerms.weight(targetUuid);
		return vw < tw;
	}
}
