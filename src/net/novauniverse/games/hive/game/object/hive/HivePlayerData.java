package net.novauniverse.games.hive.game.object.hive;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import net.novauniverse.games.hive.game.Hive;

public class HivePlayerData {
	private Player player;
	private int collectionTimeLeft;
	private boolean collecting;

	private BossBar bossBar;

	public HivePlayerData(Player player) {
		this.player = player;
		this.collecting = false;
		this.resetCollectionTime();

		this.bossBar = Bukkit.createBossBar(ChatColor.GREEN + "Collecting honey", BarColor.BLUE, BarStyle.SEGMENTED_10);
		this.bossBar.addPlayer(player);
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

	public void updateBossBar() {
		bossBar.setProgress(collectionTimeLeft / Hive.COLLECTION_TIME);
	}

	public void setCollecting(boolean collecting) {
		this.collecting = collecting;
		bossBar.setVisible(collecting);
		updateBossBar();
	}

	public int decrementCollectionTimer() {
		collectionTimeLeft--;
		updateBossBar();
		return collectionTimeLeft;
	}
}