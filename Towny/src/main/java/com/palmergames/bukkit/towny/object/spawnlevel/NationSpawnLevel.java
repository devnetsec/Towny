package com.palmergames.bukkit.towny.object.spawnlevel;

import com.palmergames.bukkit.towny.TownyUniverse;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;

public enum NationSpawnLevel {
	PART_OF_NATION(
			ConfigNodes.SPAWNING_ALLOW_NATION_SPAWN,
			"msg_err_nation_spawn_forbidden",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL,
			PermissionNodes.TOWNY_NATION_SPAWN_NATION.getNode(),
			ConfigNodes.SPAWNING_NATION_SPAWN_NATION_MEMBER_COOLDOWN_TIMER),
	UNAFFILIATED(
			ConfigNodes.SPAWNING_ALLOW_NATION_SPAWN_TRAVEL,
			"msg_err_public_nation_spawn_forbidden",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC,
			PermissionNodes.TOWNY_NATION_SPAWN_PUBLIC.getNode(),
			ConfigNodes.SPAWNING_NATION_SPAWN_NATION_UNAFFILIATED_COOLDOWN_TIMER),
	ADMIN(
			null,
			null,
			null,
			null,
			null);

	private final ConfigNodes isAllowingConfigNode;
	private final ConfigNodes ecoPriceConfigNode;
	private final String permissionNode;
	private final String notAllowedLangNode;
	private final int cooldown;

	NationSpawnLevel(ConfigNodes isAllowingConfigNode, String notAllowedLangNode, ConfigNodes ecoPriceConfigNode, String permissionNode, ConfigNodes cooldownConfigNode) {

		this.isAllowingConfigNode = isAllowingConfigNode;
		this.notAllowedLangNode = notAllowedLangNode;
		this.ecoPriceConfigNode = ecoPriceConfigNode;
		this.permissionNode = permissionNode;
		this.cooldown = cooldownConfigNode == null ? 0 : TownySettings.getInt(cooldownConfigNode);
	}

	private boolean isAllowed(Player player, Nation nation) {

		return this == NationSpawnLevel.ADMIN || (TownyUniverse.getInstance().getPermissionSource().testPermission(player, this.permissionNode)) && (isAllowedNation(nation));
	}
	
	private boolean isAllowedNation(Nation nation) {
		SpawnLevel level = TownySettings.getSpawnLevel(this.isAllowingConfigNode);
		return level == SpawnLevel.TRUE;
	}

	public double getCost() {

		return this == NationSpawnLevel.ADMIN ? 0 : TownySettings.getDouble(ecoPriceConfigNode);
	}

	public double getCost(Nation nation) {

		return this == NationSpawnLevel.ADMIN ? 0 : nation.getSpawnCost();
	}

	/**
	 * @return the cooldown
	 */
	public int getCooldown() {
		return cooldown;
	}
}
