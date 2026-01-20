package eu.sekunity.paper.replay.npc;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * © Copyright 17.01.2026 - 16:14 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ReplayNpc {

	private final UUID uuid;
	private final String name;
	private final int entityId;

	private volatile MojangSkinResolver.Skin skin;

	private volatile Location lastLoc;
	private volatile String lastPrefix;
	private volatile String lastSuffix;
	private volatile NamedTextColor lastColor;

	public ReplayNpc(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
		this.entityId = ThreadLocalRandom.current().nextInt(2_000_000, 50_000_000);
	}

	public UUID getUuid() { return uuid; }
	public String getName() { return name; }
	public int getEntityId() { return entityId; }

	public MojangSkinResolver.Skin getSkin() { return skin; }
	public void setSkin(MojangSkinResolver.Skin skin) { this.skin = skin; }

	public Location getLastLoc() { return lastLoc; }
	public void setLastLoc(Location lastLoc) { this.lastLoc = lastLoc; }

	public String getLastPrefix() { return lastPrefix; }
	public void setLastPrefix(String lastPrefix) { this.lastPrefix = lastPrefix; }

	public String getLastSuffix() { return lastSuffix; }
	public void setLastSuffix(String lastSuffix) { this.lastSuffix = lastSuffix; }

	public NamedTextColor getLastColor()
	{
		return lastColor;
	}

	public void setLastColor(NamedTextColor lastColor)
	{
		this.lastColor = lastColor;
	}
}
