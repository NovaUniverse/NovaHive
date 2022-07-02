package net.novauniverse.games.hive.game.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.zeeraa.novacore.spigot.teams.Team;

public class HiveTeamCompletedEvent extends Event {
	private static final HandlerList HANDLERS_LIST = new HandlerList();

	private Team team;
	private int placement;

	public HiveTeamCompletedEvent(Team team, int placement) {
		this.team = team;
		this.placement = placement;
	}

	public Team getTeam() {
		return team;
	}

	public int getPlacement() {
		return placement;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS_LIST;
	}
}