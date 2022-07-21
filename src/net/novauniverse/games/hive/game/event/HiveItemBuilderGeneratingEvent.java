package net.novauniverse.games.hive.game.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.zeeraa.novacore.spigot.utils.ItemBuilder;

public class HiveItemBuilderGeneratingEvent extends Event {
	private static final HandlerList HANDLERS_LIST = new HandlerList();

	private ItemBuilder builder;
	private Player player;
	private HiveItemType type;

	public HiveItemBuilderGeneratingEvent(ItemBuilder builder, Player player, HiveItemType type) {
		this.builder = builder;
		this.player = player;
		this.type = type;
	}

	public ItemBuilder getBuilder() {
		return builder;
	}

	public Player getPlayer() {
		return player;
	}

	public HiveItemType getType() {
		return type;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS_LIST;
	}
}