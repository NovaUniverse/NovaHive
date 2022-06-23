package net.novauniverse.games.hive.game.object.hive;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import net.novauniverse.games.hive.NovaHive;
import net.zeeraa.novacore.spigot.teams.Team;
import net.zeeraa.novacore.spigot.teams.TeamManager;

public class HiveData {
	private Team owner;

	private int honey;
	private int honeyJarRadius;
	private int honeyJarHeight;
	private int depositRadius;

	private boolean completed;

	private Location honeyJarLocation;
	private Location spawnLocation;

	public HiveData(Team owner, int honeyJarHeight, int depositRadius, Location jarCenter, Location spawnLocation) {
		this.owner = owner;

		this.honey = 0;
		this.honeyJarHeight = honeyJarHeight;
		this.depositRadius = depositRadius;

		this.completed = false;

		this.spawnLocation = spawnLocation;
		this.honeyJarLocation = jarCenter;
	}

	public int getHoney() {
		return honey;
	}

	public Location getHoneyJarLocation() {
		return honeyJarLocation;
	}

	public int getHoneyJarRadius() {
		return honeyJarRadius;
	}

	public int getDepositRadius() {
		return depositRadius;
	}

	public int getHoneyJarHeight() {
		return honeyJarHeight;
	}

	public Team getOwner() {
		return owner;
	}

	public Location getSpawnLocation() {
		return spawnLocation;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public boolean canDeposit(Player player) {
		Team team = TeamManager.getTeamManager().getPlayerTeam(player);
		if (team != null) {
			if (owner.equals(team)) {
				return this.isInRange(player);
			}
		}

		return false;
	}

	public boolean isInRange(Player player) {
		if (player.getLocation().getWorld() == honeyJarLocation.getWorld()) {
			if (player.getLocation().distance(honeyJarLocation) <= depositRadius) {
				return true;
			}
		}
		return false;
	}

	public void updateJar() {
		double perStep = (double) NovaHive.getInstance().getGame().getConfig().getHoneyRequiredtoFillJar() / (double) honeyJarHeight;
		int blocksFilled = (int) Math.floor(honey / perStep);

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int i = 0; i < honeyJarHeight; i++) {
					Location location = honeyJarLocation.clone().add(x, i, z);
					boolean reached = (i + 1) < blocksFilled;

					Block block = location.getBlock();
					if (reached) {
						if (block.getType() == Material.AIR) {
							Material material = Material.HONEY_BLOCK;
							if (NovaHive.getInstance().getGame().getRandom().nextBoolean()) {
								material = Material.HONEYCOMB_BLOCK;
							}
							block.setType(material);
						}
					} else {
						block.setType(Material.AIR);
					}
				}
			}
		}
	}

	public void addHoney(int amount) {
		int max = NovaHive.getInstance().getGame().getConfig().getHoneyRequiredtoFillJar();
		if ((this.honey + amount) > max) {
			this.honey = max;
		} else {
			this.honey += amount;
		}
	}
}