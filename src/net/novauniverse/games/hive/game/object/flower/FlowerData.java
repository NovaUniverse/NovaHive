package net.novauniverse.games.hive.game.object.flower;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.novauniverse.games.hive.NovaHive;

public class FlowerData {
	private Location location;

	private int collectionRadius;
	private int recoveryTimer;

	private boolean canBeCollected;

	public FlowerData(Location location, int collectionRadius) {
		this.location = location;
		this.collectionRadius = collectionRadius;

		this.canBeCollected = true;
		this.resetTimer();
	}

	public void resetTimer() {
		this.recoveryTimer = NovaHive.getInstance().getGame().getConfig().getFlowerRecoveryTime();
	}

	public void recoveryTick() {
		if (!canBeCollected) {
			recoveryTimer--;
			if (recoveryTimer <= 0) {
				canBeCollected = true;
				this.resetTimer();
			}
		}
	}

	public int getCollectionRadius() {
		return collectionRadius;
	}

	public Location getLocation() {
		return location;
	}

	public int getRecoveryTimer() {
		return recoveryTimer;
	}

	public boolean canBeCollected() {
		return canBeCollected;
	}

	public void collect() {
		canBeCollected = false;
	}

	public boolean canCollect(Player player) {
		if (canBeCollected) {
			if (getLocation().distance(player.getLocation()) <= collectionRadius) {
				return true;
			}
		}
		return false;
	}
}