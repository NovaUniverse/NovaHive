package net.novauniverse.games.hive.game.object.misc;

import java.util.Comparator;

import org.bukkit.entity.Player;

import net.novauniverse.games.hive.game.object.flower.FlowerData;

public class PlayerFlowerDistanceComparator implements Comparator<FlowerData> {
	private Player player;

	public PlayerFlowerDistanceComparator(Player player) {
		this.player = player;
	}

	@Override
	public int compare(FlowerData o1, FlowerData o2) {
		int flower1Dist = (int) Math.round(o1.getLocation().distance(player.getLocation()));
		int flower2Dist = (int) Math.round(o2.getLocation().distance(player.getLocation()));
		return flower1Dist - flower2Dist;
	}
}