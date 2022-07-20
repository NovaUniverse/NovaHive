package net.novauniverse.games.hive.game.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class HiveInitialCountdownEvent extends Event {
	private static final HandlerList HANDLERS_LIST = new HandlerList();

	private int timeLeft;

	public HiveInitialCountdownEvent(int timeLeft) {
		this.timeLeft = timeLeft;
	}

	public int getTimeLeft() {
		return timeLeft;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS_LIST;
	}
}