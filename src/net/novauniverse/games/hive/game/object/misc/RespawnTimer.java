package net.novauniverse.games.hive.game.object.misc;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.novauniverse.games.hive.game.Hive;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependentUtils;

public class RespawnTimer {
	private int timeLeft;
	private UUID uuid;

	public RespawnTimer(UUID uuid) {
		this.uuid = uuid;
		this.timeLeft = Hive.RESPAWN_TIMER_VALUE;
	}

	public boolean shouldDecrement() {
		Player player = Bukkit.getServer().getPlayer(uuid);
		if (player != null) {
			return player.isOnline();
		}
		return false;
	}

	public void decrement() {
		if (timeLeft > 0) {
			this.showTitle();
			timeLeft--;
		}
	}

	public boolean isDone() {
		return timeLeft <= 0;
	}

	public Player getPlayer() {
		return Bukkit.getPlayer(uuid);
	}

	public void showTitle() {
		if (timeLeft > 0) {
			Player player = Bukkit.getServer().getPlayer(uuid);
			if (player != null) {
				VersionIndependentUtils.get().sendTitle(player, ChatColor.RED + "You died", ChatColor.AQUA + "Respawning in " + timeLeft + " second" + (timeLeft == 1 ? "" : "s"), 0, 25, 0);
			}
		}
	}
}