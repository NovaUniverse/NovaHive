package net.novauniverse.games.hive.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import net.novauniverse.games.hive.NovaHive;
import net.novauniverse.games.hive.game.config.ConfiguredHiveData;
import net.novauniverse.games.hive.game.config.HiveConfig;
import net.novauniverse.games.hive.game.event.HiveInitialCountdownEvent;
import net.novauniverse.games.hive.game.event.HiveItemBuilderGeneratingEvent;
import net.novauniverse.games.hive.game.event.HiveItemType;
import net.novauniverse.games.hive.game.event.HivePlayerDepositHoneyEvent;
import net.novauniverse.games.hive.game.event.HiveTeamCompletedEvent;
import net.novauniverse.games.hive.game.object.flower.FlowerData;
import net.novauniverse.games.hive.game.object.hive.HiveData;
import net.novauniverse.games.hive.game.object.hive.HivePlayerData;
import net.novauniverse.games.hive.game.object.misc.PlayerFlowerDistanceComparator;
import net.novauniverse.games.hive.game.object.misc.RespawnTimer;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.commons.utils.TextUtils;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependentUtils;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentMaterial;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameEndReason;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.MapGame;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.elimination.PlayerQuitEliminationAction;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;
import net.zeeraa.novacore.spigot.teams.Team;
import net.zeeraa.novacore.spigot.teams.TeamManager;
import net.zeeraa.novacore.spigot.utils.ChatColorRGBMapper;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;
import net.zeeraa.novacore.spigot.utils.PlayerUtils;
import net.zeeraa.novacore.spigot.utils.RandomFireworkEffect;
import xyz.xenondevs.particle.ParticleEffect;

public class Hive extends MapGame implements Listener {
	// Inventory
	public static final int WEAPON_SLOT = 0;
	public static final int COLLECTOR_BOTTLE_SLOT = 1;
	public static final int COMPASS_SLOT = 2;
	public static final int HONEY_SLOT = 8;

	public static final int COLLECTION_RADIUS = 6;

	public static final int COLLECTION_TIME = 10; // 2 = 1 second

	public static final int RESPAWN_TIMER_VALUE = 10; // 10 seconds
	public static final int START_TIMER_VALUE = 10; // 10 seconds

	private boolean started;
	private boolean ended;

	private HiveConfig config;

	private int timeLeft;

	private int placementCounter;

	private Task timer;
	private Task checkTask;
	private Task collectorTask;
	private Task particleTask;
	private Task regenTask;
	private Task startTimer;

	private List<HiveData> hives;
	private List<FlowerData> flowers;
	private List<HivePlayerData> playerData;
	private List<RespawnTimer> respawnTimers;

	private int startTime;
	private boolean startTimerFinished;

	public Hive() {
		super(NovaHive.getInstance());

		this.config = null;

		this.started = false;
		this.ended = false;

		this.hives = new ArrayList<HiveData>();
		this.flowers = new ArrayList<FlowerData>();
		this.playerData = new ArrayList<HivePlayerData>();
		this.respawnTimers = new ArrayList<RespawnTimer>();

		this.placementCounter = 1;

		this.startTime = START_TIMER_VALUE;
		this.startTimerFinished = false;

		this.startTimer = new SimpleTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				if (startTime > 0) {
					VersionIndependentSound.NOTE_PLING.broadcast(1.0F, 1.0F);
					Bukkit.getServer().getOnlinePlayers().forEach(player -> VersionIndependentUtils.get().sendTitle(player, ChatColor.GREEN + "Starting in " + startTime, "", 0, 25, 0));
					Event event = new HiveInitialCountdownEvent(startTime);
					Bukkit.getServer().getPluginManager().callEvent(event);
					startTime--;
				} else {
					Task.tryStopTask(startTimer);
					startTimerFinished = true;
					VersionIndependentSound.NOTE_PLING.broadcast(1.0F, 1.25F);
					Bukkit.getServer().getOnlinePlayers().forEach(player -> VersionIndependentUtils.get().sendTitle(player, ChatColor.GREEN + "GO", "", 0, 20, 10));
					sendBeginEvent();
				}
			}
		}, 20L);

		this.timeLeft = 0;
		this.timer = new SimpleTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				// Recovery ticks
				flowers.forEach(flower -> flower.recoveryTick());

				// Time left
				if (timeLeft > 0) {
					if (timeLeft == 30 || timeLeft == 60) {
						Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + timeLeft + " seconds left " + TextUtils.ICON_WARNING);
						Bukkit.getServer().getOnlinePlayers().forEach(player -> {
							VersionIndependentSound.NOTE_PLING.play(player);
							VersionIndependentUtils.get().sendTitle(player, "", ChatColor.RED + TextUtils.ICON_WARNING + " " + timeLeft + " seconds left " + TextUtils.ICON_WARNING, 0, 40, 10);
						});
					}

					if (timeLeft <= 10) {
						Bukkit.getServer().getOnlinePlayers().forEach(player -> {
							VersionIndependentSound.NOTE_PLING.play(player);
							VersionIndependentUtils.get().sendTitle(player, "", ChatColor.RED + TextUtils.ICON_WARNING + " " + timeLeft + " second" + (timeLeft == 1 ? "" : "s") + " left " + TextUtils.ICON_WARNING, 0, 20, 10);
						});
					}

					if (startTimerFinished) {
						timeLeft--;
					}
				} else if (!hasEnded()) {
					endGame(GameEndReason.TIME);
					return;
				}

				// Respawn timers
				try {
					respawnTimers.stream().filter(d -> d.isDone()).forEach(d -> spawnPlayer(d.getPlayer()));
				} catch (Exception e) {
					Log.error("Hive", "Failed to respawn a player. " + e.getClass().getName() + " " + e.getMessage());
					e.printStackTrace();
				}
				respawnTimers.removeIf(d -> d.isDone());
				respawnTimers.stream().filter(d -> d.shouldDecrement()).forEach(d -> d.decrement());
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
							if (playerData.getPlayer().isFlying()) {
								playerData.setCollecting(false);
								playerData.resetCollectionTime();
								playerData.getPlayer().sendMessage(ChatColor.RED + "You cant fly while collecting honey");
							} else {
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
									playerData.getPlayer().sendMessage(ChatColor.RED + "You are not in range of a flower with pollen");
								}
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

		this.regenTask = new SimpleTask(getPlugin(), new Runnable() {
			@Override
			public void run() {
				hives.forEach(hive -> {
					hive.getOwner().getOnlinePlayers().forEach(player -> {
						if (player.getWorld() == getWorld()) {
							if (player.getLocation().distance(hive.getHoneyJarLocation()) <= hive.getDepositRadius()) {
								player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false, false));
							}
						}
					});
				});
			}
		}, 10L);
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

	public List<HiveData> getHives() {
		return hives;
	}

	@Override
	public void tpToSpectator(Player player) {
		PlayerUtils.clearPotionEffects(player);
		PlayerUtils.resetMaxHealth(player);
		player.setGameMode(GameMode.SPECTATOR);
		player.teleport(getActiveMap().getSpectatorLocation(), TeleportCause.PLUGIN);
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
			if (players.contains(player.getUniqueId())) {
				if (!hasPlayerData(player)) {
					playerData.add(new HivePlayerData(player));
				}
				spawnPlayer(player);
			} else {
				tpToSpectator(player);
			}
		});

		hives.forEach(hive -> hive.updateBossBarPlayers());

		config.getFlowers().forEach(loc -> this.flowers.add(new FlowerData(loc.toBukkitLocation(getWorld()), COLLECTION_RADIUS)));

		Task.tryStartTask(timer);
		Task.tryStartTask(checkTask);
		Task.tryStartTask(collectorTask);
		Task.tryStartTask(particleTask);
		Task.tryStartTask(regenTask);

		Bukkit.getServer().getWorlds().forEach(world -> {
			world.setGameRule(GameRule.KEEP_INVENTORY, true);
			world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
		});

		started = true;

		Task.tryStartTask(startTimer);

		Bukkit.getServer().getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).forEach(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (START_TIMER_VALUE * 20) + 40, 0)));
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
		compassBuilder.addLore(ChatColor.GREEN + "Use this to find your hive");
		compassBuilder.setAmount(1);

		ItemBuilder weaponBuilder = new ItemBuilder(VersionIndependentMaterial.WOODEN_SWORD);
		weaponBuilder.setName("Stinger");
		weaponBuilder.setAmount(1);
		weaponBuilder.setUnbreakable(true);

		ItemBuilder chestplateBuilder = new ItemBuilder(Material.LEATHER_CHESTPLATE);
		chestplateBuilder.setAmount(1);
		chestplateBuilder.setLeatherArmorColor(ChatColorRGBMapper.chatColorToRGBColorData(playerHive.getOwner().getTeamColor()).toBukkitColor());
		chestplateBuilder.setUnbreakable(true);

		Bukkit.getServer().getPluginManager().callEvent(new HiveItemBuilderGeneratingEvent(collectorBuilder, player, HiveItemType.COLLECTOR_ITEM));
		Bukkit.getServer().getPluginManager().callEvent(new HiveItemBuilderGeneratingEvent(compassBuilder, player, HiveItemType.COMPASS));
		Bukkit.getServer().getPluginManager().callEvent(new HiveItemBuilderGeneratingEvent(weaponBuilder, player, HiveItemType.WEAPON));
		Bukkit.getServer().getPluginManager().callEvent(new HiveItemBuilderGeneratingEvent(chestplateBuilder, player, HiveItemType.CHESTPLATE));

		player.getInventory().setItem(Hive.WEAPON_SLOT, weaponBuilder.build());
		player.getInventory().setItem(Hive.COLLECTOR_BOTTLE_SLOT, collectorBuilder.build());
		player.getInventory().setItem(Hive.COMPASS_SLOT, compassBuilder.build());

		player.getInventory().setChestplate(chestplateBuilder.build());

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
		Task.tryStopTask(regenTask);

		switch (reason) {
		case ALL_FINISHED:
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Game Over> No remaining players");
			break;

		case TIME:
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Game Over> Time is up");
			break;

		case OPERATOR_ENDED_GAME:
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Game Over> An admin has ended the game");
			break;

		default:
			break;
		}

		VersionIndependentSound.WITHER_DEATH.broadcast(0.5F, 1.0F);

		hives.forEach(hive -> {
			Location location = hive.getSpawnLocation();
			Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
			FireworkMeta fwm = fw.getFireworkMeta();

			fwm.setPower(0);
			fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());

			if (random.nextBoolean()) {
				fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());
			}

			fw.setFireworkMeta(fwm);
			fw.detonate();
		});

		Bukkit.getServer().getOnlinePlayers().forEach(player -> {
			VersionIndependentUtils.get().resetEntityMaxHealth(player);
			player.setFoodLevel(20);
			PlayerUtils.clearPlayerInventory(player);
			PlayerUtils.resetPlayerXP(player);
			player.setGameMode(GameMode.SPECTATOR);
		});

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
			/*
			 * ItemBuilder builder = new ItemBuilder(Material.GLASS_BOTTLE);
			 * builder.setName(ChatColor.RED + "0 Honey"); builder.addLore(ChatColor.RED +
			 * "Cant deposit"); player.getInventory().setItem(Hive.HONEY_SLOT,
			 * builder.build());
			 */
			ItemBuilder noHoney = new ItemBuilder(Material.BARRIER);

			noHoney.setAmount(1);
			noHoney.setName(ChatColor.RED + "No honey");

			Bukkit.getServer().getPluginManager().callEvent(new HiveItemBuilderGeneratingEvent(noHoney, player, HiveItemType.NO_HONEY_ICON));

			player.getInventory().setItem(HONEY_SLOT, noHoney.build());
		} else {
			int max = getConfig().getMaxHoneyInInventory();
			if (amount > max) {
				amount = max;
			}

			ItemBuilder builder = new ItemBuilder(Material.HONEY_BOTTLE);
			builder.setAmount(amount);
			builder.setName(ChatColor.GOLD + "" + amount + " Honey");
			builder.addLore(ChatColor.GOLD + "Go to your hive and");
			builder.addLore(ChatColor.GOLD + "right click to deposit");

			Bukkit.getServer().getPluginManager().callEvent(new HiveItemBuilderGeneratingEvent(builder, player, HiveItemType.HONEY_BOTTLE));

			player.getInventory().setItem(Hive.HONEY_SLOT, builder.build());
		}
	}

	public HivePlayerData getPlayerData(Player player) {
		return playerData.stream().filter(pd -> pd.getPlayer() == player).findFirst().get();
	}

	private boolean hasPlayerData(Player player) {
		return playerData.stream().filter(pd -> pd.getPlayer() == player).count() > 0;
	}

	/* ===== Events ===== */

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			HivePlayerData playerData = getPlayerData(player);
			if (playerData != null) {
				if (playerData.isCollecting()) {
					player.sendMessage(ChatColor.RED + "Collecting canceled since you took damage");
					playerData.setCollecting(false);
					playerData.resetCollectionTime();
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityRegen(EntityRegainHealthEvent e) {
		if (e.getEntity() instanceof Player) {
			if (e.getRegainReason() == RegainReason.REGEN || e.getRegainReason() == RegainReason.SATIATED) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();

		if (players.contains(player.getUniqueId())) {
			playerData.add(new HivePlayerData(player));

			hives.forEach(hive -> hive.updateBossBarPlayers());

			if (hasStarted()) {
				new BukkitRunnable() {
					@Override
					public void run() {
						player.setAllowFlight(true);
						player.setFlying(true);
					}
				}.runTaskLater(getPlugin(), 3L);
			}
		} else {
			if (hasStarted()) {
				tpToSpectator(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent e) {
		playerData.removeIf(pd -> pd.getPlayer() == e.getPlayer());
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		if (started) {
			if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		e.getPlayer().setGameMode(GameMode.SPECTATOR);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();
		int honey = getPlayerHoney(player);

		setPlayerHoney(player, 0);

		e.setKeepInventory(true);
		VersionIndependentSound.WITHER_HURT.play(player, 0.5F, 1.0F);
		player.sendMessage(ChatColor.RED + "You died. Respawning in " + Hive.RESPAWN_TIMER_VALUE + " seconds");
		player.setGameMode(GameMode.SPECTATOR);

		RespawnTimer respawnTimer = new RespawnTimer(player.getUniqueId());
		respawnTimers.add(respawnTimer);
		respawnTimer.showTitle();

		Log.trace("Hive", player.getName() + " died with " + honey + " honey");
		Player killer = player.getKiller();
		if (killer != null) {
			int killerHoney = getPlayerHoney(killer);
			int toAdd = honey;
			if (toAdd > 0) {
				if (killerHoney + honey > config.getMaxHoneyInInventory()) {
					toAdd = config.getMaxHoneyInInventory() - killerHoney;
				}
				addPlayerHoney(killer, toAdd);
				killer.sendMessage(ChatColor.GREEN + "Stole " + toAdd + " bottle" + (toAdd == 1 ? "" : "s") + " of honey from " + player.getName());
			}

			net.md_5.bungee.api.ChatColor playerColor = net.md_5.bungee.api.ChatColor.AQUA;
			net.md_5.bungee.api.ChatColor killerColor = net.md_5.bungee.api.ChatColor.AQUA;

			Team playerTeam = TeamManager.getTeamManager().getPlayerTeam(player);
			Team killerTeam = TeamManager.getTeamManager().getPlayerTeam(killer);

			if (playerTeam != null) {
				playerColor = playerTeam.getTeamColor();
			}

			if (killerTeam != null) {
				killerColor = killerTeam.getTeamColor();
			}

			Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Player Killed> " + playerColor + ChatColor.BOLD + player.getName() + ChatColor.RED + ChatColor.BOLD + " was killed by " + killerColor + ChatColor.BOLD + killer.getName());
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerDropItem(EntityPickupItemEvent e) {
		if (e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();

			if (started) {
				if (player.getGameMode() != GameMode.CREATIVE) {
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent e) {
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

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
			if (started && !ended && !startTimerFinished) {
				Location to = e.getFrom().clone();

				to.setYaw(e.getTo().getYaw());
				to.setPitch(e.getTo().getPitch());

				e.setTo(to);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Player player = e.getPlayer();
				if (getPlayers().contains(player.getUniqueId())) {
					if (e.getItem() != null) {
						if (e.getItem().getType() == Material.HONEY_BOTTLE) {
							HiveData hive = hives.stream().filter(h -> h.canDeposit(player)).findFirst().orElse(null);
							if (hive == null) {
								player.sendMessage(ChatColor.RED + "You need to enter your hive before you can deposit honey. You can use your compass to find your way back home");
							} else {
								int amount = this.getPlayerHoney(player);
								if (amount > 0) {
									this.setPlayerHoney(player, 0);

									hive.getOwner().sendMessage(ChatColor.GREEN + player.getName() + " deposited " + amount + " bottle" + (amount == 1 ? "" : "s") + " of honey");

									hive.addHoney(amount);
									hive.update();

									Event depositEvent = new HivePlayerDepositHoneyEvent(hive, player, amount);
									Bukkit.getServer().getPluginManager().callEvent(depositEvent);

									if (hive.getHoney() >= config.getHoneyRequiredtoFillJar()) {
										Event completeEvent = new HiveTeamCompletedEvent(hive.getOwner(), placementCounter);
										Bukkit.getServer().getPluginManager().callEvent(completeEvent);

										Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Completed> " + hive.getOwner().getTeamColor() + ChatColor.BOLD + hive.getOwner().getDisplayName() + ChatColor.GREEN + ChatColor.BOLD + " filled their hive. " + TextUtils.ordinal(placementCounter) + " place");

										hive.getOwner().getOnlinePlayers().forEach(p -> {
											p.setGameMode(GameMode.SPECTATOR);
											VersionIndependentSound.LEVEL_UP.play(p);
											p.sendTitle(ChatColor.GREEN + TextUtils.ordinal(placementCounter) + " place", "", 10, 40, 10);
											players.remove(p.getUniqueId());
										});

										placementCounter++;
									}
								}
							}
						} else if (e.getItem().getType() == Material.GLASS_BOTTLE) {
							HivePlayerData playerData = getPlayerData(player);
							if (!playerData.isCollecting()) {
								if (getPlayerHoney(player) >= config.getMaxHoneyInInventory()) {
									player.sendMessage(ChatColor.RED + "You cant hold any more honey in your inventory. Find your hive and deposit the honey before you can collect more");
								} else {
									if (flowers.stream().filter(flower -> flower.canCollect(player)).count() > 0) {
										if (!player.isFlying()) {
											playerData.resetCollectionTime();
											playerData.setCollecting(true);
										} else {
											player.sendMessage(ChatColor.RED + "You need to land before collecting honey");
										}

									} else {
										player.sendMessage(ChatColor.RED + "You are not in range of a flower with pollen");
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
		if (e.getItem().getType() == Material.HONEY_BOTTLE) {
			e.setCancelled(true);
		}
	}
}