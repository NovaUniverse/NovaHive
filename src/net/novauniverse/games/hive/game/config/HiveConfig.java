package net.novauniverse.games.hive.game.config;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.utils.XYZLocation;

public class HiveConfig extends MapModule {
	private int gameTime;
	private int maxHoneyInInventory;
	private int honeyRequiredtoFillJar;
	private int flowerRecoveryTime;

	private List<ConfiguredHiveData> hives;
	private List<XYZLocation> flowers;

	public HiveConfig(JSONObject json) {
		super(json);

		this.gameTime = json.getInt("game_time");
		this.maxHoneyInInventory = json.getInt("max_honey_in_inventory");
		this.honeyRequiredtoFillJar = json.getInt("honey_required_to_fill_jar");
		this.flowerRecoveryTime = json.getInt("flower_recovery_time");

		this.hives = new ArrayList<>();

		JSONArray hivesJson = json.getJSONArray("hives");
		for (int i = 0; i < hivesJson.length(); i++) {
			hives.add(new ConfiguredHiveData(hivesJson.getJSONObject(i)));
		}

		flowers = new ArrayList<>();
		JSONArray flowersArray = json.getJSONArray("flowers");
		for (int i = 0; i < flowersArray.length(); i++) {
			flowers.add(XYZLocation.fromJSON(flowersArray.getJSONObject(i)));
		}
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

	public List<ConfiguredHiveData> getHives() {
		return hives;
	}
	
	public List<XYZLocation> getFlowers() {
		return flowers;
	}
}