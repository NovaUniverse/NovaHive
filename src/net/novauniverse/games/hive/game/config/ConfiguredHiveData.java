package net.novauniverse.games.hive.game.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.json.JSONObject;

import net.novauniverse.games.hive.game.object.hive.HiveData;
import net.zeeraa.novacore.spigot.teams.Team;
import net.zeeraa.novacore.spigot.utils.LocationUtils;
import net.zeeraa.novacore.spigot.utils.XYZLocation;

public class ConfiguredHiveData {
	private int honeyJarRadius;
	private int honeyJarHeight;
	private int depositRadius;

	private XYZLocation honeyJarLocation;
	private Location spawnLocation;

	public ConfiguredHiveData(JSONObject json) {
		honeyJarHeight = 5;
		honeyJarRadius = 1;
		depositRadius = 6;

		if (json.has("jar_height")) {
			honeyJarHeight = json.getInt("jar_height");
		}

		if (json.has("jar_radius")) {
			depositRadius = json.getInt("jar_radius");
		}

		if (json.has("deposit_radius")) {
			depositRadius = json.getInt("deposit_radius");
		}

		honeyJarLocation = new XYZLocation(json.getJSONObject("jar_center"));
		spawnLocation = LocationUtils.fromJSONObject(json.getJSONObject("spawn_location"), Bukkit.getServer().getWorlds().stream().findFirst().get());
	}

	public HiveData toHiveData(Team owner, World world) {
		spawnLocation.setWorld(world);
		return new HiveData(owner, honeyJarHeight, honeyJarRadius, depositRadius, honeyJarLocation.toBukkitLocation(world), spawnLocation);
	}
}