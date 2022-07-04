package net.novauniverse.games.hive.game.object.hive;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;

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

	private Hologram hologram;

	private BossBar bossBar;

	public HiveData(Team owner, int honeyJarHeight, int honeyJarRadius, int depositRadius, Location jarCenter, Location spawnLocation) {
		this.owner = owner;

		this.honey = 0;
		this.honeyJarHeight = honeyJarHeight;
		this.depositRadius = depositRadius;
		this.honeyJarRadius = honeyJarRadius;

		this.completed = false;

		this.spawnLocation = spawnLocation;
		this.honeyJarLocation = jarCenter;

		this.bossBar = Bukkit.createBossBar(getBarText(), BarColor.GREEN, BarStyle.SOLID);
		this.bossBar.setProgress(0);

		this.hologram = HologramsAPI.createHologram(NovaHive.getInstance(), spawnLocation.clone().add(0D, 2.5D, 0D));
		hologram.appendTextLine(owner.getTeamColor() + owner.getDisplayName());
		hologram.appendTextLine("Loading...");

		this.updateHologram();
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

	public BossBar getBossBar() {
		return bossBar;
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
			if (player.getLocation().distance(honeyJarLocation.clone().add(0, honeyJarHeight / 2, 0)) <= depositRadius) {
				return true;
			}
		}
		return false;
	}

	public void updateHologram() {
		((TextLine) this.hologram.getLine(1)).setText(ChatColor.GOLD + "" + honey + " / " + NovaHive.getInstance().getGame().getConfig().getHoneyRequiredtoFillJar());
	}

	public void updateBossBarPlayers() {
		bossBar.removeAll();
		Bukkit.getServer().getOnlinePlayers().forEach(player -> {
			Team team = TeamManager.getTeamManager().getPlayerTeam(player);
			if (team != null) {
				if (team.equals(this.owner)) {
					bossBar.addPlayer(player);
				}
			}
		});
	}

	private String getBarText() {
		return ChatColor.GOLD + "" + honey + " / " + NovaHive.getInstance().getGame().getConfig().getHoneyRequiredtoFillJar() + " stashed honey";
	}

	public void update() {
		double toFill = (double) NovaHive.getInstance().getGame().getConfig().getHoneyRequiredtoFillJar();

		double progress = ((double) honey) / toFill;

		bossBar.setProgress(progress);
		bossBar.setTitle(getBarText());

		this.updateHologram();

		double perStep = toFill / (double) honeyJarHeight;
		int blocksFilled = (int) Math.ceil(honey / perStep);

		// Log.trace("HiveData", "honey: " + honey + " toFill: " + toFill + " jarHeight:
		// " + honeyJarHeight + " perStep: " + perStep + " blocksFilled: " +
		// blocksFilled);

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int i = 0; i < honeyJarHeight; i++) {
					Location location = honeyJarLocation.clone().add(x, i, z);
					boolean reached = (i + 1) <= blocksFilled;

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