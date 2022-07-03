package net.novauniverse.games.hive.game.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.novauniverse.games.hive.game.object.hive.HiveData;

public class HivePlayerDepositHoneyEvent extends Event {
	private static final HandlerList HANDLERS_LIST = new HandlerList();

	private HiveData hive;
	private Player player;
	private int amount;

	public HivePlayerDepositHoneyEvent(HiveData hive, Player player, int amount) {
		this.hive = hive;
		this.player = player;
		this.amount = amount;
	}

	public HiveData getHive() {
		return hive;
	}

	public Player getPlayer() {
		return player;
	}

	public int getAmount() {
		return amount;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS_LIST;
	}
}