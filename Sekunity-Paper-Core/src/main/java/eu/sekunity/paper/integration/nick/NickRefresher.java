package eu.sekunity.paper.integration.nick;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;

import eu.sekunity.api.service.NickIdentity;

/**
 * © Copyright 12.01.2026 - 19:55 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class NickRefresher
{
	private static final boolean DEBUG = false;

	private final Plugin plugin;

	public NickRefresher(Plugin plugin)
	{
		this.plugin = plugin;
	}

	public void applyNick(Player target, NickIdentity id)
	{
		UUID real = id.realUuid();
		UUID fake = id.fakeUuid();

		log("applyNick target=" + target.getName() + " real=" + real + " fake=" + fake + " fakeName=" + id.fakeName());

		Bukkit.getScheduler().runTask(plugin, () -> {
			int viewers = 0;

			for (Player viewer : Bukkit.getOnlinePlayers())
			{
				if (viewer.equals(target))
				{
					log("applyNick skip self viewer=" + viewer.getName());
					continue;
				}

				viewers++;

				log("applyNick viewer=" + viewer.getName() + " -> sendRemove(real+fake) + hide");
				sendRemove(viewer, real);
				sendRemove(viewer, fake);

				viewer.hidePlayer(plugin, target);
			}

			log("applyNick viewersAffected=" + viewers + " scheduling show in 2 ticks");

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				for (Player viewer : Bukkit.getOnlinePlayers())
				{
					if (viewer.equals(target))
						continue;

					log("applyNick viewer=" + viewer.getName() + " -> show");
					viewer.showPlayer(plugin, target);
				}
			}, 2L);
		});
	}

	public void removeNick(Player target, NickIdentity id)
	{
		UUID real = id.realUuid();
		UUID fake = id.fakeUuid();

		log("removeNick target=" + target.getName() + " real=" + real + " fake=" + fake);

		Bukkit.getScheduler().runTask(plugin, () -> {
			int viewers = 0;

			for (Player viewer : Bukkit.getOnlinePlayers())
			{
				if (viewer.equals(target))
				{
					log("removeNick skip self viewer=" + viewer.getName());
					continue;
				}

				viewers++;

				log("removeNick viewer=" + viewer.getName() + " -> sendRemove(fake+real) + hide");
				sendRemove(viewer, fake);
				sendRemove(viewer, real);

				viewer.hidePlayer(plugin, target);
			}

			log("removeNick viewersAffected=" + viewers + " scheduling show in 2 ticks");

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				for (Player viewer : Bukkit.getOnlinePlayers())
				{
					if (viewer.equals(target))
						continue;

					log("removeNick viewer=" + viewer.getName() + " -> show");
					viewer.showPlayer(plugin, target);
				}
			}, 2L);
		});
	}

	private void sendRemove(Player viewer, UUID profileId)
	{
		log("sendRemove to=" + viewer.getName() + " profileId=" + profileId);
		PacketEvents.getAPI().getPlayerManager().sendPacket(
				viewer,
				new WrapperPlayServerPlayerInfoRemove(List.of(profileId))
		);
	}

	private void log(String msg)
	{
		if (!DEBUG)
			return;

		plugin.getLogger().info("[NickDebug][Refresher] " + msg);
	}
}
