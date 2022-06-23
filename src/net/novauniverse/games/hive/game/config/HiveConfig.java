package net.novauniverse.games.hive.game.config;

import org.json.JSONObject;

import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule;

public class HiveConfig extends MapModule {
	private int gameTime;
	private int maxHoneyInInventory;
	private int honeyRequiredtoFillJar;
	private int flowerRecoveryTime;

	public HiveConfig(JSONObject json) {
		super(json);

		this.gameTime = json.getInt("game_time");
		this.maxHoneyInInventory = json.getInt("max_honey_in_inventory");
		this.honeyRequiredtoFillJar = json.getInt("honey_required_to_fill_jar");
		this.flowerRecoveryTime = json.getInt("flower_recovery_time");
	}

	public int getGameTime() {
		return gameTime;
	}
	
	public int getMaxHoneyInInventory() {
		return maxHoneyInInventory;
	}

	public int getHoneyRequiredtoFillJar() {
		return honeyRequiredtoFillJar;
	}
	
	public int getFlowerRecoveryTime() {
		return flowerRecoveryTime;
	}
}