package com.palmergames.bukkit.towny.utils;

import java.util.List;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeTools;

public class TownUtil {
	
	private TownUtil() {
		throw new IllegalStateException("Utility Class");
	}

	/**
	 * Makes a list of {@linkplain Resident}s who haven't logged in in the given days.
	 * NPCs and Mayors are not included.
	 * 
	 * @param resList List of Residents from which to test for inactivity.
	 * @param days Number of days after which players are considered inactive.
	 * @since 0.97.0.7
	 */
	public static List<Resident> gatherInactiveResidents(List<Resident> resList, int days) {
		return resList.stream()
				.filter(res -> !res.isNPC() && !res.isMayor() && !BukkitTools.isOnline(res.getName()) && (System.currentTimeMillis() - res.getLastOnline() > TimeTools.getMillis(days + "d")))
				.collect(Collectors.toList());
	}

	public static boolean townCanHaveThisAmountOfResidents(Town town, int residentCount, boolean isCapital) {
		int maxResidents = !isCapital
				? !town.hasNation() ? getMaxAllowedNumberOfResidentsWithoutNation(town) : TownySettings.getMaxResidentsPerTown()
				: TownySettings.getMaxResidentsPerTownCapitalOverride();

		return maxResidents == 0 || residentCount <= maxResidents;
	}

	public static int getMaxAllowedNumberOfResidentsWithoutNation(Town town) {
		int maxResidents = TownySettings.getMaxNumResidentsWithoutNation() > 0 ? TownySettings.getMaxNumResidentsWithoutNation() : TownySettings.getMaxResidentsPerTown();
		return maxResidents == 0 ? Integer.MAX_VALUE : maxResidents;
	}
}
