package eu.sekunity.paper.replay.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

import eu.sekunity.paper.commands.admin.AdminCommandReplay;
import eu.sekunity.paper.replay.export.timeline.TimelinePlayer;

/**
 * © Copyright 17.01.2026 - 14:44 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ReplayHotbarController implements Listener {

	private static final String PERM_JOIN = "sekunity.replay.join";

	// Slots
	private static final int SLOT_BACK_10 = 0;
	private static final int SLOT_BACK_1  = 1;
	private static final int SLOT_PAUSE   = 2;
	private static final int SLOT_RESUME  = 3;
	private static final int SLOT_FWD_1   = 4;
	private static final int SLOT_FWD_10  = 5;
	private static final int SLOT_STOP    = 8;

	// “IDs” via custom model data / displayname check
	private static final String TAG = ChatColor.DARK_GRAY + "ReplayControl";

	private final JavaPlugin plugin;
	private final AdminCommandReplay replayCmd;

	private final Map<UUID, Long> clickCooldown = new HashMap<>();

	public ReplayHotbarController(JavaPlugin plugin, AdminCommandReplay replayCmd) {
		this.plugin = plugin;
		this.replayCmd = replayCmd;
	}

	public void register() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	// Call this after /replay load finished and you teleport viewers
	public void giveControls(Player p) {
		if (!p.hasPermission(PERM_JOIN)) return;

		p.getInventory().setItem(SLOT_BACK_10, item(Material.ARROW, ChatColor.AQUA + "⏮ -10s", TAG));
		p.getInventory().setItem(SLOT_BACK_1,  item(Material.ARROW, ChatColor.YELLOW + "⏪ -1s", TAG));
		p.getInventory().setItem(SLOT_PAUSE,   item(Material.CLOCK, ChatColor.RED + "⏸ Pause", TAG));
		p.getInventory().setItem(SLOT_RESUME,  item(Material.LIME_DYE, ChatColor.GREEN + "▶ Resume", TAG));
		p.getInventory().setItem(SLOT_FWD_1,   item(Material.ARROW, ChatColor.YELLOW + "⏩ +1s", TAG));
		p.getInventory().setItem(SLOT_FWD_10,  item(Material.ARROW, ChatColor.AQUA + "⏭ +10s", TAG));
		p.getInventory().setItem(SLOT_STOP,    item(Material.BARRIER, ChatColor.DARK_RED + "⏹ Stop", TAG));

		// Zuschauer-Setup
		p.setGameMode(GameMode.CREATIVE);
	}

	public void clearControls(Player p) {
		for (int slot : new int[]{SLOT_BACK_10, SLOT_BACK_1, SLOT_PAUSE, SLOT_RESUME, SLOT_FWD_1, SLOT_FWD_10, SLOT_STOP}) {
			ItemStack it = p.getInventory().getItem(slot);
			if (isReplayItem(it)) p.getInventory().setItem(slot, null);
		}
	}

	private ItemStack item(Material mat, String name, String tagLine) {
		ItemStack it = new ItemStack(mat);
		ItemMeta im = it.getItemMeta();
		if (im != null) {
			im.setDisplayName(name);
			im.setLore(List.of(tagLine));
			it.setItemMeta(im);
		}
		return it;
	}

	private boolean isReplayItem(ItemStack it) {
		if (it == null) return false;
		if (!it.hasItemMeta()) return false;
		ItemMeta im = it.getItemMeta();
		if (im == null) return false;
		List<String> lore = im.getLore();
		if (lore == null || lore.isEmpty()) return false;
		return lore.contains(TAG);
	}

	private boolean cooldown(Player p) {
		long now = System.currentTimeMillis();
		long last = clickCooldown.getOrDefault(p.getUniqueId(), 0L);
		if (now - last < 200) return true; // 200ms
		clickCooldown.put(p.getUniqueId(), now);
		return false;
	}

	private void action(Player p, Runnable r) {
		if (cooldown(p)) return;

		if (!replayCmd.isPlayback()) {
			p.sendMessage(ChatColor.RED + "Not a playback server.");
			return;
		}
		if (!replayCmd.hasLoadedReplay()) {
			p.sendMessage(ChatColor.RED + "No replay loaded.");
			return;
		}

		TimelinePlayer t = replayCmd.timeline();
		if (t == null || !t.isLoaded()) {
			p.sendMessage(ChatColor.RED + "Timeline not running.");
			return;
		}

		r.run();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		if (!p.hasPermission(PERM_JOIN)) return;

		// Wenn gerade ein Replay geladen ist, beim Join direkt controls geben
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (replayCmd.isPlayback() && replayCmd.hasLoadedReplay()) {
				giveControls(p);
				if (replayCmd.replayCenter() != null) p.teleport(replayCmd.replayCenter());
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		ItemStack it = e.getItem();
		if (!isReplayItem(it)) return;

		e.setCancelled(true);

		int slot = p.getInventory().getHeldItemSlot();

		switch (slot) {
		case SLOT_BACK_10 -> action(p, () -> {
			replayCmd.timeline().jumpBySeconds(-10);
			p.sendActionBar(ChatColor.GRAY + "⏮ -10s");
		});
		case SLOT_BACK_1 -> action(p, () -> {
			replayCmd.timeline().jumpBySeconds(-1);
			p.sendActionBar(ChatColor.GRAY + "⏪ -1s");
		});
		case SLOT_PAUSE -> action(p, () -> {
			replayCmd.timeline().setPaused(true);
			p.sendActionBar(ChatColor.RED + "Paused");
		});
		case SLOT_RESUME -> action(p, () -> {
			replayCmd.timeline().setPaused(false);
			p.sendActionBar(ChatColor.GREEN + "Running");
		});
		case SLOT_FWD_1 -> action(p, () -> {
			replayCmd.timeline().jumpBySeconds(+1);
			p.sendActionBar(ChatColor.GRAY + "⏩ +1s");
		});
		case SLOT_FWD_10 -> action(p, () -> {
			replayCmd.timeline().jumpBySeconds(+10);
			p.sendActionBar(ChatColor.GRAY + "⏭ +10s");
		});
		case SLOT_STOP -> {
			if (cooldown(p)) return;
			if (!replayCmd.isPlayback()) return;
			replayCmd.stopReplayFromUi(p);
			p.sendActionBar(ChatColor.YELLOW + "Stopped");
		}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInvClick(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player p)) return;
		ItemStack it = e.getCurrentItem();
		if (!isReplayItem(it)) return;
		e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDrop(PlayerDropItemEvent e) {
		if (!isReplayItem(e.getItemDrop().getItemStack())) return;
		e.setCancelled(true);
	}
}
