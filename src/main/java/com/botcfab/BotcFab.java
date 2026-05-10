package com.botcfab;

import com.botcfab.classes.BotcGame;
import com.botcfab.classes.BotcPlayer;
import com.botcfab.classes.BotcRole;
import com.botcfab.classes.BotcRoleCacher;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import net.minecraft.state.property.Properties;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static com.botcfab.FormattingHelper.formatRoleName;
import static com.botcfab.FormattingHelper.getPlayerOrder;
import static com.botcfab.PlayerUtils.*;
import static  com.botcfab.ItemUtils.*;

import static net.minecraft.server.command.CommandManager.literal;

public class BotcFab implements ModInitializer {
    public static final String MOD_ID = "botc-fab";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    //Removed all other teams, screw coloured names
    static final String TEAM_ALL = "team_all";
    //Variable for tag definitions
    static final String STORYTELLER = "storyteller";
    static final String PLAYER = "player";
    static final String SPEC = "spectator";
    static final String TRAVELLER = "traveller";
    static final String ALIVE = "alive";
    static final String MARKED = "marked";
    static final String DEAD = "dead";
    static final String GHOST = "ghost";
    static final String DEATH_FLAG = "death_flag";
    static final String REVIVE_FLAG = "revive_flag";
    static final String ACCUSED = "accused";
    static final String CURRENT_EXECUTEE = "current_executee";
    static final String LEGION = "legion";
    static final String SURVIVE_EXECUTION = "survive_exe";
    static final String INVIS_TAG = "invisible";
    static final List<String> ALL_TAGS = Arrays.asList(PLAYER,STORYTELLER,SPEC,TRAVELLER,ALIVE,MARKED,DEAD,GHOST,DEATH_FLAG,REVIVE_FLAG,ACCUSED,CURRENT_EXECUTEE,LEGION,SURVIVE_EXECUTION,INVIS_TAG);
    static final List<String> ALL_GAME_TAGS = Arrays.asList(ALIVE,MARKED,DEAD,GHOST,DEATH_FLAG,REVIVE_FLAG,ACCUSED,CURRENT_EXECUTEE,LEGION,SURVIVE_EXECUTION,INVIS_TAG);

    static final String INFO_OBJECTIVE = "info";
    static final String ALIVE_SCORE_HOLDER = "Alive";
    static final String DEAD_SCORE_HOLDER = "Dead";
    static final String VOTE_SCORE_HOLDER = "Vote Threshold";
    public static List<String> POSSIBLE_COLOURS;
    //Huge penis of coordinates with ref, e.g. mapCoords.get("Yellow").ghost
    static Map<String, CoordinateMapper> mapCoords = new HashMap<>();
    static int maxPlayers;
    //Highest vote count
    static int highestVote;
    static int playerTotal = 0;

    //Player class list
    static BotcGame currentGame;
    static BotcRoleCacher rolesList;

    //Timer bar variables
    private static ServerBossBar timerBar;
    private static boolean timerBarActive = false;
    private static int timerBarTicks = 0;
    private static int timerDurationTicks = 10*20; // 10 seconds default
    private static boolean discussionTime = false;
    private static boolean sendHalfwayMessage = false;

    //Variables for on tick checks
    static boolean playersLockedToSeats = false;
    static boolean executionInProgress = false;
    static boolean organGrinderActive = false;

    //Text for displays
    private static MutableText MEETING_MESSAGE;
    private static MutableText RETURN_MESSAGE;
    private static MutableText DAY_MESSAGE;
    private static MutableText NIGHT_MESSAGE;
    private static MutableText KILL_TEXT;
    private static MutableText REVIVE_TEXT;
    private static MutableText EXECUTE_TEXT;

    //variables for command inputs
    private static final List<String> tpOptions = Arrays.asList("home","house","vote","chair","town","dorm","legion","evil");
    final static String DEFAULT_MAP = "default";
    final static String SCHOOL_MAP = "school";
    final static String CIRCUS_MAP = "circus";
    final static String WINTER_MAP = "winter";
    final static List<String> MAPS = Arrays.asList(DEFAULT_MAP,SCHOOL_MAP,CIRCUS_MAP, WINTER_MAP);
    static String mapSelected = DEFAULT_MAP; //Defaults to clocktower map

    //BLOCK POS FOR CLOCKTOWER MAP
    static BlockPos EXECUTE_POS;
    static BlockPos SPAWN_POS;
    static BlockPos EVIL_ROOM_POS;
    static BlockPos MINIGAMES_POS;
    static BlockPos LEAVE_VC_TRIGGER_POS;
    static BlockPos TOWN_VC_TRIGGER_POS;

    public static File getConfigFilePath() {
        // Get the Minecraft config directory
        Path configDir = FabricLoader.getInstance().getConfigDir();

        // Make your mod's subfolder (recommended)
        Path modFolder = configDir.resolve("botc-fab"); // "config/"string"
        File folder = modFolder.toFile();
        if (!folder.exists()) {
            if (folder.mkdirs()) LOGGER.info("Created config"); // create folder if it doesn't exist
        }

        // Final file path
        return switch (mapSelected) {
            case DEFAULT_MAP -> modFolder.resolve("BOTC-coords-sheet.csv").toFile();
            case SCHOOL_MAP -> modFolder.resolve("school-BOTC-coords-sheet.csv").toFile();
            case CIRCUS_MAP -> modFolder.resolve("circus-BOTC-coords-sheet.csv").toFile();
            case WINTER_MAP -> modFolder.resolve("winter-BOTC-coords-sheet.csv").toFile();
            default -> null;
        };
    }

    public static Path getFolderPath() {
        Path configDir = FabricLoader.getInstance().getConfigDir();

        // Make your mod's subfolder (recommended)
        Path modFolder = configDir.resolve("botc-fab"); // "config/"string"
        File folder = modFolder.toFile();
        if (!folder.exists()) {
            if (folder.mkdirs()) LOGGER.info("Created folder"); // create folder if it doesn't exist
        }

        return modFolder;
    }

    private static void onGameInit(MinecraftServer srv) { //Run this on server startup, onlinePlayers do not need to be connected
        ServerCommandSource src = srv.getCommandSource();
        ServerScoreboard scoreboard = srv.getScoreboard();
        ServerWorld world = src.getWorld();
        mapCoords = ImportExcelCoordinates.read(getConfigFilePath()); //Import csv file here
        rolesList = new BotcRoleCacher();

        System.out.println("INITIALISED");
        src.sendFeedback(() -> Text.literal("Starting initialise code now"), false);

        //Define game rules, shouldn't need to run every time but just to be safe
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
        world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, world.getServer());
        world.getGameRules().get(GameRules.DISABLE_RAIDS).set(true, world.getServer());
        world.getGameRules().get(GameRules.DO_FIRE_TICK).set(false, world.getServer());
        world.getGameRules().get(GameRules.COMMAND_BLOCK_OUTPUT).set(false, world.getServer());
        world.getGameRules().get(GameRules.DO_MOB_GRIEFING).set(false, world.getServer());
        world.getGameRules().get(GameRules.DO_PATROL_SPAWNING).set(false, world.getServer());
        world.getGameRules().get(GameRules.DO_TRADER_SPAWNING).set(false, world.getServer());
        world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, world.getServer());
        world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true, world.getServer());
        world.getGameRules().get(GameRules.DO_VINES_SPREAD).set(false, world.getServer());
        world.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE).set(200, world.getServer());

        // Create the one and only team we need
        createTeam(scoreboard);

        //Setup text for maps
        switch (mapSelected) {
            case SCHOOL_MAP:
                MEETING_MESSAGE = Text.literal("\nPlease head to the main hall");
                RETURN_MESSAGE = Text.literal("\nPlease return to your dorms");
                DAY_MESSAGE = Text.literal("The morning bell rings ☀")
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD));
                NIGHT_MESSAGE = Text.literal("The final bell rings 🌙")
                        .setStyle(Style.EMPTY.withColor(Formatting.BLUE));
                KILL_TEXT = Text.literal(" has gone ") //Make these not final for school map, change to expelled, suspended etc
                        .append(Text.literal("missing.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.DARK_RED))));
                REVIVE_TEXT = Text.literal(" has ")
                        .append(Text.literal("returned.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW))));
                EXECUTE_TEXT = Text.literal(" has been ")
                        .append(Text.literal("suspended.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED))));
                break;
            case CIRCUS_MAP:
                MEETING_MESSAGE = Text.literal("\nPlease head to the central tent");
                RETURN_MESSAGE = Text.literal("\nPlease return to your tents");
                DAY_MESSAGE = Text.literal("The cockerel caws ☀")
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD));
                NIGHT_MESSAGE = Text.literal("The curtain falls 🌙")
                        .setStyle(Style.EMPTY.withColor(Formatting.BLUE));
                KILL_TEXT = Text.literal(" has been ") //Make these not final for school map, change to expelled, suspended etc
                        .append(Text.literal("clowned. 🤡").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.DARK_RED))));
                REVIVE_TEXT = Text.literal(" has ")
                        .append(Text.literal("landed safely.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW))));
                EXECUTE_TEXT = Text.literal(" has been ")
                        .append(Text.literal("fired out of the cannon.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED))));
                break;
            default: //Default is used for default map as well
                MEETING_MESSAGE = Text.literal("\nPlease head to the town square");
                RETURN_MESSAGE = Text.literal("\nPlease return to your houses");
                DAY_MESSAGE = Text.literal("The sun rises ☀")
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD));
                NIGHT_MESSAGE = Text.literal("Night falls... 🌙")
                        .setStyle(Style.EMPTY.withColor(Formatting.BLUE));
                KILL_TEXT = Text.literal(" has been ") //Make these not final for school map, change to expelled, suspended etc
                        .append(Text.literal("killed.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.DARK_RED))));
                REVIVE_TEXT = Text.literal(" has been ")
                        .append(Text.literal("revived.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW))));
                EXECUTE_TEXT = Text.literal(" has been ")
                        .append(Text.literal("executed.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED))));
                break;
        }

        //Sets the coordinates missing from sheet per map
        switch (mapSelected) {
            case DEFAULT_MAP:
                POSSIBLE_COLOURS = List.of("black","yellow","orange","pink","red","purple","brown","green","white","blue","cyan");
                EXECUTE_POS = new BlockPos(6, -29, -2);
                SPAWN_POS = new BlockPos(4,-29,-2);
                EVIL_ROOM_POS = new BlockPos(126, -28, 57);
                MINIGAMES_POS = new BlockPos(-120, -27, -13);
                LEAVE_VC_TRIGGER_POS = new BlockPos(18,-37,12);
                TOWN_VC_TRIGGER_POS = new BlockPos(16,-37,12);
                break;
            case SCHOOL_MAP:
                POSSIBLE_COLOURS = List.of("black","yellow","orange","pink","red","purple","brown","green","white","blue","cyan");
                EXECUTE_POS = new BlockPos(455, 4, 157);
                SPAWN_POS = new BlockPos(455,1,190);
                EVIL_ROOM_POS = new BlockPos(1, -28, 57);
                MINIGAMES_POS = new BlockPos(-120, -27, -13);
                LEAVE_VC_TRIGGER_POS = new BlockPos(18,-37,12);
                TOWN_VC_TRIGGER_POS = new BlockPos(16,-37,12);
                break;
            case WINTER_MAP:
                POSSIBLE_COLOURS = List.of("green","cyan","blue","magenta","pink","red","orange","yellow");
                EXECUTE_POS = new BlockPos(-140, -28, 330);
                SPAWN_POS = new BlockPos(-140,-22,344);
                EVIL_ROOM_POS = new BlockPos(126, -28, 57);//Not updated, could just use same one tho
                MINIGAMES_POS = new BlockPos(-120, -27, -13);
                LEAVE_VC_TRIGGER_POS = new BlockPos(18,-37,12);
                TOWN_VC_TRIGGER_POS = new BlockPos(16,-37,12);
        }
        maxPlayers = POSSIBLE_COLOURS.size();

        // Loop through all colours in map
        for (String i : mapCoords.keySet()) {
            // Set all lamps and status markers to netherite and levers to air
            world.setBlockState(mapCoords.get(i).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState());
            world.setBlockState(mapCoords.get(i).lever, Blocks.AIR.getDefaultState());
            world.setBlockState(mapCoords.get(i).lampsVoteMarker, Blocks.COAL_BLOCK.getDefaultState());
        }

        src.sendFeedback(() -> Text.literal("Finished"), false);
    }

    static int setupGame(ServerCommandSource src) {
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        ServerScoreboard scoreboard = srv.getScoreboard();
        ServerWorld world = src.getWorld();
        ArrayList<ServerPlayerEntity> onlinePlayers = new ArrayList<>(playerMgr.getPlayerList());
        ArrayList<ServerPlayerEntity> players = new ArrayList<>();
        ServerPlayerEntity storyTeller = src.getPlayer(); // Gets the person that called the command. Whoever called it is Storyteller

        if (storyTeller != null) {
            // Remove all tags before adding new ones
            storyTeller.equipStack(EquipmentSlot.FEET,ItemStack.EMPTY); //Remove boots

            storyTeller.addCommandTag(STORYTELLER); //Add tag for storyteller
            storyTeller.removeCommandTag(PLAYER);
            src.sendFeedback(() -> (Text.literal("Storyteller is: " + storyTeller.getNameForScoreboard())),true);
            storyTeller.changeGameMode(GameMode.CREATIVE);
        } else {
            src.sendFeedback(() -> Text.literal("Failed to find storyteller. Do not execute this command from the server window"), false);
            return 0;
        }
        storyTeller.setSpawnPoint(World.OVERWORLD, EXECUTE_POS,0,true,false); //Should work now?

        for (ServerPlayerEntity p : onlinePlayers){
            resetPlayer(p); //Remove all game related tags to avoid overlaps
            scoreboard.addScoreHolderToTeam(p.getNameForScoreboard(), scoreboard.getTeam(TEAM_ALL));
            Set<String> tags = p.getCommandTags();
            if (tags.contains(SPEC)) {
                p.removeCommandTag(PLAYER);
                //p.changeGameMode(GameMode.SPECTATOR); //Don't auto change gamemode
            }
            if (tags.contains(PLAYER)) {
                p.removeCommandTag(STORYTELLER);
                playerMgr.removeFromOperators(p.getGameProfile()); //remove from ops
                players.add(p);
                p.changeGameMode(GameMode.ADVENTURE);
                p.equipStack(EquipmentSlot.FEET,ItemStack.EMPTY); //Remove boots
            }
        }

        if (onlinePlayers.isEmpty()) {
            src.sendFeedback(() -> Text.literal("No pony online :("), false);
        } else {
            BotcPlayer ST = new BotcPlayer(-1,storyTeller,null);
            currentGame = new BotcGame(ST, players.size(), POSSIBLE_COLOURS, 0);
            currentGame.setupRandomGame(players);
        }

        for (BotcPlayer player :currentGame.getPlayers()) {
            String assignedColour = player.getColour();
            ServerPlayerEntity p = player.getPlayer();
            if (p != null) { //Makes sure the server player is not null
                setColourBoots(p, assignedColour);
                p.setSpawnPoint(World.OVERWORLD, mapCoords.get(assignedColour).homeInside, 0, true, false); //Set player spawn point
            } else {
                src.sendFeedback(() -> Text.literal("Could not find player entity with colour "+ assignedColour +" in current game."), false);
            }
            //Places signs, levers update in the updatePlayer function
            Text signText;
            if (p != null){
                signText = player.getName();
            } else {
                signText = Text.literal(assignedColour);
            }
            BlockState signType;
            switch (mapSelected) {
                case DEFAULT_MAP:
                    signType = Blocks.SPRUCE_WALL_SIGN.getDefaultState();
                    switch (assignedColour) {
                        case "black", "yellow", "orange":
                            placeWallSign(world, mapCoords.get(assignedColour).sign, Direction.SOUTH, signText, assignedColour, signType);
                            break;
                        case "pink", "red", "purple":
                            placeWallSign(world, mapCoords.get(assignedColour).sign, Direction.WEST, signText, assignedColour, signType);
                            break;
                        case "brown", "green", "white":
                            placeWallSign(world, mapCoords.get(assignedColour).sign, Direction.NORTH, signText, assignedColour, signType);
                            break;
                        case "blue", "cyan", "gray":
                            placeWallSign(world, mapCoords.get(assignedColour).sign, Direction.EAST, signText, assignedColour, signType);
                            break;
                    }
                    break;
                case SCHOOL_MAP:
                    //sign behind chair
                    placeStandingSign(world, mapCoords.get(assignedColour).sign, 8, player.getName(), assignedColour);
                    //sign on wall theatre
                    placeWallSign(world, mapCoords.get(assignedColour).lampsVoteMarker.up(1).south(1), Direction.SOUTH, signText, assignedColour, Blocks.DARK_OAK_WALL_SIGN.getDefaultState());
                    //Room signs above door
                    signType = Blocks.PALE_OAK_WALL_SIGN.getDefaultState();
                    switch (assignedColour) {
                        case "black", "green", "orange":
                            placeWallSign(world, mapCoords.get(assignedColour).homeOutside.up(2), Direction.SOUTH, signText, assignedColour, signType);
                            break;
                        case "pink", "red", "purple":
                            placeWallSign(world, mapCoords.get(assignedColour).homeOutside.up(2), Direction.WEST, signText, assignedColour, signType);
                            break;
                        case "brown", "yellow", "white":
                            placeWallSign(world, mapCoords.get(assignedColour).homeOutside.up(2), Direction.NORTH, signText, assignedColour, signType);
                            break;
                        case "blue", "cyan", "gray":
                            placeWallSign(world, mapCoords.get(assignedColour).homeOutside.up(2), Direction.EAST, signText, assignedColour, signType);
                            break;
                    }
                    break;
                case WINTER_MAP:
                    signType = Blocks.SPRUCE_WALL_SIGN.getDefaultState();
                    switch (assignedColour) {
                        case "yellow", "orange":
                            placeWallSign(world, mapCoords.get(assignedColour).sign, Direction.EAST, signText, assignedColour, signType);
                            break;
                        case "pink", "red":
                            placeWallSign(world, mapCoords.get(assignedColour).sign, Direction.NORTH, signText, assignedColour, signType);
                            break;
                        case "magenta", "blue":
                            placeWallSign(world, mapCoords.get(assignedColour).sign, Direction.WEST, signText, assignedColour, signType);
                            break;
                        case "cyan", "green":
                            placeWallSign(world, mapCoords.get(assignedColour).sign, Direction.SOUTH, signText, assignedColour, signType);
                            break;
                    }
                    break;
            }
            updateVoteStatus(world, player); //player levers and update lamps
        }
        createOrSetAliveDisplay(scoreboard, srv); //Update/create scoreboard for start of game.

        //Get player total here to help account for players leaving
        playerTotal = currentGame.getTotalPlayers();
        srv.sendMessage(Text.literal("Total players: "+ playerTotal));

        //Remove votes and signs for missing players
        for (String c:POSSIBLE_COLOURS){
            if (currentGame.getPlayerAtColour(c) == null){
                world.setBlockState(mapCoords.get(c).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState()); //Set player indicator to netherite
                world.setBlockState(mapCoords.get(c).lampsVoteMarker, Blocks.AIR.getDefaultState()); //Set block to AIR for empty player seat
                world.setBlockState(mapCoords.get(c).lever, Blocks.AIR.getDefaultState()); //Remove lever
                world.setBlockState(mapCoords.get(c).sign, Blocks.AIR.getDefaultState()); //Remove empty signs
                if (mapSelected.equals(SCHOOL_MAP)) {
                    world.setBlockState(mapCoords.get(c).homeOutside.up(2), Blocks.AIR.getDefaultState()); //Remove empty room signs
                    world.setBlockState(mapCoords.get(c).sign, Blocks.AIR.getDefaultState()); //Remove empty signs
                }
            }
        }

        return 1;
    }

    static int beginGame(ServerCommandSource src) {
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();

        if (currentGame != null) {

            for (BotcPlayer p : currentGame.getPlayers()) { //Give players in game the items they need, book, teleport items, paper and role item (nether star)
                givePlayerGameItems(p.getPlayer());
                if (p.getRole() != null){
                    givePlayerRole(p.getPlayer(),p.getRole());
                }
            }
            teleportPlayers("home", srv, currentGame); //teleports onlinePlayers to homes
            sendMessageToPlayers(getPlayerOrder(currentGame), playerMgr.getPlayerList()); //Sends player order to all onlinePlayers
            nightFalls(src);

            return 1;
        } else {
            src.sendFeedback(() -> Text.literal("No game initialised. Please run setup first!"), false);
            return 0;
        }
    }

    static int nightFalls(ServerCommandSource src){
        MinecraftServer srv = src.getServer();
        ServerWorld world = src.getWorld();
        PlayerManager playerMgr = srv.getPlayerManager();
        ServerScoreboard scoreboard = world.getScoreboard();
        world.setTimeOfDay(18000L);
        playersLockedToSeats = false;
        executionInProgress = false;
        organGrinderActive = false;
        timerBarActive = false; //Clear timer for day end
        world.setBlockState(TOWN_VC_TRIGGER_POS,Blocks.AIR.getDefaultState()); //Stop auto-joining town square
        //Remove all temp tags from onlinePlayers
        for (ServerPlayerEntity p:playerMgr.getPlayerList()){
            p.removeStatusEffect(StatusEffects.BLINDNESS);
            p.removeStatusEffect(StatusEffects.DARKNESS);
        }
        currentGame.resetGameFlags();
        Team team = scoreboard.getTeam(TEAM_ALL);
        if (team != null) { //Make nametags visible at morning.
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
        }
        sendMessageToPlayers(NIGHT_MESSAGE.copy().append(RETURN_MESSAGE),playerMgr.getPlayerList()); //Send msg to all onlinePlayers
        return 1;
    }

    static int startDay(ServerCommandSource src){
        MinecraftServer srv = src.getServer();
        ServerWorld world = src.getWorld();
        List<ServerPlayerEntity> playerList = srv.getPlayerManager().getPlayerList();
        ServerScoreboard scoreboard = srv.getScoreboard();
        Team team = scoreboard.getTeam(TEAM_ALL);
        playersLockedToSeats = false;
        world.setTimeOfDay(500L);
        if (team != null) { //Make nametags visible at morning.
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        }

        sendMessageToPlayers((DAY_MESSAGE.copy().append(MEETING_MESSAGE)),playerList);
        //Send player death message and update status
        for (BotcPlayer deadPlayer:currentGame.getKillFlaggedPlayers()){
            sendMessageToPlayers(getEventText(deadPlayer.getPlayer(),DEATH_FLAG) ,playerList);
            killPlayer(deadPlayer);
            deadPlayer.removeFlags();
        }
        for (BotcPlayer revivedPlayer:currentGame.getReviveFlaggedPlayers()){
            sendMessageToPlayers(getEventText(revivedPlayer.getPlayer(),REVIVE_FLAG) ,playerList);
            revivePlayer(revivedPlayer);
            revivedPlayer.removeFlags();
        }
        for (ServerPlayerEntity p : playerList){
            showTitle(p,DAY_MESSAGE);
            p.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE,SoundCategory.BLOCKS,1.0f,0.5f);
        }
        highestVote = 0; //reset highest vote

        createOrSetAliveDisplay(scoreboard, srv); //Updates scoreboard
        return 1;
    }

    static void voteLockIn(ServerCommandSource src) {
        // remove redstone block;
        ServerWorld world = src.getWorld();
        int delayPerBlock = 20; // 1 second = 20 ticks
        int voteThreshold, startColourIndex, count = 1, alivePlayers;
        List<DelayedBlockSetter> taskList = new ArrayList<>();
        List<String> currentGameColours = currentGame.getColours();
        int maxIndex = currentGameColours.size()-1;
        //src.sendFeedback(() -> Text.literal("Max index: " + maxIndex), false); //Just for testing, not needed

        //Get vote threshold for alive onlinePlayers
        alivePlayers = currentGame.getAlivePlayers();
        voteThreshold = (alivePlayers / 2) + (alivePlayers % 2);

        BotcPlayer accusedPlayer = currentGame.getAccusedPlayer();
        if (accusedPlayer == null){
            src.sendFeedback(() -> Text.literal("No player accused, please accuse someone!"), false);
            return;
        }
        String accusedColour = accusedPlayer.getColour();

        int accusedIndex = currentGameColours.indexOf(accusedColour);
        startColourIndex = accusedIndex+1; //+1 to start from player after accused

        if (startColourIndex > maxIndex){
            startColourIndex = 0; //sets start to 0 if > possible colour size
        }

        for (int i = startColourIndex; count <= playerTotal+1; i++) { //need to be <= or else the accused doesn't get a vote (+1 cus it starts at 1 to have initial delay
            int delay = count * delayPerBlock; //Add delay between vote locks
            if (i > maxIndex){
                i = 0;
            }
            BlockPos redstoneBlock = mapCoords.get(currentGameColours.get(i)).triggersLampPiston;
            taskList.add(new DelayedBlockSetter(world, redstoneBlock, Blocks.AIR.getDefaultState(), delay)); //Set redstone block to air
            count++;
        }

        int finalStartColourIndex = startColourIndex;
        Runnable onAllDone = () -> {
            src.sendFeedback(() -> Text.literal("Starting count..."), false);
            List<ServerPlayerEntity> playerList = src.getWorld().getPlayers();
            ArrayList<Text> playersVoted = new ArrayList<>();
            int playerCount = 0, aliveVotesTotal = 0, ghostVotesTotal = 0;
            int playerSize = currentGame.getTotalPlayers();

            for (int i = finalStartColourIndex; playerCount < playerSize; i++) {
                if (i > maxIndex){
                    i = 0;
                }
                String colour = currentGameColours.get(i);
                BlockPos pos = mapCoords.get(colour).triggersLampPiston;
                BlockPos lockedVote;
                switch (mapSelected) {
                    case DEFAULT_MAP:
                        lockedVote = pos.up(2); //Position of locked in vote lamp, 2 blocks above piston
                        break;
                    case SCHOOL_MAP:
                        lockedVote = pos.south(2); //Position of locked in vote lamp, school map has horizontal vote lock-in
                        break;
                    default:
                        lockedVote = pos.up(2);
                }

                BlockState lockedVoteState = world.getBlockState(lockedVote);
                BotcPlayer playerVoting = currentGame.getPlayerAtColour(colour);
                if (lockedVoteState.getBlock() == Blocks.WAXED_COPPER_BULB && lockedVoteState.get(Properties.LIT)) {
                    aliveVotesTotal++;
                    if (playerVoting != null) {
                        playersVoted.add(playerVoting.getName());//Add player to voted list
                    }
                } else if (lockedVoteState.getBlock() == Blocks.SEA_LANTERN) {
                    ghostVotesTotal++;
                    // remove ghost vote and tag from player
                    world.setBlockState(lockedVote,Blocks.COAL_BLOCK.getDefaultState());
                    if (playerVoting != null) {
                        playersVoted.add(playerVoting.getName());//Add player to voted list
                        if (playerVoting.canLoseGhostVote()){
                            playerVoting.removeGhostVote();
                        }
                    }
                }
                world.setBlockState(pos, Blocks.REDSTONE_BLOCK.getDefaultState()); //Set back redstone blocks

                playerCount++;
            }
            int displayTotalVotes = aliveVotesTotal + ghostVotesTotal; //total votes
            int displayGhostVotes = ghostVotesTotal; //ghost votes used
            BotcPlayer markedPlayer = currentGame.getMarkedPlayer();


            MutableText votedListText;
            if (!playersVoted.isEmpty()) {
                votedListText = Text.literal("These players voted: ");
                for (Text t : playersVoted) {
                    votedListText
                            .append(t) //Add name
                            .append(Text.literal(" ")); //Space after
                }
                votedListText.setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GRAY))); //Grey for storyteller text
            } else {
                votedListText = Text.literal("");
            }
            //TODO change this to show only to storyteller + spectators (no op required)
            if (currentGame.showVoteResult()) { //Only show result to ops if hide result is on
                sendMessageToPlayers(Text.literal("A total of " + displayTotalVotes + " votes were received, including " + displayGhostVotes + " ghost votes. \n"), playerList);
            } else {
                src.sendFeedback(() -> Text.literal("A total of " + displayTotalVotes + " votes were received, including " + displayGhostVotes + " ghost votes. \n"),true);
            }
            //Show voted list to storyteller only
            currentGame.getStoryteller().getPlayer().sendMessage(votedListText);


            if ((displayTotalVotes > highestVote) && (displayTotalVotes >= voteThreshold)){ //On beating highest vote and vote threshold mark accused player and remove old.
                highestVote = displayTotalVotes; //Change the highest vote to this vote
                //Mark player for execution
                accusedPlayer.markMarked();
                if (markedPlayer != null) { //Remove any previous marked onlinePlayers
                    markedPlayer.removedMarked();
                }
                src.sendFeedback(accusedPlayer::getName, false);

                Text name = accusedPlayer.getName();
                MutableText message = name.copy();
                message.append(" has now been marked for execution");
                if (currentGame.showVoteResult()) { //if show result to players is active
                    sendMessageToPlayers(message, playerList);
                } else { //send result to ops if not initialised
                    src.sendFeedback(() -> message,true);
                }

            }
            if (displayTotalVotes == highestVote){ //On matching highest vote, remove all marked onlinePlayers
                if (markedPlayer != null) {
                    markedPlayer.removedMarked();
                }
            }
            accusedPlayer.removedAccused();
        };
        TickScheduler.scheduleGroup(taskList, onAllDone);
    }

    static int beginExecution(ServerCommandSource src){
        MinecraftServer srv = src.getServer();
        List<ServerPlayerEntity> playerList = srv.getPlayerManager().getPlayerList();
        BotcPlayer player = currentGame.getMarkedPlayer();
        if (currentGame.getAccusedPlayer() != null){
            currentGame.getAccusedPlayer().removedAccused(); //Removed any leftover accused players
        }
        for (ServerPlayerEntity p:playerList){
            p.removeStatusEffect(StatusEffects.BLINDNESS);
            p.removeStatusEffect(StatusEffects.DARKNESS);
        }
        if (player != null) {
            player.removedMarked();
            executePlayer(player);
        } else src.sendFeedback(() -> Text.literal("No pony is marked for execution so no pony was killed"),false);
        return 1;
    }

    static void startDiscussionTime(ServerCommandSource src){
        MinecraftServer srv = src.getServer();
        ServerWorld world = src.getWorld();
        world.setTimeOfDay(1000L);
        discussionTime = true; //Initiate day time pass code
        sendHalfwayMessage = true; //allow halfway msg
        startTimer(srv,6*60); //Start 6min timer
        src.sendFeedback(() -> Text.literal("Starting discussion timer"), false);
    }

    static void createTeam(@NotNull ServerScoreboard scoreboard) {
        // Check if the team already exists. Should only need to run once per server
        Team team = scoreboard.getTeam(BotcFab.TEAM_ALL);
        if (team == null) {
            // Create the team if it doesn't exist
            team = scoreboard.addTeam(BotcFab.TEAM_ALL);
            System.out.println("Created new team: " + BotcFab.TEAM_ALL);
        } else {
            System.out.println("Team already exists: " + BotcFab.TEAM_ALL);
        }
        team.setShowFriendlyInvisibles(true); //makes invisible onlinePlayers visible
    }

    static void createOrSetAliveDisplay(ServerScoreboard scoreboard, MinecraftServer srv){
        if (currentGame != null) {
            int alivePlayers = currentGame.getAlivePlayers();
            int deadPlayers = currentGame.getTotalPlayers() - alivePlayers;
            int voteThreshold = (alivePlayers / 2) + (alivePlayers % 2);
            ScoreboardObjective objective = scoreboard.getNullableObjective(INFO_OBJECTIVE);
            NumberFormat numberFormat = StyledNumberFormat.YELLOW; //Can be RED, YELLOW or EMPTY
            if (objective == null) {
                objective = scoreboard.addObjective(
                        INFO_OBJECTIVE, // Objective name (unique id)
                        ScoreboardCriterion.DUMMY, // Criterion type (dummy = manual numbers)
                        Text.literal("Player Info"), // Display name (shown in sidebar, below name, etc.)
                        ScoreboardCriterion.RenderType.INTEGER, // Render type (number type)
                        true, //Whether the value updates live
                        numberFormat //Format of numbers, colour and display
                );
            }
            ScoreAccess aliveScoreAccess = scoreboard.getOrCreateScore(ScoreHolder.fromName(ALIVE_SCORE_HOLDER), objective);
            ScoreAccess deadScoreAccess = scoreboard.getOrCreateScore(ScoreHolder.fromName(DEAD_SCORE_HOLDER), objective);
            ScoreAccess voteScoreAccess = scoreboard.getOrCreateScore(ScoreHolder.fromName(VOTE_SCORE_HOLDER), objective);
            aliveScoreAccess.setDisplayText(Text.literal("Alive "));
            aliveScoreAccess.setScore(alivePlayers);
            deadScoreAccess.setDisplayText(Text.literal("Dead "));
            deadScoreAccess.setScore(deadPlayers);
            voteScoreAccess.setDisplayText(Text.literal("Vote Threshold "));
            voteScoreAccess.setScore(voteThreshold);
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
        }
    }

    private static MutableText getEventText(ServerPlayerEntity player, String event){
        MutableText eventMsg;
        MutableText playerName = player.getStyledDisplayName().copy(); //Should be formatted with player colour from team
        eventMsg = switch (event) {
            case REVIVE_FLAG -> REVIVE_TEXT;
            case DEATH_FLAG -> KILL_TEXT;
            case CURRENT_EXECUTEE -> EXECUTE_TEXT;
            default -> Text.literal(" needs to tell Ruby she is bad at coding");
        };
        return playerName.append(eventMsg);
    }

    private static void startTimer(MinecraftServer srv, int duration){ //Starts a timer and shows boss bar as remaining, duration in seconds
        if (timerBar == null) {
            timerBar = new ServerBossBar(Text.literal("Time remaining:"), BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
        }
        timerBar.setPercent(1.0f); //100%
        timerBar.setVisible(true);
        timerBar.setColor(BossBar.Color.GREEN); //Reset colour for new timer
        for (ServerPlayerEntity player : srv.getPlayerManager().getPlayerList()) {
            timerBar.addPlayer(player);
        }

        timerBarTicks = 0;
        timerDurationTicks = duration*20*3; //*20 to get seconds value in ticks *3
        timerBarActive = true;
        sendHalfwayMessage = true;

        srv.sendMessage(Text.literal("Boss bar timer started."));
    }

    private void onWorldTick(ServerWorld world) {
        List<ServerPlayerEntity> playerList = world.getServer().getPlayerManager().getPlayerList(); //gets all onlinePlayers every tick
        for (String i : mapCoords.keySet()) {
            BlockPos leverPos = mapCoords.get(i).lever; // Get the block state at that position
            BlockState leverState = world.getBlockState(leverPos); // Get the block state at that position
            // Check if lever
            if (leverState.isOf(Blocks.LEVER)){
                BlockPos voteLampPos = mapCoords.get(i).lampsVoteMarker;
                BlockState voteLampState = world.getBlockState(mapCoords.get(i).lampsVoteMarker);
                //Update lamp state based on lever state
                if (voteLampState.contains(Properties.LIT)){
                    BlockState updatedBulbState = voteLampState.with(Properties.LIT, leverState.get(Properties.POWERED)); //This is where is checks lever power state
                    world.setBlockState(voteLampPos, updatedBulbState, 3);
                }
                if (voteLampState.isOf(Blocks.WAXED_OXIDIZED_COPPER) || voteLampState.isOf(Blocks.SEA_LANTERN)) {
                    if (leverState.get(Properties.POWERED)) {
                        world.setBlockState(voteLampPos, Blocks.SEA_LANTERN.getDefaultState(), 3);
                    } else {
                        world.setBlockState(voteLampPos, Blocks.WAXED_OXIDIZED_COPPER.getDefaultState(), 3);
                    }
                }
            }
            if (mapSelected.equals(SCHOOL_MAP)){
                BlockState voteLampState = world.getBlockState(mapCoords.get(i).lampsVoteMarker);
                //check lamp and copy to north 1, down 2 if not air
                if (!voteLampState.isAir()) {
                    BlockPos lampCopyPos = mapCoords.get(i).chair.north(1).down(2);
                    world.setBlockState(lampCopyPos, voteLampState,3);
                }
            }
        }

        if (timerBarActive) { //Timer bar for talk time
            timerBarTicks++;
            float percent = (float) timerBarTicks / timerDurationTicks;
            float progress = 1.0f - percent;
            if (timerBar != null) {
                timerBar.setPercent(Math.max(progress, 0f));
                int timeLeftSec = (timerDurationTicks - timerBarTicks)/60;
                int minutes = (timeLeftSec % 3600) / 60;
                int seconds = timeLeftSec % 60;
                String timeString = String.format("%01d:%02d", minutes, seconds);
                timerBar.setName(Text.literal("Time remaining - " + timeString));
                //day lasts ~12000 ticks, advances time to fill that during the day
                if (discussionTime) {
                    world.setTimeOfDay(Math.round(1000 + (11000*percent)));

                }
                if (progress <= 0.5f && sendHalfwayMessage){
                    sendHalfwayMessage = false;
                    sendMessageToPlayers(Text.literal("Your time is halfway through, " + String.format("%01d:%02d", minutes, seconds) + " remaining."),playerList);
                }
                if (timeLeftSec <= 60 && timeLeftSec > 56) {
                    timerBar.setColor(BossBar.Color.YELLOW);
                }
                if (timeLeftSec <= 30 && timeLeftSec > 26) {
                    timerBar.setColor(BossBar.Color.RED);
                }
            }

            if (timerBarTicks >= timerDurationTicks) { //On timer finish
                timerBarActive = false;
                discussionTime = false;
                timerBarTicks = 0;
                if (timerBar != null) {
                    timerBar.setPercent(0f);
                    timerBar.setVisible(false); //Make timer invisible as finished
                    for (ServerPlayerEntity p : playerList) {
                        showTitle(p,Text.literal("TIME IS UP"));
                        p.sendMessage(Text.literal("Return to the town square!"));
                        p.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_STEP,SoundCategory.PLAYERS,1f,0.5f);
                    }
                }
            }
        }

        if (world.getBlockState(LEAVE_VC_TRIGGER_POS).isOf(Blocks.REDSTONE_BLOCK)){
            //Set Leave vc trigger to air each tick to allow command to run
            world.setBlockState(LEAVE_VC_TRIGGER_POS,Blocks.AIR.getDefaultState());
        }

        if (currentGame != null) {
            if (currentGame.getPlayers() != null) {
                //Code for player death particles and particle effects
                for (BotcPlayer player : currentGame.getPlayers()) {
                    ServerPlayerEntity p = player.getPlayer();
                    if (p != null) {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 2, false, false));
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, -1, 2, false, false));
                        if (currentGame.invisPlayers()) {
                            if (player.getInvisStatus()) {
                                p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 1, false, false));
                                //Spooky particles   world.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY(), p.getZ(), 1, 0.4, 0, 0.4, 0.001);
                            } else {
                                p.removeStatusEffect(StatusEffects.INVISIBILITY); //Remove invisible for alive onlinePlayers
                            }
                        } else {
                            p.removeStatusEffect(StatusEffects.INVISIBILITY); //Remove invisible for all if disabled
                        }

                        if (player.isAccused() || player.isMarked()) {
                            p.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 1, false, false));
                        } else {
                            p.removeStatusEffect(StatusEffects.GLOWING);
                        }
                        if (currentGame.getRoleVisible()) {
                            if (player.getRole() != null) {
                                //Shows roles on player action bar
                                showActionBar(p, formatRoleName(player.getRole())); //Text.literal(player.getRole().getName())
                            }
                        }
                    }
                }
                if (currentGame.getStoryteller().getPlayer() != null & currentGame.getRoleVisible()){
                    showActionBar(currentGame.getStoryteller().getPlayer(), Text.literal("Storyteller"));
                }
                //Code for execution checks
                if (executionInProgress) {
                    boolean executionFinished = false;
                    BotcPlayer executeePlayer = currentGame.getPlayerBeingExecuted();
                    ServerPlayerEntity executee = executeePlayer.getPlayer();
                    Vec3d murderZone;
                    double distance;
                    if (executee != null) {
                        switch (mapSelected) {
                            case DEFAULT_MAP:
                                murderZone = new Vec3d(EXECUTE_POS.down(1).getX() + 0.5, EXECUTE_POS.down(1).getY() + 0.3, EXECUTE_POS.down(1).getZ() + 0.5); //Get centre of block
                                distance = executee.getPos().squaredDistanceTo(murderZone); //should work with vec3d now
                                if (distance > 2) {
                                    executee.teleport(murderZone.getX(), murderZone.getY(), murderZone.getZ(), false); //needs accurate tp
                                    executee.sendMessage(Text.literal("Nice try :)"), false);
                                }
                                BlockState anvilPosState = world.getBlockState(EXECUTE_POS);
                                if (anvilPosState.isOf(Blocks.ANVIL) || anvilPosState.isOf(Blocks.CHIPPED_ANVIL) || anvilPosState.isOf(Blocks.DAMAGED_ANVIL)) {
                                    world.setBlockState(EXECUTE_POS, Blocks.AIR.getDefaultState());
                                    //spawn redstone blood particles
                                    world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState()), EXECUTE_POS.getX() + 0.5, EXECUTE_POS.getY() - 0.2, EXECUTE_POS.getZ() + 0.5, 25, 0.3, 0.5, 0.3, 0.1);
                                    executionFinished = true;
                                }
                                break;
                            case SCHOOL_MAP:
                                if (executee.getBlockPos().getY() < -60) { //checks if played being executed is below certain y level.
                                    executionFinished = true;
                                    executee.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10, 1, false, false)); //Add slow fall for a moment
                                    tp(executee, mapCoords.get(executeePlayer.getColour()).chair);
                                    //Cover up hole
                                    world.setBlockState(new BlockPos(454, 3, 156), Blocks.RED_TERRACOTTA.getDefaultState());
                                    world.setBlockState(new BlockPos(455, 3, 156), Blocks.RED_TERRACOTTA.getDefaultState());
                                    world.setBlockState(new BlockPos(456, 3, 156), Blocks.RED_TERRACOTTA.getDefaultState());
                                    world.setBlockState(new BlockPos(454, 3, 157), Blocks.RED_TERRACOTTA.getDefaultState());
                                    world.setBlockState(new BlockPos(455, 3, 157), Blocks.RED_TERRACOTTA.getDefaultState());
                                    world.setBlockState(new BlockPos(456, 3, 157), Blocks.RED_TERRACOTTA.getDefaultState());
                                    world.setBlockState(new BlockPos(454, 3, 158), Blocks.RED_TERRACOTTA.getDefaultState());
                                    world.setBlockState(new BlockPos(455, 3, 158), Blocks.RED_TERRACOTTA.getDefaultState());
                                    world.setBlockState(new BlockPos(456, 3, 158), Blocks.RED_TERRACOTTA.getDefaultState());
                                    //also place all pit cover blocks back
                                }
                                break;
                            case WINTER_MAP:
                                murderZone = new Vec3d(EXECUTE_POS.getX() + 0.5, EXECUTE_POS.getY() + 0.3, EXECUTE_POS.getZ() + 0.5); //Get centre of block
                                distance = executee.getPos().squaredDistanceTo(murderZone); //should work with vec3d now
                                if (distance > 1.6) {
                                    executee.teleport(murderZone.getX(), murderZone.getY(), murderZone.getZ(), false); //needs accurate tp
                                    executee.sendMessage(Text.literal("Nice try :)"), false);
                                }
                                //BlockState dripPosState = world.getBlockState(EXECUTE_POS.up(1));
                                if (executee.getInventory().contains(new ItemStack(Items.POINTED_DRIPSTONE))) {
                                    executee.getInventory().removeStack(executee.getInventory().getSlotWithStack(new ItemStack(Items.POINTED_DRIPSTONE)));
                                    //Removes dripstone from player inv
                                    //TODO check does not work, need to use entities
                                    //spawn ice particles
                                    world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ICE.getDefaultState()), EXECUTE_POS.getX() + 0.5, EXECUTE_POS.getY() - 0.2, EXECUTE_POS.getZ() + 0.5, 40, 0.3, 1.5, 0.3, 0.1);
                                    world.setBlockState(EXECUTE_POS.up(20), Blocks.DRIPSTONE_BLOCK.getDefaultState()); //restore dripstone support
                                    world.setBlockState(EXECUTE_POS.up(19), Blocks.POINTED_DRIPSTONE.getDefaultState().with(Properties.VERTICAL_DIRECTION,Direction.DOWN)); //restore dripstone support
                                    world.setBlockState(EXECUTE_POS.up(18), Blocks.POINTED_DRIPSTONE.getDefaultState().with(Properties.VERTICAL_DIRECTION,Direction.DOWN)); //restore dripstone support
                                    world.setBlockState(EXECUTE_POS.up(17), Blocks.POINTED_DRIPSTONE.getDefaultState().with(Properties.VERTICAL_DIRECTION,Direction.DOWN)); //restore dripstone support
                                    world.setBlockState(EXECUTE_POS.up(16), Blocks.POINTED_DRIPSTONE.getDefaultState().with(Properties.VERTICAL_DIRECTION,Direction.DOWN)); //restore dripstone support
                                    //FallingBlockEntity.spawnFromBlock(world,EXECUTE_POS.up(16),Blocks.POINTED_DRIPSTONE.getDefaultState());
                                    executionFinished = true;
                                }
                                break;
                        }
                        if (executionFinished) { //code that overlaps for all maps
                            //sends message in chat for kill
                            sendMessageToPlayers(getEventText(executee, CURRENT_EXECUTEE), playerList); //Sends execution message to all onlinePlayers
                            if (!executeePlayer.surviveExecution()) {
                                //If player is not marked to survive execution
                                killPlayer(currentGame.findPlayer(executee));
                            } else {
                                //Code for surviving execution
                                Text noDeathMsg = switch (mapSelected) {
                                    case DEFAULT_MAP -> Text.literal("but they survived!");
                                    case SCHOOL_MAP -> Text.literal("but it was revoked!");
                                    case CIRCUS_MAP -> Text.literal("but they landed safely!");
                                    default -> Text.literal("but they didn't die!");
                                };
                                sendMessageToPlayers(noDeathMsg, playerList);
                            }
                            executeePlayer.changeExecutionStatus(false);
                            executionInProgress = false;
                        }
                    } else {
                        //No player is being executed
                        world.getServer().sendMessage(Text.literal("No pony is being executed, cancelling execution phase"));
                        executionInProgress = false;
                    }
                }
            }
        }
    }

    private void onServerTick(MinecraftServer srv){

        if (playersLockedToSeats && srv.getPlayerManager().getPlayerList() != null && currentGame != null) { //checks if onlinePlayers locked is true and onlinePlayers list is not null
            for (BotcPlayer player:currentGame.getPlayers()) {
                String playerColour = player.getColour();
                ServerPlayerEntity p = player.getPlayer();
                //BlockPos playerPos = p.getBlockPos();
                Vec3d playerPos = p.getPos();
                BlockPos targetPos;

                if (player.getExecutionStatus()) { //Check player is not currently being executed
                    targetPos = mapCoords.get(playerColour).chair.up(1);
                } else {
                    break;
                }

                //double distance = playerPos.getSquaredDistance(targetPos);
                double distance = playerPos.squaredDistanceTo(targetPos.getX()+0.5, targetPos.getY()+0.5, targetPos.getZ()+0.5); //accounts for slab of chair now
                if (distance > 1.5) {
                    tp(p,targetPos);
                    p.sendMessage(Text.literal("You were too far away. Teleporting to target..."), false);
                }
                if (organGrinderActive){
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, -1, 10, false, true));
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, -1, 10, false, true));
                }
            }
        }
    }

    private ActionResult onRightClickEntity(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult entityHitResult) {
        // Make sure the entity is a player (targeting another player)
        if (entity instanceof PlayerEntity target) {
            ItemStack itemInHand = player.getStackInHand(hand);

            // Check if the player is holding the specific item and Hand is not empty
            if (!itemInHand.isEmpty() && itemInHand.getItem() == Items.BREEZE_ROD) {
                if (target instanceof ServerPlayerEntity serverTarget) {
                    // Added selector tag to player
                    if (currentGame != null) {
                        BotcPlayer playerClass = currentGame.findPlayer(serverTarget);
                        if (playerClass != null) {
                            accusePlayer(playerClass, currentGame); //TODO seems to trigger twice? need to fix, non-urgent
                            player.sendMessage(Text.literal("You tagged ").append(target.getName()), false);
                            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.6F, 0.7F);
                        } else {
                            return ActionResult.FAIL;
                        }
                    } else {
                        return ActionResult.FAIL;
                    }
                }
                return ActionResult.SUCCESS; // Return success to stop further processing
            }
        }
        return ActionResult.PASS; // Pass if not right-clicking with the correct item or targeting a player
    }

    private ActionResult onRightClickItem(PlayerEntity playerE, World world, Hand hand) { //Checks if player right-clicked with paper, then sends player order
        ItemStack itemInHand = playerE.getStackInHand(hand);
        ServerPlayerEntity player = (ServerPlayerEntity) playerE;
        if (!itemInHand.isEmpty()) {
            // Check if the player is holding the specific item and Hand is not empty
            if (itemInHand.getItem() == Items.PAPER) {
                player.sendMessage(getPlayerOrder(currentGame), false);
                return ActionResult.SUCCESS;
            }
            //Check for heart pottery shard
            if (itemInHand.getItem() == Items.HEART_POTTERY_SHERD) {
                String colour = getColourFromPlayer(player);
                if (POSSIBLE_COLOURS.contains(colour)) {
                    tp(player, mapCoords.get(colour).homeInside);
                } else {
                    tp(player, SPAWN_POS);
                }
                return ActionResult.SUCCESS;
            }

            //Check for recovery compass
            if (itemInHand.getItem() == Items.RECOVERY_COMPASS) {
                gotoMinigames(player);
                return ActionResult.SUCCESS;
            }

            //Check for grimoire
            if (itemInHand.getItem() == Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE) {
                if (player.hasPermissionLevel(4)) {
                    SelectionInventory.openMenu(player);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("botc")
                .then(literal("setupGame")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> setupGame(context.getSource())))
//                .then(literal("presetupColours").then(
//                        CommandManager.argument("players", IntegerArgumentType.integer())
//                                .requires(source -> source.hasPermissionLevel(4))
//                                .executes(context -> {
//                                    int playerNum = IntegerArgumentType.getInteger(context, "players");
//                                    if (playerNum <= maxPlayers) {
//                                        //Runs initialise to change csv coordinates sheet and update other map coords
//                                        setupGame()
//                                    } else {
//                                        context.getSource().sendFeedback(() -> Text.literal("Too many players for map selected"), false);
//                                    }
//
//                                    return 1;
//                                })))
                .then(literal("startGame")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> beginGame(context.getSource())))
                .then(literal("importCSV")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes((context) -> {
                            mapCoords = ImportExcelCoordinates.read(getConfigFilePath()); //Import coordinates for map from Excel sheet
                            return 1;
                        }))
                .then(literal("changeMap").then(
                        CommandManager.argument("map", StringArgumentType.string())
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests((context, builder) -> {
                                    for (String m:MAPS)
                                        builder.suggest(m); //Suggest maps
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String map = StringArgumentType.getString(context, "map");

                                    if (MAPS.contains(map)) {
                                        mapSelected = map;
                                    } else {
                                        context.getSource().sendFeedback(() -> Text.literal("Invalid map specified, setting to default"), false);
                                        mapSelected = DEFAULT_MAP;
                                    }
                                    //Runs initialise to change csv coordinates sheet and update other map coords
                                    onGameInit(context.getSource().getServer());
                                    return 1;
                                })))
                .then(literal("add")
                        .then(CommandManager.argument("type", StringArgumentType.string())
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests((context, builder) -> {
                                    //for (String o:tpOptions)
                                    builder.suggest(SPEC); //Suggest types to add
                                    builder.suggest(PLAYER);
                                    builder.suggest(TRAVELLER);
                                    return builder.buildFuture();
                                })
                                .then(CommandManager.argument("player_name", StringArgumentType.string())
                                        .requires(source -> source.hasPermissionLevel(4))
                                        .suggests(new PlayerSuggestionProvider())
                                        .executes(context -> {
                                            String type = StringArgumentType.getString(context, "type");
                                            type = type.toLowerCase(); //lowercase to account for typos

                                            switch (type) {
                                                case SPEC:
                                                    onAddSpectator(context);
                                                    break;
                                                case PLAYER:
                                                    onAddPlayer(context);
                                                    break;
                                                case TRAVELLER:
                                                    onAddTraveller(context);
                                                    break;
                                                default:
                                                    context.getSource().sendFeedback(() -> Text.literal("Invalid team"), false);
                                            }
                                            return 1;
                                        }))))
                .then(literal("setRole")
                        .then(CommandManager.argument("colour", StringArgumentType.string())
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests((context, builder) -> {
                                    if (currentGame != null) {
                                        for (String c : currentGame.getColours()) {
                                            builder.suggest(c); //Adds all possible colours in game to suggestion
                                        }
                                    } else {
                                        //If there is no game initialized, send warning
                                        builder.suggest("NO GAME INITIALISED");
                                    }
                                    return builder.buildFuture();

                                })
                                .then(CommandManager.argument("role_id", StringArgumentType.string())
                                        .requires(source -> source.hasPermissionLevel(4))
                                        .suggests((context, builder) -> {
                                            //TODO Change to current selected script if applicable
                                            for (String id:rolesList.getAllRoleIDs()){
                                                builder.suggest(id); //Adds all possible role ids to suggestion
                                                //Might crash as there is a lot
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String colour = StringArgumentType.getString(context, "colour");
                                            colour = colour.toLowerCase(); //lowercase to account for typos
                                            String role_id = StringArgumentType.getString(context, "role_id");
                                            role_id = role_id.toLowerCase().replaceAll("\\s", ""); //lowercase to account for typos and removes spaces

                                            if (currentGame.getColours().contains(colour)){
                                                BotcPlayer player = currentGame.getPlayerAtColour(colour);
                                                if (player != null){
                                                    BotcRole role = rolesList.getRole(role_id);
                                                    if (role != null){
                                                        player.setRole(role); //Sets player role in game to input
                                                        //Sends output msg to command runner
                                                        context.getSource().sendFeedback(() -> Text.literal("Set " + player.getNameString() + "'s role to " + role.getName()), false);
                                                    } else {
                                                        context.getSource().sendFeedback(() -> Text.literal("Invalid role id"), false);
                                                    }
                                                } else {
                                                    context.getSource().sendFeedback(() -> Text.literal("Player not found at colour"), false);
                                                }

                                            } else {
                                                context.getSource().sendFeedback(() -> Text.literal("Invalid colour"), false);
                                            }
                                            return 1;
                                        }))))
                .then(literal("setPlayerColour")
                        .then(CommandManager.argument("colour", StringArgumentType.string())
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests((context, builder) -> {
                                    if (currentGame != null) {
                                        for (String c : currentGame.getColours()) {
                                            builder.suggest(c); //Adds all possible colours in game to suggestion
                                        }
                                    } else {
                                        //If there is no game initialized, send warning
                                        builder.suggest("NO GAME INITIALISED");
                                    }
                                    return builder.buildFuture();

                                })
                                .then(CommandManager.argument("player_name", StringArgumentType.string())
                                        .requires(source -> source.hasPermissionLevel(4))
                                        .suggests(new PlayerSuggestionProvider()) //Suggests online players
                                        .executes(context -> {
                                            String colour = StringArgumentType.getString(context, "colour");
                                            colour = colour.toLowerCase(); //lowercase to account for typos


                                            if (currentGame.getColours().contains(colour)){
                                                BotcPlayer player = currentGame.getPlayerAtColour(colour);
                                                if (player != null){
                                                    ServerPlayerEntity playerTarget = getPlayerFromSource(context);
                                                    if (playerTarget != null){
                                                        player.setPlayer(playerTarget); //Set player to position
                                                        //Sends output msg to command runner
                                                        String finalColour = colour;
                                                        context.getSource().sendFeedback(() -> Text.literal("Set " + playerTarget.getName() + " position to " + finalColour), false);
                                                    } else {
                                                        context.getSource().sendFeedback(() -> Text.literal("Invalid player specified"), false);
                                                    }
                                                } else {
                                                    context.getSource().sendFeedback(() -> Text.literal("Player not found at colour"), false);
                                                }

                                            } else {
                                                context.getSource().sendFeedback(() -> Text.literal("Invalid colour"), false);
                                            }
                                            return 1;
                                        }))))
                .then(literal("tpPlayers").then(
                        CommandManager.argument("tp_location", StringArgumentType.string())
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests((context, builder) -> {
                                    for (String o:tpOptions)
                                        builder.suggest(o); //Suggest teleport locations
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String option = StringArgumentType.getString(context, "tp_location");
                                    option = option.toLowerCase(); //lowercase to account for typos

                                    if (tpOptions.contains(option)) {
                                        teleportPlayers(option, context.getSource().getServer(),currentGame);
                                    } else {
                                        context.getSource().sendFeedback(() -> Text.literal("Invalid teleport location"), false);
                                    }
                                    return 1;
                                })))
                .then(literal("timer").then( //Starts a timer boss bar for [argument] minutes
                        CommandManager.argument("Time (minutes)", StringArgumentType.string())
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> {
                                    String stringTime = StringArgumentType.getString(context, "Time (minutes)");// Get the timer duration as string from argument
                                    ServerCommandSource src = context.getSource();
                                    MinecraftServer srv = src.getServer();
                                    float durationMins;
                                    int durationSecs;
                                    try {
                                        durationMins = Float.parseFloat(stringTime); //parse string value into float
                                    } catch (Exception e){
                                        durationMins = 0.1f; //default to 10seconds without arg
                                        src.sendFeedback(() -> Text.literal("Invalid or no duration supplied, defaulting to 10 second timer"), false);
                                    }
                                    durationSecs = Math.round(durationMins*60); //round to 2dp and convert to seconds
                                    src.sendFeedback(() -> Text.literal("Starting timer"), false);
                                    startTimer(srv,durationSecs);
                                    return 1;
                                })))
                .then(literal("execute").then(
                        CommandManager.argument("player", EntityArgumentType.player())
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests(new PlayerSuggestionProvider())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                                    executePlayer(currentGame.findPlayer(player));
                                    return 1;
                                })
                ))
                .then(literal("demonKillMark").then(
                        CommandManager.argument("player", EntityArgumentType.player()) //Command to mark player as the demon kill tonight.
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests(new PlayerSuggestionProvider())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                                    markPlayerDemonKill(currentGame.findPlayer(player));
                                    context.getSource().sendFeedback(() -> Text.literal("Marked player for demon kill: ").append(player.getStyledDisplayName()), false);
                                    return 1;
                                })
                ))
                .then(literal("reviveMark").then(
                        CommandManager.argument("player", EntityArgumentType.player()) //Command to mark player to be revived in the day.
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests(new PlayerSuggestionProvider())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                                    markPlayerRevived(currentGame.findPlayer(player));
                                    context.getSource().sendFeedback(() -> Text.literal("Marked player for revival upon morning: ").append(player.getStyledDisplayName()), false);
                                    return 1;
                                })
                ))
                .then(literal("accuse").then(
                        CommandManager.argument("player", EntityArgumentType.player()) // accuse player for execution, in case right click selector doesn't work.
                                .requires(source -> source.hasPermissionLevel(4))
                                .suggests(new PlayerSuggestionProvider())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                                    accusePlayer(currentGame.findPlayer(player),currentGame);
                                    context.getSource().sendFeedback(() -> Text.literal("Accused: ").append(player.getStyledDisplayName()), false);
                                    return 1;
                                })
                ))
                .then(literal("startDay")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> startDay(context.getSource())))
                .then(literal("nightFalls")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> nightFalls(context.getSource())))
                .then(literal("voteLockIn")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                                    LOGGER.info("Beginning vote lock in");
                                    voteLockIn(context.getSource());
                                    LOGGER.info("Completed /voteLockIn.");
                                    return 1;
                                }
                        ))
                .then(literal("toggleSeatLock")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            playersLockedToSeats = !playersLockedToSeats;
                            context.getSource().sendFeedback(() -> Text.literal("Toggled player seat lock to " + playersLockedToSeats), false);
                            return 1;
                        }))
                .then(literal("hideVoteToggle")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            currentGame.changeShowVoteResult(!currentGame.showVoteResult());
                            context.getSource().sendFeedback(() -> Text.literal("Show vote result set to " + currentGame.showVoteResult()), false);
                            return 1;
                        }))
                .then(literal("toggleInvis")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            currentGame.changePlayerInvis(!currentGame.invisPlayers()); //Sets invis to other state
                            context.getSource().sendFeedback(() -> Text.literal("Player invisibility set to " + currentGame.invisPlayers()), false);
                            return 1;
                        }))
                .then(literal("toggleShownRoles")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            currentGame.setRoleVisible(!currentGame.getRoleVisible()); //Sets role visibility to other state
                            context.getSource().sendFeedback(() -> Text.literal("Shown roles set to " + currentGame.getRoleVisible()), false);
                            return 1;
                        }))
                .then(literal("showPlayerOrder")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer(); //gets player running command
                            if (player != null) {
                                player.sendMessage(getPlayerOrder(currentGame));
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("No player to send text to, do not run in server console."), false);
                            }
                            return 1;
                        }))
                .then(literal("getStorytellerItems")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer(); //gets player running command
                            if (player != null) {
                                givePlayerStorytellerItems(player);
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("No player to give items, do not run in server console."), false);
                            }
                            return 1;
                        }))
                .then(literal("getGameItems")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer(); //gets player running command
                            if (player != null) {
                                givePlayerGameItems(player);
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("No player to give items, do not run in server console."), false);
                            }
                            return 1;
                        }))
                .then(literal("beginExecution")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> beginExecution(context.getSource())))
                .then(literal("openMenu").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player != null) {
                        SelectionInventory.openMenu(player);
                    }
                    return 1;
                }))
                .then(literal("getGameInfo") //Print info about current game into chat, roles players, etc...
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            if (currentGame != null){
                                context.getSource().sendFeedback(() -> currentGame.getGameInfo(), false);
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("No game to show info for"), false);
                            }
                            context.getSource().sendFeedback(() -> Text.literal("Put Game info here"), false);
                            return 1;
                        }))
                .then(literal("hat")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer(); //gets player running command
                            if (player != null) {
                                setPlayerHat(player); //Sets HEAD slot of player to item in main hand.
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("No player to set hat of, do not run in server console."), false);
                            }
                            return 1;
                        }))
                .then(literal("test")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            //ServerWorld world = context.getSource().getWorld();
                            context.getSource().sendFeedback(() -> Text.literal("colours in map: " + POSSIBLE_COLOURS), false);
                            if (currentGame != null) {
                                context.getSource().sendFeedback(() -> Text.literal("colours in use in game: " + currentGame.getColours()), false);
                            }
                            return 1;
                        }))
        );

        dispatcher.register(literal("leaveMinigames")
                .executes(PlayerUtils::leaveMinigames));

        dispatcher.register(literal("gotoMinigames")
                .executes(context -> gotoMinigames(context.getSource().getPlayer()))
        );
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");
        //Run on each world end tick
        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTick);
        TickScheduler.register();

        //Run on each server start tick
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        //Code that runs on server start.
        ServerLifecycleEvents.SERVER_STARTED.register(BotcFab::onGameInit);

        //Register events
        UseEntityCallback.EVENT.register(this::onRightClickEntity);
        UseItemCallback.EVENT.register(this::onRightClickItem);

        //Register chat commands
        CommandRegistrationCallback.EVENT.register(BotcFab::registerCommands);
    }
}