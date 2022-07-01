package net.novauniverse.games.hive.game.object.hive;

import org.bukkit.entity.Player;

import net.novauniverse.games.hive.game.Hive;

public class HivePlayerData {
	private Player player;
	private int collectionTimeLeft;
	private boolean collecting;

	public HivePlayerData(Player player) {
		this.player = player;
		this.collecting = false;
		this.resetCollectionTime();
	}

	public Player getPlayer() {
		return player;
	}

	public int getCollectionTimeLeft() {
		return collectionTimeLeft;
	}

	public void resetCollectionTime() {
		this.collectionTimeLeft = Hive.COLLECTION_TIME;
	}

	public boolean isCollecting() {
		return collecting;
	}

	public void setCollecting(boolean collecting) {
		this.collecting = collecting;
	}

	public int decrementCollectionTimer() {
		collectionTimeLeft--;
		return collectionTimeLeft;
	}
}