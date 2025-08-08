package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.damage.TownBlockPVPTestEvent;
import com.palmergames.bukkit.towny.event.damage.TownyDispenserDamageEntityEvent;
import com.palmergames.bukkit.towny.event.damage.TownyFriendlyFireTestEvent;
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.event.damage.WildernessPVPTestEvent;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.util.BukkitTools;

import com.palmergames.bukkit.util.EntityLists;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * 
 * @author ElgarL,Shade,LlmDl
 * 
 */
public class CombatUtil {

	/**
	 * Tests the attacker against defender to see if we need to cancel
	 * the damage event due to world PvP, Plot PvP or Friendly Fire settings.
	 * Only allow a Wolves owner to cause it damage, and residents with destroy
	 * permissions to damage passive animals and villagers while in a town.
	 * 
	 * @param attacker - Entity attacking the Defender
	 * @param defender - Entity defending from the Attacker
	 * @param cause - The DamageCause behind this DamageCall.
	 * @return true if we should cancel.
	 */
	public static boolean preventDamageCall(Entity attacker, Entity defender, DamageCause cause) {

		TownyWorld world = TownyAPI.getInstance().getTownyWorld(defender.getWorld());

		// World using Towny
		if (world == null || !world.isUsingTowny())
			return false;

		Player a = null;
		Player b = null;
		
		Entity directSource = attacker;

		/*
		 * Find the shooter if this is a projectile.
		 */
		if (attacker instanceof Projectile projectile) {
			
			final ProjectileSource source = projectile.getShooter();
			
			if (source instanceof Entity entity)
				directSource = entity;
			else if (source instanceof BlockProjectileSource blockProjectileSource) {
				if (CombatUtil.preventDispenserDamage(blockProjectileSource.getBlock(), defender, cause))
					return true;
			}
		} else if (attacker instanceof LightningStrike lightning) {
			final Entity causingEntity = getLightningCausingEntity(lightning);
			if (causingEntity != null)
				directSource = causingEntity;
		}

		if (directSource instanceof Player player)
			a = player;
		if (defender instanceof Player player)
			b = player;

		// Allow players to injure themselves
		if (a == b && a != null && b != null)
			return false;

		return preventDamageCall(world, attacker, defender, a, b, cause);
	}

	/**
	 * Tests the attacker against defender to see if we need to cancel
	 * the damage event due to world PvP, Plot PvP or Friendly Fire settings.
	 * Only allow a Wolves owner to cause it damage, and residents with destroy
	 * permissions to damage passive animals and villagers while in a town.
	 * 
	 * @param world - World in which DamageCall was issued
	 * @param attackingEntity - Entity attacking
	 * @param defendingEntity - Entity defending
	 * @param attackingPlayer - Player attacking
	 * @param defendingPlayer - Player defending
	 * @param cause - The DamageCause behind this DamageCall.
	 * @return true if we should cancel.
	 */
	private static boolean preventDamageCall(TownyWorld world, Entity attackingEntity, Entity defendingEntity, Player attackingPlayer, Player defendingPlayer, DamageCause cause) {

		Projectile projectileAttacker = null;
		if (attackingEntity instanceof Projectile projectile) {
			projectileAttacker = projectile;
			
			if (projectile.getShooter() instanceof Entity entity)
				attackingEntity = entity;
		}
		
		TownBlock defenderTB = TownyAPI.getInstance().getTownBlock(defendingEntity.getLocation());
		TownBlock attackerTB = TownyAPI.getInstance().getTownBlock(attackingEntity.getLocation());
		/*
		 * We have an attacking player, which is not an NPC.
		 */
		if (attackingPlayer != null && isNotNPC(attackingPlayer)) {

			/*
			 * Defender is a player, which is not an NPC..
			 */
			if (defendingPlayer != null && isNotNPC(defendingPlayer)) {

				boolean cancelled = false;

				/*
				 * Player is not considered an Admin by Towny.
				 * Admins can some times bypass pvp settings.
				 */
				if (!isTownyAdminBypassingPVP(attackingPlayer)) {
					
					// TODO: Allow duels if both players/teams agree to it
					cancelled = true;
				}

				/*
				 * A player has attempted to damage a player. Throw a TownPlayerDamagePlayerEvent.
				 */
				TownyPlayerDamagePlayerEvent event = new TownyPlayerDamagePlayerEvent(defendingPlayer.getLocation(), defendingPlayer, cause, defenderTB, cancelled, attackingPlayer);

				// A cancelled event should contain a message.
				if (BukkitTools.isEventCancelled(event) && event.getMessage() != null)
					TownyMessaging.sendErrorMsg(attackingPlayer, event.getMessage());
				
				return event.isCancelled();

			/*
			 * Defender is not a player.
			 */
			} else {
				/*
				 * First test protections for Non-Player defenders who are being protected
				 * because they are specifically in Town-Claimed land.
				 */
				if (defenderTB != null) {
					
					/*
					 * Protect tamed dogs in town land which are not owned by the attacking player,
					 * unless they are angry and attacking the player.
					 */
					if (defendingEntity instanceof Wolf wolf) {
						if (!isOwner(wolf, attackingPlayer) && !isTargetingPlayer(wolf, attackingPlayer)) {
							if (EntityTypeUtil.isProtectedEntity(defendingEntity))
								return !(defenderTB.getPermissions().pvp || TownyActionEventExecutor.canDestroy(attackingPlayer, wolf.getLocation(), Material.STONE));
						} else
							// The player doesn't own the wolf, and the wolf is actively angry and targeting the player.
							// Allow the combat.
							return false;
					}
					
					/*
					 * Farm Animals - based on whether this is allowed using the PlayerCache and then a cancellable event.
					 */
					if (defenderTB.getType() == TownBlockType.FARM && TownySettings.getFarmAnimals().contains(defendingEntity.getType().toString()))
						return !TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), Material.WHEAT);

					/*
					 * Config's protected entities: Animals,WaterMob,NPC,Snowman,ArmorStand,Villager
					 */
					if (EntityTypeUtil.isProtectedEntity(defendingEntity))
						return !TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), Material.DIRT);
				}

				/*
				 * Protect specific entity interactions (faked with Materials).
				 * Requires destroy permissions in either the Wilderness or in Town-Claimed land.
				 */
				if (EntityLists.DESTROY_PROTECTED.contains(defendingEntity)) {
					Material material = EntityTypeUtil.parseEntityToMaterial(defendingEntity.getType());

					if (material != null) {
						//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
						return !TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), material);
					}
				}
			}

		/*
		 * This is not an attack by a player....
		 */
		} else {

			/*
			 * If Defender is a player, Attacker is not.
			 */
			if (defendingPlayer != null) {

				/*
				 * If attackingEntity is a tamed Wolf...
				 * Prevent pvp and remove Wolf targeting.
				 */
				if (attackingEntity instanceof Wolf wolf) {
					wolf.setAngry(false);
					return true;
				}
				
				if (attackingEntity instanceof LightningStrike 
					&& world.hasTridentStrike(attackingEntity.getUniqueId())) {
					return true;
				}
				
			/*
			 * DefendingEntity is not a player.
			 * This is now non-player vs non-player damage.
			 */
			} else {
				
				/*
				 * The defending non-player is in the wilderness, do not prevent this combat.
				 */
				if (defenderTB == null)
					return false;

				/*
				 * The config is set up so that non-players (mobs) are allowed to hurt the normally-protected entity types.
				 */
				if (!TownySettings.areProtectedEntitiesProtectedAgainstMobs())
					return false;

			    /*
			     * Prevents projectiles fired by non-players harming non-player entities.
			     * Could be a monster or it could be a dispenser.
			     */
				if (projectileAttacker != null && EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), defendingEntity)) {
					return true;
				}

				/*
				* Allow wolves to attack unprotected entites (such as skeletons), but not protected ones.
				*/
				if (attackingEntity instanceof Wolf wolf && EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), defendingEntity)) {
					if (isATamedWolfWithAOnlinePlayer(wolf)) {
						Player owner = BukkitTools.getPlayerExact(wolf.getOwner().getName());
						return !PlayerCacheUtil.getCachePermission(owner, defendingEntity.getLocation(), Material.AIR, ActionType.DESTROY);
					} else {
						wolf.setAngry(false);
						return true;
					}
				}
				
				if (attackingEntity.getType().getKey().equals(NamespacedKey.minecraft("axolotl")) && EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), defendingEntity)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return true if the outlaw system allows for outlaws to harm/be harmed.
	 * 
	 * @param defenderTB TownBlock where the defendingPlayer is harmed.
	 * @param attackingPlayer Player harming the defendingPlayer.
	 * @param defendingPlayer Player getting harmed.
	 * @return true if one of the players is an outlaw in a situation where that matters.
	 */
	private static boolean isOutlawInTown(TownBlock defenderTB, Player attackingPlayer, Player defendingPlayer) {
		if (defenderTB == null)
			return false;
		
		Town town = defenderTB.getTownOrNull();
		if (town == null)
			return false;
		if (TownySettings.forcePVPForTownOutlaws() && town.hasOutlaw(defendingPlayer.getName()))
			return true;
		if (TownySettings.outlawsAlwaysAllowedToPVP() && town.hasOutlaw(attackingPlayer.getName()))
			return true;
		return false;
	}

	/**
	 * Is town b in a nation with town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are in the same nation.
	 */
	public static boolean isSameNation(Town a, Town b) {

		if (isSameTown(a, b))
			return true;
		if (a.hasNation() && b.hasNation() && a.getNationOrNull().hasTown(b))
			return true;
		return false;
	}

	/**
	 * Is town b the same town as town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are allies.
	 */
	public static boolean isSameTown(Town a, Town b) {

		if (a == null || b == null)
			return false;

		return a.getUUID().equals(b.getUUID());
	}

	/**
	 * Is resident a in a nation with resident b?
	 * 
	 * @param a - Resident A in comparison.
	 * @param b - Resident B in comparison.
	 * @return true if they are in the same nation.
	 */
	public static boolean isSameNation(Resident a, Resident b) {
		if (!a.hasTown() || !b.hasTown())
			return false;
				
		return isSameNation(a.getTownOrNull(), b.getTownOrNull());
	}
	
	
	/**
	 * Is resident a in a town with resident b?
	 * @param a - Resident A in comparison.
	 * @param b - Resident B in comparison.
	 * @return true if they are in the same town.
	 */
	public static boolean isSameTown(Resident a, Resident b) {
		if (!a.hasTown() || !b.hasTown())
			return false;

		return isSameTown(a.getTownOrNull(), b.getTownOrNull());
	}
	
	/**
	 * Test if all the listed residents are friends
	 * 
	 * @param possibleFriends - List of Residents (List&lt;Resident&gt;)
	 * @return true if they are all friends
	 */
	public static boolean areAllFriends(List<Resident> possibleFriends) {

		if (possibleFriends.size() <= 1)
			return true;
		else {
			for (int i = 0; i < possibleFriends.size() - 1; i++)
				if (!possibleFriends.get(i).hasFriend(possibleFriends.get(i + 1)))
					return false;
			return true;
		}
	}
	
	/**
	 * 
	 * @param wolf Wolf being attacked by a player.
	 * @param attackingPlayer Player attacking the wolf.
	 * @return true when the attackingPlayer is the owner
	 */
	private static boolean isOwner(Wolf wolf, Player attackingPlayer) {
		return wolf.getOwner() instanceof HumanEntity owner && owner.getUniqueId().equals(attackingPlayer.getUniqueId());
	}

	private static boolean isTargetingPlayer(Wolf wolf, Player attackingPlayer) {
		return wolf.isAngry() && wolf.getTarget() != null && wolf.getTarget().equals(attackingPlayer);
	}

	private static boolean isATamedWolfWithAOnlinePlayer(Wolf wolf) {
		return wolf.getOwner() instanceof HumanEntity owner && Bukkit.getPlayer(owner.getUniqueId()) != null;
	}
	
	public static boolean preventDispenserDamage(Block dispenser, Entity entity, DamageCause cause) {
		TownBlock dispenserTB = WorldCoord.parseWorldCoord(dispenser).getTownBlockOrNull();
		TownBlock defenderTB = WorldCoord.parseWorldCoord(entity).getTownBlockOrNull();
		
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(dispenser.getWorld());
		if (world == null || !world.isUsingTowny())
			return false;
		
		// Allow players and mobs to be damaged by dispensers in the wilderness
		boolean preventDamage = dispenserTB != null || defenderTB != null;

		return BukkitTools.isEventCancelled(new TownyDispenserDamageEntityEvent(entity.getLocation(), entity, cause, defenderTB, preventDamage, dispenser));
	}

	private static boolean isNotNPC(Entity entity) {
		return !PluginIntegrations.getInstance().isNPC(entity);
	}
	
	private static final @Nullable MethodHandle GET_LIGHTNING_CAUSING_ENTITY;
	
	static {
		MethodHandle temp = null;
		try {
			// https://jd.papermc.io/paper/1.20/org/bukkit/entity/LightningStrike.html#getCausingEntity()
			//noinspection JavaReflectionMemberAccess
			temp = MethodHandles.publicLookup().unreflect(LightningStrike.class.getMethod("getCausingEntity"));
		} catch (Throwable ignored) {}
		
		GET_LIGHTNING_CAUSING_ENTITY = temp;
	}
	
	@ApiStatus.Internal
	public static @Nullable Entity getLightningCausingEntity(@NotNull LightningStrike lightning) {
		if (GET_LIGHTNING_CAUSING_ENTITY == null)
			return null;
		
		try {
			return (Entity) GET_LIGHTNING_CAUSING_ENTITY.invokeExact(lightning);
		} catch (Throwable thr) {
			return null;
		}
	}

	private static boolean isTownyAdminBypassingPVP(Player attackingPlayer) {
		return TownySettings.isPVPAlwaysAllowedForAdmins() && TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(attackingPlayer);
	}
}
