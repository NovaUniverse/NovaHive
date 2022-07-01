package net.novauniverse.games.hive.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import net.novauniverse.games.hive.NovaHive;
import net.novauniverse.games.hive.game.config.ConfiguredHiveData;
import net.novauniverse.games.hive.game.config.HiveConfig;
import net.novauniverse.games.hive.game.object.flower.FlowerData;
import net.novauniverse.games.hive.game.object.hive.HiveData;
import net.novauniverse.games.hive.game.object.hive.HivePlayerData;
import net.novauniverse.games.hive.game.object.misc.PlayerFlowerDistanceComparator;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameEndReason;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.MapGame;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.elimination.PlayerQuitEliminationAction;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;
import net.zeeraa.novacore.spigot.teams.TeamManager;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;
import net.zeeraa.novacore.spigot.utils.PlayerUtils;
import xyz.xenondevs.particle.ParticleEffect;

public class Hive extends MapGame implements Listener {
	public static final int COLLECTOR_BOTTLE_SLOT = 0;
	public static final int COMPASS_SLOT = 1;
	public static final int HONEY_SLOT = 8;

	public static final int COLLECTION_TIME = 10; // 2 = 1 second

	private boolean started;
	private boolean ended;

	private HiveConfig config;

	private int timeLeft;

	private Task timer;
	private Task checkTask;

	private Task collectorTask;
	private Task particleTask;

	private List<HiveData> hives;
	private List<FlowerData> flowers;
	private List<HivePlayerData> playerData;

	public Hive() {
		super(NovaHive.getInstance());

		this.config = null;

		this.started = false;
		this.ended = false;

		this.hives = new ArrayList<HiveData>();
		this.flowers = new ArrayList<FlowerData>();
		this.playerData = new ArrayList<>();

		this.timeLeft = 0;
		this.timer = new SimpleTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				// Recovery ticks
				flowers.forEach(flower -> flower.recoveryTick());

				// Time left
				if (timeLeft > 0) {
					timeLeft--;
				} else if (!hasEnded()) {
					endGame(GameEndReason.TIME);
				}
			}
		}, 20L);

		this.checkTask = new SimpleTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				if (!ended) {
					if (players.size() == 0) {
						endGame(GameEndReason.ALL_FINISHED);
						return;
					}

					Bukkit.getServer().getOnlinePlayers().forEach(player -> {
						player.setFoodLevel(20);
						player.setSaturation(20);
					});

					// Update compass
					hives.forEach(hive -> {
						hive.getOwner().getMembers().forEach(uuid -> {
							Player player = Bukkit.getServer().getPlayer(uuid);
							if (player != null) {
								player.setCompassTarget(hive.getHoneyJarLocation());
							}
						});
					});
				}
			}
		}, 5L);

		this.collectorTask = new SimpleTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				playerData.forEach(playerData -> {
					if (playerData.getPlayer().getWorld() == getWorld()) {
						if (playerData.isCollecting()) {
							if (flowers.stream().filter(flower -> flower.canCollect(playerData.getPlayer())).count() > 0) {
								int timeLeft = playerData.decrementCollectionTimer();
								if (timeLeft <= 0) {
									FlowerData closest = flowers.stream().filter(flower -> flower.canCollect(playerData.getPlayer())).sorted(new PlayerFlowerDistanceComparator(playerData.getPlayer())).findFirst().orElse(null);
									if (closest != null) {
										closest.collect();
										addPlayerHoney(playerData.getPlayer(), 1);
										playerData.setCollecting(false);
										playerData.resetCollectionTime();
										VersionIndependentSound.ITEM_PICKUP.play(playerData.getPlayer());
									}
								}
							} else {
								playerData.setCollecting(false);
								playerData.resetCollectionTime();
								playerData.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "Failed to collect honey since you are not in range of any flowers ready to be collected");
							}
						}
					}
				});
			}
		}, 10L);

		this.particleTask = new SimpleTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				flowers.stream().filter(flower -> flower.canBeCollected()).forEach(flower -> {
					Location location = flower.getLocation().clone().add(0, 1, 0);
					ParticleEffect.REDSTONE.display(location, Color.YELLOW);
				});
			}
		}, 5L);
	}

	public HiveConfig getConfig() {
		return config;
	}

	@Override
	public String getName() {
		return "ng_hive";
	}

	@Override
	public String getDisplayName() {
		return "Hive";
	}

	@Override
	public PlayerQuitEliminationAction getPlayerQuitEliminationAction() {
		return PlayerQuitEliminationAction.DELAYED;
	}

	@Override
	public boolean eliminatePlayerOnDeath(Player player) {
		return false;
	}

	@Override
	public boolean isPVPEnabled() {
		return true;
	}

	@Override
	public boolean autoEndGame() {
		return false;
	}

	@Override
	public boolean hasStarted() {
		return started;
	}

	@Override
	public boolean hasEnded() {
		return ended;
	}

	@Override
	public boolean isFriendlyFireAllowed() {
		return false;
	}

	@Override
	public boolean canAttack(LivingEntity attacker, LivingEntity target) {
		return true;
	}

	public int getTimeLeft() {
		return timeLeft;
	}

	@Override
	public void onStart() {
		if (started) {
			return;
		}

		HiveConfig cfg = (HiveConfig) getActiveMap().getMapData().getMapModule(HiveConfig.class);
		if (cfg == null) {
			Log.fatal("Hive", "The map " + this.getActiveMap().getMapData().getMapName() + " has no Hive config map module");
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "Hive has run into an uncorrectable error and has to be ended");
			this.endGame(GameEndReason.ERROR);
			return;
		}
		this.config = cfg;

		List<ConfiguredHiveData> cfgHive = new ArrayList<>(config.getHives());
		TeamManager.getTeamManager().getTeams().forEach(team -> {
			if (cfgHive.size() > 0) {
				ConfiguredHiveData h = cfgHive.remove(0);
				HiveData hive = h.toHiveData(team, getWorld());
				hives.add(hive);
			} else {
				Log.warn("Hive", "Not enough hives configured for team " + team.getDisplayName());
			}
		});

		timeLeft = config.getGameTime();

		Bukkit.getServer().getOnlinePlayers().forEach(player -> {
			if (!hasPlayerData(player)) {
				playerData.add(new HivePlayerData(player));
			}
			spawnPlayer(player);
		});

		hives.forEach(hive -> hive.updateBossBarPlayers());

		Task.tryStartTask(timer);
		Task.tryStartTask(checkTask);
		Task.tryStartTask(collectorTask);
		Task.tryStartTask(particleTask);

		started = true;

		sendBeginEvent();
	}

	public void spawnPlayer(Player player) {
		if (!players.contains(player.getUniqueId())) {
			return;
		}

		HiveData playerHive = hives.stream().filter(hive -> hive.getOwner().getMembers().contains(player.getUniqueId())).findFirst().orElse(null);
		if (playerHive == null) {
			return;
		}

		player.teleport(playerHive.getSpawnLocation());

		player.setGameMode(GameMode.ADVENTURE);
		player.setAllowFlight(true);
		player.setFlying(true);

		player.setFoodLevel(20);
		player.setSaturation(20);

		PlayerUtils.resetMaxHealth(player);
		PlayerUtils.clearPlayerInventory(player);
		PlayerUtils.clearPotionEffects(player);
		PlayerUtils.resetPlayerXP(player);

		ItemBuilder collectorBuilder = new ItemBuilder(Material.GLASS_BOTTLE);
		collectorBuilder.setName(ChatColor.GREEN + "Collect honey");
		collectorBuilder.addLore(ChatColor.GREEN + "Get close to a flower");
		collectorBuilder.addLore(ChatColor.GREEN + "and right click to collect honey");
		collectorBuilder.setAmount(1);

		ItemBuilder compassBuilder = new ItemBuilder(Material.COMPASS);
		compassBuilder.setName(ChatColor.GREEN + "Hive tracker");
		compassBuilder.addLore(ChatColor.GREEN + "Use this to find");
		compassBuilder.addLore(ChatColor.GREEN + "your hive");
		compassBuilder.setAmount(1);

		player.getInventory().setItem(Hive.COLLECTOR_BOTTLE_SLOT, collectorBuilder.build());
		player.getInventory().setItem(Hive.COMPASS_SLOT, compassBuilder.build());

		this.setPlayerHoney(player, 0);
	}

	@Override
	public void onEnd(GameEndReason reason) {
		if (ended) {
			return;
		}

		Task.tryStopTask(timer);
		Task.tryStopTask(checkTask);
		Task.tryStopTask(collectorTask);
		Task.tryStopTask(particleTask);

		ended = true;
	}

	public void addPlayerHoney(Player player, int amount) {
		int honey = this.getPlayerHoney(player);
		this.setPlayerHoney(player, honey + amount);
	}

	public int getPlayerHoney(Player player) {
		ItemStack item = player.getInventory().getItem(HONEY_SLOT);
		if (item != null) {
			if (item.getType() == Material.HONEY_BOTTLE) {
				return item.getAmount();
			}
		}
		return 0;
	}

	public void setPlayerHoney(Player player, int amount) {
		if (amount < 0) {
			amount = 0;
		}

		if (amount == 0) {
			ItemBuilder builder = new ItemBuilder(Material.GLASS_BOTTLE);
			builder.setName(ChatColor.RED + "0 Honey");
			builder.addLore(ChatColor.RED + "Cant deposit");
			player.getInventory().setItem(Hive.HONEY_SLOT, builder.build());
		} else {
			int max = getConfig().getMaxHoneyInInventory();
			if (amount > max) {
				amount = max;
			}

			ItemBuilder builder = new ItemBuilder(Material.HONEY_BOTTLE);
			builder.setName(ChatColor.GOLD + "" + amount + " Honey");
			builder.addLore(ChatColor.GOLD + "Go to your hive and");
			builder.addLore(ChatColor.GOLD + "right click to deposit");
			player.getInventory().setItem(Hive.HONEY_SLOT, builder.build());
		}
	}

	public HivePlayerData getPlayerData(Player player) {
		return playerData.stream().filter(pd -> pd.getPlayer() == player).findFirst().get();
	}

	private boolean hasPlayerData(Player player) {
		return playerData.stream().filter(pd -> pd.getPlayer() == player).count() > 0;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent e) {
		playerData.add(new HivePlayerData(e.getPlayer()));

		hives.forEach(hive -> hive.updateBossBarPlayers());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent e) {
		playerData.removeIf(pd -> pd.getPlayer() == e.getPlayer());
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryInteract(InventoryInteractEvent e) {
		if (started && !ended) {
			if (e.getWhoClicked() instanceof Player) {
				Player player = (Player) e.getWhoClicked();

				if (player.getGameMode() == GameMode.CREATIVE) {
					// Ignore creative for testing
					return;
				}

				if (players.contains(player.getUniqueId())) {
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Player player = e.getPlayer();
				if (getPlayers().contains(player.getUniqueId())) {
					if (e.getItem().getType() == Material.HONEY_BOTTLE) {
						HiveData hive = hives.stream().filter(h -> h.canDeposit(player)).findFirst().orElse(null);
						if (hive == null) {
							player.sendMessage(ChatColor.RED + "You need to enter your hive before you can deposit honey. Use your compass to find your way back home");
						} else {
							int amount = this.getPlayerHoney(player);
							if (amount > 0) {
								this.setPlayerHoney(player, 0);

								hive.getOwner().sendMessage(org.bukkit.ChatColor.GREEN + player.getName() + " deposited " + amount + " bottle" + (amount == 1 ? "" : "s") + " of honey");

								hive.addHoney(amount);
								hive.update();
							}
						}
					} else if (e.getItem().getType() == Material.GLASS_BOTTLE) {
						HivePlayerData playerData = getPlayerData(player);
						if (!playerData.isCollecting()) {
							if (flowers.stream().filter(flower -> flower.canCollect(player)).count() > 0) {
								playerData.resetCollectionTime();
								playerData.setCollecting(true);
							}
						}
					}
				}
			}
		}
	}
}