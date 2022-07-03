package net.novauniverse.games.hive.game.object.misc;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.novauniverse.games.hive.game.Hive;

public class RespawnTimer {
	private int timeLeft;
	private UUID uuid;

	public RespawnTimer(UUID uuid) {
		this.uuid = uuid;
		this.timeLeft = Hive.RESPAWN_TIMER;
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
			timeLeft--;
		}
	}

	public boolean isDone() {
		return timeLeft <= 0;
	}

	public Player getPlayer() {
		return Bukkit.getPlayer(uuid);
	}
}