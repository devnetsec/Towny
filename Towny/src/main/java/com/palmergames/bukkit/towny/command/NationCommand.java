package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.confirmations.ConfirmationTransaction;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.event.nation.NationSanctionTownAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationSanctionTownRemoveEvent;
import com.palmergames.bukkit.towny.event.nation.NationSetSpawnEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
import com.palmergames.bukkit.towny.event.nation.PreNewNationEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleOpenEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationTogglePublicEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleTaxPercentEvent;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
import com.palmergames.bukkit.towny.event.nation.NationKingChangeEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.comparators.ComparatorCaches;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ProximityUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class NationCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	
	@VisibleForTesting
	public static final List<String> nationTabCompletes = Arrays.asList(
		"list",
		"online",
		"leave",
		"withdraw",
		"deposit",
		"new",
		"create",
		"rank",
		"ranklist",
		"say",
		"set",
		"toggle",
		"invite",
		"townlist",
		"spawn",
		"sanctiontown",
		"king",
		"leader",
		"bankhistory",
		"baltop"
	);

	@VisibleForTesting
	public static final List<String> nationSetTabCompletes = Arrays.asList(
		"king",
		"leader",
		"capital",
		"board",
		"taxes",
		"name",
		"spawn",
		"spawncost",
		"title",
		"surname",
		"tag",
		"mapcolor",
		"conqueredtax",
		"taxpercentcap"
	);
	
	private static final List<String> nationListTabCompletes = Arrays.asList(
		"residents",
		"balance",
		"founded",
		"name",		
		"online",
		"open",
		"public",
		"townblocks",
		"towns",
		"upkeep"
	);
	
	static final List<String> nationToggleTabCompletes = Arrays.asList(
		"public",
		"open",
		"taxpercent"
	);

	private static final List<String> nationKingTabCompletes = Collections.singletonList("?");
	
	private static final List<String> nationConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list"
	);

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (res == null)
				return Collections.emptyList();
			Nation nation = res.getNationOrNull();

			switch (args[0].toLowerCase(Locale.ROOT)) {
				case "toggle":
					if (args.length == 2)
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.NATION_TOGGLE, nationToggleTabCompletes), args[1]);
					else if (args.length == 3)
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
					break;
				case "king":
				case "leader":
					if (args.length == 2)
						return NameUtil.filterByStart(nationKingTabCompletes, args[1]);
					break;
				case "townlist":
				case "ranklist":
				case "online":
				case "baltop":
					if (args.length == 2)
						return getTownyStartingWith(args[1], "n");
					break;
				case "spawn":
					if (args.length == 2) {
						List<String> nationOrIgnore = getTownyStartingWith(args[1], "n");
						nationOrIgnore.add("-ignore");
						return NameUtil.filterByStart(nationOrIgnore, args[1]);
					}
					if (args.length == 3) {
						return Collections.singletonList("-ignore");
					}
					break;
				case "sanctiontown":
					if (nation == null)
						break;
					if (args.length == 2) 
						return NameUtil.filterByStart(Arrays.asList("add", "remove", "list"), args[1]); 
					if (args.length == 3 && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")))
						return NameUtil.filterByStart(TownyUniverse.getInstance().getTowns()
							.stream()
							.filter(t -> !nation.hasTown(t))
							.map(Town::getName)
							.collect(Collectors.toList()), args[2]);
					if (args.length == 3 && args[1].equalsIgnoreCase("list"))
						return getTownyStartingWith(args[2], "n");
					break;
				case "rank":
					if (!res.hasNation())
						break;
					switch (args.length) {
					case 3:
						return getNationResidentNamesOfPlayerStartingWith(player, args[2]);
					case 4:
						switch (args[1].toLowerCase(Locale.ROOT)) {
							case "add":
								if (nation == null)
									return Collections.emptyList();
								return NameUtil.filterByStart(TownyPerms.getNationRanks(nation), args[3]);
							case "remove": {
								Resident rankHaver = TownyUniverse.getInstance().getResident(args[2]);
								if (rankHaver != null)
									return rankHaver.getNationRanks().isEmpty() ? Collections.emptyList() : NameUtil.filterByStart(rankHaver.getNationRanks(), args[3]);
								break;
							}
							default:
								return Collections.emptyList();
						}
					default:
						return Collections.emptyList();
					}
				case "set":
					if (!res.hasNation())
						return Collections.emptyList();
					else 
						return nationSetTabComplete(sender, nation, args);
				case "list":
					switch (args.length) {
						case 2:
							return Collections.singletonList("by");
						case 3:
							return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.NATION_LIST_BY, nationListTabCompletes), args[2]);
						default:
							return Collections.emptyList();
					}
				default:
					if (args.length == 1) {
						List<String> nationNames = NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.NATION, nationTabCompletes), args[0]);
						if (nationNames.size() > 0) {
							return nationNames;
						} else {
							return getTownyStartingWith(args[0], "n");
						}
					} else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.NATION, args[0]))
						return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.NATION, args[0]).getTabCompletion(sender, args), args[args.length-1]);
			}
		} else if (args.length == 1) {
			return filterByStartOrGetTownyStartingWith(nationConsoleTabCompletes, args[0], "n");
		}

		return Collections.emptyList();
	}
	
	static List<String> nationSetTabComplete(CommandSender sender, Nation nation, String[] args) {
		if (args.length == 2) {
			return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.NATION_SET, nationSetTabCompletes), args[1]);
		} else if (args.length > 2){
			if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_SET, args[1]))
				return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
			
			switch (args[1].toLowerCase(Locale.ROOT)) {
				case "king":
				case "leader":
				case "title":
				case "surname":
					return NameUtil.filterByStart(NameUtil.getNames(nation.getResidents()), args[2]);
				case "capital":
					return NameUtil.filterByStart(NameUtil.getNames(nation.getTowns()), args[2]);
				case "tag":
					if (args.length == 3)
						return NameUtil.filterByStart(Collections.singletonList("clear"), args[2]);
					break;
				case "mapcolor":
					if (args.length == 3)
						return NameUtil.filterByStart(TownySettings.getNationColorsMap().keySet().stream().collect(Collectors.toList()), args[2]);
					break;
				default:
					return Collections.emptyList();
			}
		}
		
		return Collections.emptyList();
	}

	public NationCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			try {
				parseNationCommand(player, args);
			} catch (TownyException te) {
				TownyMessaging.sendErrorMsg(player, te.getMessage(player));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}

		} else
			parseNationCommandForConsole(sender, args);

		return true;
	}

	/**
	 * Returns a nation from the player if args is empty or from the name supplied at arg[0].  
	 * @param player {@link Player} to try and get a nation from when args is empty.
	 * @param args {@link String[]} from which to try and get a nation name from.
	 * @return nation {@link Nation} from the Player or from the arg.
	 * @throws TownyException thrown when the player has no nation, or no nation exists by the name supplied in arg[0].
	 */
	private static Nation getPlayerNationOrNationFromArg(Player player, String[] args) throws TownyException {
		return args.length == 0 ? getNationFromPlayerOrThrow(player) : getNationOrThrow(args[0]);  
	}
	
	private void parseNationCommandForConsole(final CommandSender sender, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			HelpMenu.NATION_HELP_CONSOLE.send(sender);

		} else if (split[0].equalsIgnoreCase("list")) {

			try {
				listNations(sender, split);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}

		} else {
			Nation nation = TownyUniverse.getInstance().getNation(split[0]);
			if (nation != null)
				nationStatusScreen(sender, nation);
			else 
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", split[0]));
		}
	}

	public void parseNationCommand(final Player player, String[] split) throws TownyException, Exception {

		if (split.length == 0) {
			nationStatusScreen(player, getNationFromPlayerOrThrow(player));
			return;
		} 

		if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.NATION_HELP.send(player);
			return;
		}

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "list":
			listNations(player, split);
			break;
		case "townlist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_TOWNLIST.getNode());
			nationTownList(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		case "withdraw":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_WITHDRAW.getNode());
			TownyEconomyHandler.economyExecutor().execute(() -> nationTransaction(player, StringMgmt.remFirstArg(split), true));
			break;
		case "spawn":
			/* Permission test is internal*/
			boolean ignoreWarning = (split.length > 1 && split[1].equals("-ignore")) || (split.length > 2 && split[2].equals("-ignore"));
			nationSpawn(player, StringMgmt.remFirstArg(split), ignoreWarning);
			break;
		case "deposit":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode());
			TownyEconomyHandler.economyExecutor().execute(() -> nationTransaction(player, StringMgmt.remFirstArg(split), false));
			break;
		case "rank":
			/* Permission test is internal*/
			nationRank(player, StringMgmt.remFirstArg(split));
			break;
		case "ranklist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_RANKLIST.getNode());
			TownyMessaging.sendMessage(player, TownyFormatter.getRanksForNation(getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)), Translator.locale(player)));
			break;
		case "king":
		case "leader":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_LEADER.getNode());
			nationKing(player, StringMgmt.remFirstArg(split));
			break;
		case "sanctiontown":
			nationSanctionTown(player, null, StringMgmt.remFirstArg(split));
			break;
		case "set":
			/* Permission test is internal*/
			nationSet(player, StringMgmt.remFirstArg(split), false, null);
			break;
		case "toggle":
			/* Permission test is internal*/
			nationToggle(player, StringMgmt.remFirstArg(split), false, null);
			break;
		case "online":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode());
			parseNationOnlineCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "say":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SAY.getNode());
			nationSay(player, StringMgmt.remFirstArg(split));
			break;
		case "bankhistory":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY.getNode());
			nationBankHistory(player, StringMgmt.remFirstArg(split));
			break;
		case "baltop":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_BALTOP.getNode());
			parseNationBaltop(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		default:
			// Test if this is an addon command
			if (tryNationAddonCommand(player, split))
				return;
			// Test if this is a town status screen lookup.
			if (tryNationStatusScreen(player, split))
				return;
			
			// Alert the player that the subcommand doesn't exist.
			throw new TownyException(Translatable.of("msg_err_invalid_sub"));
		}
	}
	
	private boolean tryNationStatusScreen(Player player, String[] split) throws TownyException {
		Nation nation = TownyUniverse.getInstance().getNation(split[0]);
		if (nation != null) {
			if (!nation.hasResident(player.getName()))
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_OTHERNATION.getNode());

			nationStatusScreen(player, nation);
			return true;
		}
		return false;
	}

	private static boolean tryNationAddonCommand(Player player, String[] split) {
		if (TownyCommandAddonAPI.hasCommand(CommandType.NATION, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.NATION, split[0]).execute(player, "nation", split);
			return true;
		}
		return false;
}

	private void nationSay(Player player, String[] split) throws TownyException {
		if (split.length == 0)
			throw new TownyException("ex: /n say [message here]");
		getNationFromPlayerOrThrow(player).playerBroadCastMessageToNation(player, StringMgmt.join(split));
	}

	private void nationBankHistory(Player player, String[] split) throws TownyException {
		int pages = 10;
		if (split.length > 0)
			try {
				pages = Integer.parseInt(split[0]);
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_error_must_be_int"));
				return;
			}

		getNationFromPlayerOrThrow(player).generateBankHistoryBook(player, pages);
	}

	private void nationTownList(Player player, Nation nation) {
		TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getName() + " " + Translatable.of("town_plu").forLocale(player)));
		TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_towns").forLocale(player), new ArrayList<>(nation.getTowns())));
	}

	private void parseNationOnlineCommand(Player player, String[] split) throws TownyException {

		if (split.length > 0) {
			Nation nation = getNationOrThrow(split[0]);
			List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, nation);
			if (onlineResidents.size() > 0 ) {
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translatable.of("msg_nation_online").forLocale(player), nation, player));
			} else {
				TownyMessaging.sendMessage(player, Colors.White +  "0 " + Translatable.of("res_list").forLocale(player) + " " + (Translatable.of("msg_nation_online").forLocale(player) + ": " + nation));
			}
		} else {
			Nation nation = getNationFromPlayerOrThrow(player);
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translatable.of("msg_nation_online").forLocale(player), nation, player));
		}
	}

	public void nationRank(Player player, String[] split) throws TownyException {

		if (split.length < 3
			|| !(split[0].equalsIgnoreCase("add") || split[0].equalsIgnoreCase("remove"))) {
			// Help output.
			HelpMenu.NATION_RANK.send(player);
			return;
		}

		Resident resident = getResidentOrThrow(player);
		Resident target = getResidentOrThrow(split[1]);
		Nation nation = getNationFromResidentOrThrow(resident);

		if (!nation.hasResident(target))
			throw new TownyException(Translatable.of("msg_err_not_same_nation", target.getName()));

		/*
		 * Match casing to an existing rank, returns null if Nation rank doesn't exist.
		 */
		String rank = TownyPerms.matchNationRank(split[2]);
		if (rank == null)
			throw new TownyException(Translatable.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getNationRanks(), ", ")));

		/*
		 * Only allow the player to assign ranks if they have the grant perm for it.
		 */
		checkPermOrThrowWithMessage(player, PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank.toLowerCase(Locale.ROOT)), Translatable.of("msg_no_permission_to_give_rank"));

		Translatable nationWord = Translatable.of("nation_sing");
		if (split[0].equalsIgnoreCase("add")) {
			if (target.hasNationRank(rank)) // Must already have this rank
				throw new TownyException(Translatable.of("msg_resident_already_has_rank", target.getName(), nationWord));

			if (TownyPerms.ranksWithNationLevelRequirementPresent()) {
				int rankLevelReq = TownyPerms.getRankNationLevelReq(rank);
				int levelNumber = target.getNationOrNull().getLevelNumber();
				if (rankLevelReq > levelNumber)
					throw new TownyException(Translatable.of("msg_town_or_nation_level_not_high_enough_for_this_rank", nationWord, rank, nationWord, levelNumber, rankLevelReq));
			}

			BukkitTools.ifCancelledThenThrow(new NationRankAddEvent(nation, rank, target));

			target.addNationRank(rank);
			TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_given_rank", nationWord, rank, target.getName()));
			if (target.isOnline()) {
				TownyMessaging.sendMsg(target.getPlayer(), Translatable.of("msg_you_have_been_given_rank", nationWord, rank));
				plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
			}
		}

		if (split[0].equalsIgnoreCase("remove")) {
			if (!target.hasNationRank(rank)) // Doesn't have this rank
				throw new TownyException(Translatable.of("msg_resident_doesnt_have_rank", target.getName(), nationWord));

			BukkitTools.ifCancelledThenThrow(new NationRankRemoveEvent(nation, rank, target));

			target.removeNationRank(rank);
			TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_taken_rank_from", nationWord, rank, target.getName()));
			if (target.isOnline()) {
				TownyMessaging.sendMsg(target.getPlayer(), Translatable.of("msg_you_have_had_rank_taken", nationWord, rank));
				plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
			}
		}

		/*
		 * If we got here we have made a change Save the altered resident
		 * data.
		 */
		target.save();

	}

	/**
	 * Send a list of all nations in the universe to player Command: /nation
	 * list
	 *
	 * @param sender - Sender (player or console.)
	 * @param split  - Current command arguments.
	 * @throws TownyException - Thrown when player does not have permission node.
	 */
	public void listNations(CommandSender sender, String[] split) throws TownyException {
		
		boolean console = true;
		Player player = null;
		
		if ( split.length == 2 && split[1].equals("?")) {
			HelpMenu.NATION_LIST.send(sender);
			return;
		}
		
		if (sender instanceof Player) {
			console = false;
			player = (Player) sender;
		}

		/*
		 * The default comparator on /n list is by residents, test it before we start anything else.
		 */
		if (split.length < 2 && !console)
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_RESIDENTS.getNode());
		
		List<Nation> nationsToSort = new ArrayList<>(TownyUniverse.getInstance().getNations());
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		ComparatorType type = ComparatorType.RESIDENTS;
		int total = (int) Math.ceil(((double) nationsToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) { // Is a case of someone using /n list by {comparator}
				if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_LIST_BY, split[i+1])) {
					TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_LIST_BY, split[i+1]).execute(sender, "nation", split);
					return;
				}

				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_multiple_comparators_nation"));
					return;
				}
				i++;
				if (i < split.length) {
					comparatorSet = true;
					if (split[i].equalsIgnoreCase("resident")) 
						split[i] = "residents";
					
					if (!console)
						checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode(split[i]));
					
					if (!nationListTabCompletes.contains(split[i].toLowerCase(Locale.ROOT)))
						throw new TownyException(Translatable.of("msg_error_invalid_comparator_nation", nationListTabCompletes.stream().filter(comp -> sender.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode(comp))).collect(Collectors.joining(", "))));

					type = ComparatorType.valueOf(split[i].toUpperCase(Locale.ROOT));
				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_missing_comparator"));
					return;
				}
				comparatorSet = true;
			} else { // Is a case of someone using /n list, /n list # or /n list by {comparator} #
				if (pageSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_too_many_pages"));
					return;
				}
				page = MathUtil.getPositiveIntOrThrow(split[i]);
				if (page == 0) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_int"));
					return;
				}
				pageSet = true;
			}
		}

	    if (page > total) {
	        TownyMessaging.sendErrorMsg(sender, Translatable.of("LIST_ERR_NOT_ENOUGH_PAGES", total));
	        return;
	    }

	    final ComparatorType finalType = type;
	    final int pageNumber = page;
		try {
			plugin.getScheduler().runAsync(() -> TownyMessaging.sendNationList(sender, ComparatorCaches.getNationListCache(finalType), finalType, pageNumber, total));
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_comparator_failed"));
		}

	}

	private Town getTownForNationCapital(Player player) throws TownyException {
		Resident resident = getResidentOrThrow(player);
		Town town = getTownFromResidentOrThrow(resident);
		if (!town.hasEnoughResidentsToBeANationCapital())
			throw new TownyException(Translatable.of("msg_err_not_enough_residents_new_nation"));

		if (!resident.isMayor() && !town.hasResidentWithRank(resident, "assistant"))
			throw new TownyException(Translatable.of("msg_peasant_right"));
		return town;
	}

	public void nationKing(Player player, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			HelpMenu.KING_HELP.send(player);
	}

	public static void nationSanctionTown(CommandSender sender, Nation nation, String[] args) throws TownyException {
		if (args.length == 0 || args[0].equals("?")) {
			HelpMenu.NATION_SANCTIONTOWN.send(sender);
			return;
		}

		if (nation == null && sender instanceof Player player)
			nation = getNationFromPlayerOrThrow(player);

		if (nation == null)
			throw new TownyException(Translatable.of("msg_err_no_nation_cannot_do"));

		if (args[0].toLowerCase(Locale.ROOT).equals("list")) {
			if (args.length == 2)
				nation = getNationOrThrow(args[1]);
			nationSanctionTownList(sender, nation);
			return;
		}

		if (args.length != 2) {
			HelpMenu.NATION_SANCTIONTOWN.send(sender);
			return;
		}
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SANCTIONTOWN.getNode());
		Town town = getTownOrThrow(args[1]);
		switch(args[0].toLowerCase(Locale.ROOT)) {
		case "add" -> nationSanctionTownAdd(sender, nation, town);
		case "remove" -> nationSactionTownRemove(sender, nation, town);
		default -> HelpMenu.NATION_SANCTIONTOWN.send(sender);
		}
	}

	private static void nationSanctionTownList(CommandSender sender, Nation nation) {
		if (nation.getSanctionedTowns().isEmpty()) {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_err_nation_has_no_sanctioned_towns"));
			return;
		}
		Translator translator = Translator.locale(sender);
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(nation.getName() + " " + translator.of("title_nation_sanctioned_towns")));
		TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedTownyObjects(translator.of("title_nation_sanctioned_towns"), new ArrayList<>(nation.getSanctionedTowns())));
	}

	private static void nationSanctionTownAdd(CommandSender sender, Nation nation, Town town) throws TownyException {
		if (nation.hasTown(town))
			throw new TownyException(Translatable.of("msg_err_nation_cannot_sanction_own_town"));

		if (nation.hasSanctionedTown(town))
			throw new TownyException(Translatable.of("msg_err_nation_town_already_sanctioned"));

		BukkitTools.ifCancelledThenThrow(new NationSanctionTownAddEvent(nation, town));

		nation.addSanctionedTown(town);
		nation.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_err_nation_town_sanctioned", town.getName()));
	}

	private static void nationSactionTownRemove(CommandSender sender, Nation nation, Town town) throws TownyException {
		if (!nation.hasSanctionedTown(town))
			throw new TownyException(Translatable.of("msg_err_nation_town_isnt_sanctioned"));

		BukkitTools.ifCancelledThenThrow(new NationSanctionTownRemoveEvent(nation, town));

		nation.removeSanctionedTown(town);
		nation.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_err_nation_town_unsanctioned", town.getName()));
	}

	public static void nationSet(CommandSender sender, String[] split, boolean admin, Nation nation) throws TownyException {
		if (split.length == 0) {
			HelpMenu.NATION_SET.send(sender);
			return;
		}

		Resident resident;
		try {
			if (!admin && sender instanceof Player player) {
				resident = getResidentOrThrow(player);
				nation = getNationFromResidentOrThrow(resident);
			} else // treat resident as king for testing purposes.
				resident = nation.getKing();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(sender, x.getMessage(sender));
			return;
		}
		
		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "leader":
		case "king":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_KING.getNode());
			nationSetKing(sender, nation, split, admin);
			break;
		case "capital":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_CAPITAL.getNode());
			nationSetCapital(sender, nation, split, admin);
			break;
		case "spawn":
			final Player player = catchConsole(sender);
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode());
			parseNationSetSpawnCommand(player, nation, admin);
			break;
		case "taxes":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAXES.getNode());
			nationSetTaxes(sender, nation, split, admin);
			break;
		case "taxpercentcap":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAXPERCENTCAP.getNode());
			nationSetTaxPercentCap(sender, split, nation);
			break;
		case "spawncost":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWNCOST.getNode());
			nationSetSpawnCost(sender, nation, split, admin);
			break;
		case "name":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode());
			nationSetName(sender, nation, split, admin);
			break;
		case "tag":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAG.getNode());
			nationSetTag(sender, nation, split, admin);
			break;
		case "title":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode());
			nationSetTitle(sender, nation, resident, split, admin);
			break;
		case "surname":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode());
			nationSetSurname(sender, nation, resident, split, admin);
			break;
		case "board":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode());
			nationSetBoard(sender, nation, split);
			break;
		case "mapcolor":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_MAPCOLOR.getNode());
			nationSetMapColor(sender, nation, split, admin);
			break;
		default:
			// Test if this is an add-on command.
			if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_SET, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_SET, split[0]).execute(sender, "nation", split);
				return;
			}

			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", split[0]));
			return;
		}
		nation.save();
	}

	private static void nationSetMapColor(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /nation set mapcolor brown.");

		String color = StringMgmt.join(StringMgmt.remFirstArg(split), " ").toLowerCase(Locale.ROOT);

		if (!TownySettings.getNationColorsMap().containsKey(color))
			throw new TownyException(Translatable.of("msg_err_invalid_nation_map_color", TownySettings.getNationColorsMap().keySet().toString()));

		double cost = TownySettings.getNationSetMapColourCost();
		if (cost > 0)
			Confirmation
				.runOnAccept(() -> setNationMapColor(nation, color, admin, sender))
				.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
				.setCost(new ConfirmationTransaction(() -> cost, nation, "Cost of setting nation map color."))
				.sendTo(sender);
		else 
			setNationMapColor(nation, color, admin, sender);
	}

	private static void setNationMapColor(Nation nation, String color, boolean admin, CommandSender sender) {
		nation.setMapColorHexCode(TownySettings.getNationColorsMap().get(color));
		nation.save();
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_map_color_changed", color));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_nation_map_color_changed", color));
	}

	private static void nationSetBoard(CommandSender sender, Nation nation, String[] split) {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set board " + Translatable.of("town_help_9").forLocale(sender));
			return;
		} else {
			String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

			if (!line.equals("none")) {
				if (!NameValidation.isValidBoardString(line)) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_string_nationboard_not_set"));
					return;
				}
				// TownyFormatter shouldn't be given any string longer than 159, or it has trouble splitting lines.
				if (line.length() > 159)
					line = line.substring(0, 159);
			} else 
				line = "";
			
			nation.setBoard(line);
			TownyMessaging.sendNationBoard(sender, nation);
		}
	}

	private static void nationSetSurname(CommandSender sender, Nation nation, Resident resident, String[] split, boolean admin) throws TownyException {
		// Give the resident a title
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set surname bilbo the dwarf ");
		else
			resident = getResidentOrThrow(split[1]);

		if (!nation.hasResident(resident)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_same_nation", resident.getName()));
			return;
		}

		String surname = NameValidation.checkAndFilterTitlesSurnameOrThrow(StringMgmt.remArgs(split, 2));

		if (TownySettings.doesSenderRequirePermissionNodeToAddColourToTitleOrSurname() && Colors.containsColourCode(surname))
			checkPermOrThrowWithMessage(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE_COLOUR.getNode(),
					Translatable.of("msg_err_you_dont_have_permission_to_use_colours"));

		resident.setSurname(surname);
		resident.save();

		if (resident.hasSurname()) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname())));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname())));
		} else {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_clear_title_surname", "Surname", resident.getName()));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_title_surname", "Surname", resident.getName()));
		}


	}

	private static void nationSetTitle(CommandSender sender, Nation nation, Resident resident, String[] split, boolean admin) throws TownyException {
		// Give the resident a title
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set title bilbo Jester ");
		else
			resident = getResidentOrThrow(split[1]);
		
		if (!nation.hasResident(resident)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_same_nation", resident.getName()));
			return;
		}

		String title = NameValidation.checkAndFilterTitlesSurnameOrThrow(StringMgmt.remArgs(split, 2));

		if (TownySettings.doesSenderRequirePermissionNodeToAddColourToTitleOrSurname() && Colors.containsColourCode(title))
			checkPermOrThrowWithMessage(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE_COLOUR.getNode(),
					Translatable.of("msg_err_you_dont_have_permission_to_use_colours"));

		resident.setTitle(title);
		resident.save();

		if (resident.hasTitle()) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle())));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle())));
		} else {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_clear_title_surname", "Title", resident.getName()));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_title_surname", "Title", resident.getName()));
		}
	}

	private static void nationSetTag(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		String name = sender instanceof Player ? sender.getName() : "Console"; 
		
		if (split.length < 2)
			throw new TownyException("Eg: /nation set tag PLT");
		else if (split[1].equalsIgnoreCase("clear")) {
			nation.setTag(" ");
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_reset_nation_tag", name));
		} else {
			String tag = NameValidation.checkAndFilterTagOrThrow(split[1]);
			nation.setTag(tag);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_set_nation_tag", name, nation.getTag()));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_nation_tag", name, nation.getTag()));
		}
	}

	private static void nationSetName(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (admin || !(sender instanceof Player))
			throw new TownyException("Use /ta nation [nation] rename");

		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set name Plutoria");				
		else {
			
			String name = String.join("_", StringMgmt.remFirstArg(split));

			name = NameValidation.checkAndFilterGovernmentNameOrThrow(name, nation);
			if (TownyUniverse.getInstance().hasNation(name))
				throw new TownyException(Translatable.of("msg_err_name_validation_name_already_in_use", name));


			if (TownySettings.getTownAutomaticCapitalisationEnabled())
				name = StringMgmt.capitalizeStrings(name);
			
			if(TownyEconomyHandler.isActive() && TownySettings.getNationRenameCost() > 0) {
				if (!nation.getAccount().canPayFromHoldings(TownySettings.getNationRenameCost()))
					throw new TownyException(Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())));

				final Nation finalNation = nation;
				final String finalName = name;
				Confirmation.runOnAccept(() -> nationRename((Player) sender, finalNation, finalName))
					.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())))
					.sendTo(sender);

			} else {
				nationRename((Player) sender, nation, name);
			}
		}
	}

	private static void nationSetSpawnCost(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set spawncost 70");
		else {
			double amount = MoneyUtil.getMoneyAboveZeroOrThrow(split[1]);
			if (TownySettings.getSpawnTravelCost() < amount)
				throw new TownyException(Translatable.of("msg_err_cannot_set_spawn_cost_more_than", TownySettings.getSpawnTravelCost()));

			nation.setSpawnCost(amount);
			String name = sender instanceof Player ? sender.getName() : "Console"; 
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_spawn_cost_set_to", name, Translatable.of("nation_sing"), split[1]));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_spawn_cost_set_to", name, Translatable.of("nation_sing"), split[1]));
		}
	}

	private static void nationSetTaxes(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /nation set taxes 70");
		Double amount = MathUtil.getDoubleOrThrow(split[1]);
		if (amount < 0 && !TownySettings.isNegativeNationTaxAllowed())
			throw new TownyException(Translatable.of("msg_err_negative_money"));
		if (nation.isTaxPercentage() && (amount > 100 || amount < 0.0))
			throw new TownyException(Translatable.of("msg_err_not_percentage"));
		if (!TownySettings.isNegativeNationTaxAllowed() && TownySettings.getNationDefaultTaxMinimumTax() > amount)
			throw new TownyException(Translatable.of("msg_err_tax_minimum_not_met", TownySettings.getNationDefaultTaxMinimumTax()));
		nation.setTaxes(amount);
		if (admin) 
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_nation_tax", sender.getName(), nation.getTaxes()));
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_set_nation_tax", sender.getName(), nation.getTaxes()));
	}

	public static void nationSetTaxPercentCap(CommandSender sender, String[] split, Nation nation) throws TownyException {
		if (!nation.isTaxPercentage())
			throw new TownyException(Translatable.of("msg_max_tax_amount_only_for_percent"));

		if (split.length < 2) 
			throw new TownyException("Eg. /nation set taxpercentcap 10000");

		nation.setMaxPercentTaxAmount(MathUtil.getPositiveIntOrThrow(split[1]));

		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_set_tax_max_percent_amount", sender.getName(), TownyEconomyHandler.getFormattedBalance(nation.getMaxPercentTaxAmount())));
	}

	private static void nationSetCapital(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set capital {town name}");
			return;
		}

		final Town newCapital = getTownOrThrow(split[1]);
		changeNationOwnership(sender, nation, newCapital, admin);
	}
	
	private static void changeNationOwnership(CommandSender sender, final Nation nation, Town newCapital, boolean admin) {
		final Town existingCapital = nation.getCapital();
		if (existingCapital != null && existingCapital.getUUID().equals(newCapital.getUUID())) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_warn_town_already_capital", newCapital.getName()));
			return;
		}

		boolean capitalNotEnoughResidents = !newCapital.hasEnoughResidentsToBeANationCapital();
		if (capitalNotEnoughResidents && !admin) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_not_enough_residents_capital", newCapital.getName()));
			return;
		}
		
		boolean capitalTooManyResidents = !existingCapital.isAllowedThisAmountOfResidents(existingCapital.getNumResidents(), false); 
		if (capitalTooManyResidents && !admin) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_capital_too_many_residents", newCapital.getName()));
			return;
		}
		
		Runnable processCommand = () -> {
			Resident oldKing = nation.getKing();
			Resident newKing = newCapital.getMayor();

			NationKingChangeEvent nationKingChangeEvent = new NationKingChangeEvent(oldKing, newKing);

			// Do proximity tests.
			if (TownySettings.getNationProximityToCapital() > 0 ) {
				List<Town> removedTowns = ProximityUtil.gatherOutOfRangeTowns(nation, newCapital);

				// There are going to be some towns removed from the nation, so we'll do a Confirmation.
				if (!removedTowns.isEmpty()) {
					Confirmation.runOnAccept(() -> {
						if (BukkitTools.isEventCancelled(nationKingChangeEvent) && !admin) {
							TownyMessaging.sendErrorMsg(sender, nationKingChangeEvent.getCancelMessage());
							return;
						}
						
						nation.setCapital(newCapital);
						ProximityUtil.removeOutOfRangeTowns(nation);
						plugin.resetCache();
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
						
						if (admin)
							TownyMessaging.sendMsg(sender, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
						
						nation.save();
					})
					.setTitle(Translatable.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")))
					.sendTo(sender);
					
					return;
				}
			}
			
			// Proximity doesn't factor in or no towns would be considered out of range after changing the capital.
			// Send a confirmation
			Confirmation.runOnAccept(() -> {
				if (BukkitTools.isEventCancelled(nationKingChangeEvent) && !admin) {
					TownyMessaging.sendErrorMsg(sender, nationKingChangeEvent.getCancelMessage());
					return;
				}
				
				nation.setCapital(newCapital);
				plugin.resetCache();
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));

				if (admin)
					TownyMessaging.sendMsg(sender, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));

				nation.save();
			})
			.setTitle(Translatable.of("msg_warn_are_you_sure_you_want_to_transfer_nation_ownership", newCapital.getMayor().getName()))
			.sendTo(sender);
		};

		if (capitalNotEnoughResidents || capitalTooManyResidents)
			Confirmation.runOnAccept(processCommand)
				.setTitle(Translatable.of("msg_warn_overriding_server_config"))
				.sendTo(sender);
		else processCommand.run();
	}

	private static void nationSetKing(CommandSender sender, Nation nation, String[] split, boolean admin) {

		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set leader Dumbo");
		else
			try {
				final Resident newKing = getResidentOrThrow(split[1]);
				if (!nation.hasResident(newKing))
					throw new TownyException(Translatable.of("msg_err_king_not_in_nation"));
				
				if (!newKing.isMayor())
					throw new TownyException(Translatable.of("msg_err_new_king_notmayor"));
				
				changeNationOwnership(sender, nation, getResidentOrThrow(split[1]).getTown(), admin);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
	}

	private static void parseNationSetSpawnCommand(Player player, Nation nation, boolean admin) throws TownyException {
		if (TownyAPI.getInstance().isWilderness(player.getLocation()))
			throw new TownyException(Translatable.of("msg_cache_block_error_wild", "set spawn"));

		NationSetSpawnEvent event = new NationSetSpawnEvent(nation, player, player.getLocation());
		if (BukkitTools.isEventCancelled(event) && !admin)
			throw new TownyException(event.getCancelMessage());

		Location newSpawn = admin ? player.getLocation() : event.getNewSpawn();

		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(newSpawn);
		Town town = townBlock.getTownOrNull();

		// Nation spawns either have to be inside of the capital.
		if (nation.getCapital() != null 
			&& TownySettings.isNationSpawnOnlyAllowedInCapital()
			&& !town.getUUID().equals(nation.getCapital().getUUID()))
				throw new TownyException(Translatable.of("msg_err_spawn_not_within_capital"));
		// Or they can be in any town in the nation.
		else 
			if(!nation.getTowns().contains(town))
				throw new TownyException(Translatable.of("msg_err_spawn_not_within_nationtowns"));
		
		// Remove the SpawnPoint particles.
		if (nation.hasSpawn())
			TownyUniverse.getInstance().removeSpawnPoint(nation.getSpawn());
		
		// Set the spawn point and send feedback message.
		nation.setSpawn(newSpawn);
		TownyMessaging.sendMsg(player, Translatable.of("msg_set_nation_spawn"));
	}

	private static void parseNationBaltop(Player player, Nation nation) {
		plugin.getScheduler().runAsync(() -> {
			StringBuilder sb = new StringBuilder();
			List<Resident> residents = new ArrayList<>(nation.getResidents());
			residents.sort(Comparator.<Resident>comparingDouble(res -> res.getAccount().getCachedBalance()).reversed());

			int i = 0;
			for (Resident res : residents)
				sb.append(Translatable.of("msg_baltop_book_format", ++i, res.getName(), TownyEconomyHandler.getFormattedBalance(res.getAccount().getCachedBalance())).forLocale(player) + "\n");

			plugin.getScheduler().run(player, () -> player.openBook(BookFactory.makeBook("Nation Baltop", nation.getName(), sb.toString())));
		});
	}

	public static void nationToggle(CommandSender sender, String[] split, boolean admin, Nation nation) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.NATION_TOGGLE_HELP.send(sender);
			return;
		}
		Resident resident;

		if (!admin) {
			resident = getResidentOrThrow(((Player) sender));
			nation = getNationFromResidentOrThrow(resident);
		} else // Treat any resident tests as though the king were doing it.
			resident = nation.getKing();

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2)
			choice = BaseCommand.parseToggleChoice(split[1]);

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "public":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_PUBLIC.getNode());
			nationTogglePublic(sender, nation, choice, admin);
			break;
		case "open":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_OPEN.getNode());
			nationToggleOpen(sender, nation, choice, admin);
			break;
        case "taxpercent":
            checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_TAXPERCENT.getNode());
            nationToggleTaxPercent(sender, nation, choice, admin);
            break;
		default:
			// Check if this is an add-on command.
			if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_TOGGLE, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_TOGGLE, split[0]).execute(sender, "nation", split);
				return;
			}

			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", split[0]));
			return;
		}
		nation.save();
	}

	private static void nationTogglePublic(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		NationTogglePublicEvent preEvent = new NationTogglePublicEvent(sender, nation, admin, choice.orElse(!nation.isPublic()));
		if (BukkitTools.isEventCancelled(preEvent))
			throw new TownyException(preEvent.getCancelMessage());

		// Set the toggle setting.
		nation.setPublic(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_changed_public", nation.isPublic() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	private static void nationToggleOpen(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		NationToggleOpenEvent preEvent = new NationToggleOpenEvent(sender, nation, admin, choice.orElse(!nation.isOpen()));
		if (BukkitTools.isEventCancelled(preEvent))
			throw new TownyException(preEvent.getCancelMessage());

		// Set the toggle setting.
		nation.setOpen(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_changed_open", nation.isOpen() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

    private static void nationToggleTaxPercent(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
        	// Fire cancellable event directly before setting the toggle.
		NationToggleTaxPercentEvent preEvent = new NationToggleTaxPercentEvent(sender, nation, admin, choice.orElse(!nation.isTaxPercentage()));
		if (BukkitTools.isEventCancelled(preEvent))
			throw new TownyException(preEvent.getCancelMessage());
		// Set the toggle setting.
		nation.setTaxPercentage(preEvent.getFutureState());
		
		// Send message feedback.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_changed_taxpercent", nation.isTaxPercentage() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_taxpercent", nation.isTaxPercentage() ? Translatable.of("enabled") : Translatable.of("disabled")));
    }

	public static void nationRename(Player player, Nation nation, String newName) {
		try {
			BukkitTools.ifCancelledThenThrow(new NationPreRenameEvent(nation, newName));
	
			double renameCost = TownySettings.getNationRenameCost();
			if (TownyEconomyHandler.isActive() && renameCost > 0 && !nation.getAccount().withdraw(renameCost, String.format("Nation renamed to: %s", newName)))
				throw new TownyException(Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(renameCost)));
	
			TownyUniverse.getInstance().getDataSource().renameNation(nation, newName);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_set_name", player.getName(), nation.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}

	/**
	 * Performs final checks before sending to SpawnUtil.
	 *
	 * @param player        Player spawning.
	 * @param split         Current command arguments.
	 * @param ignoreWarning Whether to ignore the cost
	 * @throws TownyException Exception thrown to deliver feedback message denying
	 *                        spawn.
	 */
	public static void nationSpawn(Player player, String[] split, boolean ignoreWarning) throws TownyException {

		Nation nation = getPlayerNationOrNationFromArg(player, split);

		String notAffordMSG = split.length == 0 ? 
			Translatable.of("msg_err_cant_afford_tp").forLocale(player) : 
			Translatable.of("msg_err_cant_afford_tp_nation", nation.getName()).forLocale(player);
		SpawnUtil.sendToTownySpawn(player, split, nation, notAffordMSG, false, ignoreWarning, SpawnType.NATION);
	}

	private static void nationTransaction(Player player, String[] args, boolean withdraw) {
		try {
			Resident resident = getResidentOrThrow(player);
			Nation nation = getNationFromResidentOrThrow(resident);

			if (args.length < 1 || args.length > 2)
				throw new TownyException(Translatable.of("msg_must_specify_amnt", "/nation" + (withdraw ? " withdraw" : " deposit")));

			int amount;
			if ("all".equalsIgnoreCase(args[0].trim()))
				amount = (int) Math.floor(withdraw ? nation.getAccount().getHoldingBalance() : resident.getAccount().getHoldingBalance());
			else 
				amount = MathUtil.getIntOrThrow(args[0].trim());

			// Stop 0 amounts being supplied.
			if (amount == 0)
				throw new TownyException(Translatable.of("msg_err_amount_must_be_greater_than_zero"));

			if (args.length == 1) {
				if (withdraw)
					MoneyUtil.nationWithdraw(player, resident, nation, amount);
				else 
					MoneyUtil.nationDeposit(player, resident, nation, amount);
				return;
			}
			
			if (withdraw)
				throw new TownyException(Translatable.of("msg_must_specify_amnt", "/nation withdraw"));

			// Check depositing into another town
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT_OTHER.getNode());

			Town town = getTownOrThrow(args[1]);
			if (!nation.hasTown(town))
				throw new TownyException(Translatable.of("msg_err_not_same_nation", town.getName()));

			MoneyUtil.townDeposit(player, resident, town, nation, amount);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
    }
    
	private void nationStatusScreen(CommandSender sender, Nation nation) {
		/*
		 * This is run async because it will ping the economy plugin for the nation bank value.
		 */
		TownyEconomyHandler.economyExecutor().execute(() -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(nation, sender)));
	}
	
	/**
	 * Parse a page number from a {@link String[]} 
	 * @param split {@link String[]} which should contain a page number at some position.
	 * @param i the array element which will be looked at for a page value.
	 * @return 1 or the page number from the {@link String[]}.
	 */
	private int getPage(String[] split, int i) {
		int page = 1;
		if (split.length > i) {
			try {
				page = Integer.parseInt(split[i]);
			} catch (NumberFormatException ignored) {}
		}
		return page;
	}
}
