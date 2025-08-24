package com.botcfab;

import com.mojang.brigadier.CommandDispatcher;
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
import java.util.concurrent.ThreadLocalRandom;

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
    static final String ALIVE = "alive";
    static final String MARKED = "marked";
    static final String DEAD = "dead";
    static final String GHOST = "ghost";
    static final String DEATH_FLAG = "death_flag";
    static final String REVIVE_FLAG = "revive_flag";
    static final String ACCUSED = "accused";
    static final String CURRENT_EXECUTEE = "current_executee";
    static final String LEGION = "legion";
    static final List<String> ALL_TAGS = Arrays.asList(PLAYER,STORYTELLER,SPEC,ALIVE,MARKED,DEAD,GHOST,DEATH_FLAG,REVIVE_FLAG,ACCUSED,CURRENT_EXECUTEE, LEGION);

    static final String INFO_OBJECTIVE = "info";
    static final String ALIVE_SCORE_HOLDER = "Alive";
    static final String DEAD_SCORE_HOLDER = "Dead";
    static final String VOTE_SCORE_HOLDER = "Vote Threshold";
    static final List<String> POSSIBLE_COLOURS = Arrays.asList(
            "black",
            "yellow",
            "orange",
            "pink",
            "red",
            "purple",
            "brown",
            "green",
            "white",
            "blue",
            "cyan",
            "gray");

    //Huge penis of coordinates with ref, e.g. mapCoords.get("Yellow").ghost
    static Map<String, CoordinateMapper> mapCoords = new HashMap<>();
    static final ArrayList<Integer> indexBounds = new ArrayList<>();
    //Highest vote count
    static int highestVote;

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

    //Text for displays
    private static final MutableText MEETING_MESSAGE = Text.literal("\nPlease head to the town square");
    private static final MutableText RETURN_MESSAGE = Text.literal("\nPlease return to your houses");
    private static final MutableText DAY_MESSAGE = Text.literal("The sun rises ☀")
            .setStyle(Style.EMPTY.withColor(Formatting.GOLD));
    private static final MutableText NIGHT_MESSAGE = Text.literal("Night falls... 🌙")
            .setStyle(Style.EMPTY.withColor(Formatting.BLUE));
    private static final MutableText KILL_TEXT = Text.literal(" has been ") //Make these not final for school map, change to expelled, suspended etc
            .append(Text.literal("killed.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.DARK_RED))));
    private static final MutableText REVIVE_TEXT = Text.literal(" has been ")
            .append(Text.literal("revived.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW))));
    private static final MutableText EXECUTE_TEXT = Text.literal(" has been ")
            .append(Text.literal("executed.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED))));

    private static final MutableText message = Text.literal("the ")
            .append(Text.literal("cat").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55))))  // Yellow for "cat"
            .append(Text.literal(" is on "))
            .append(Text.literal("fire").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)))); // Red for "fire"

    //variables for command inputs
    private static final List<String> tpOptions = Arrays.asList("home","house","vote","chair","town","dorm","legion","evil");
    final static String DEFAULT_MAP = "default";
    final static String SCHOOL_MAP = "school";
    final static List<String> MAPS = Arrays.asList(DEFAULT_MAP,SCHOOL_MAP);
    static String mapSelected = DEFAULT_MAP; //Defaults to clocktower map

    //BLOCK POS FOR CLOCKTOWER MAP
    static BlockPos EXECUTE_POS;
    static BlockPos EVIL_ROOM_POS;
    static BlockPos MINIGAMES_POS;

    public static Path getFolderPath() {
        Path configDir = FabricLoader.getInstance().getConfigDir();

        // Make your mod's subfolder (recommended)
        Path modFolder = configDir.resolve("botc-fab"); // "config/"string"
        File folder = modFolder.toFile();
        if (!folder.exists()) {
            if (folder.mkdirs()) LOGGER.info("Created config"); // create folder if it doesn't exist
        }

        return modFolder;
    }

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
        //TODO Add code for both maps here: returns different csv
        return switch (mapSelected) {
            case DEFAULT_MAP -> modFolder.resolve("BOTC-coords-sheet.csv").toFile();
            case SCHOOL_MAP -> modFolder.resolve("school-BOTC-coords-sheet.csv").toFile();
            default -> null;
        };
    }

    private static void onGameInit(MinecraftServer srv) { //Run this on server startup, players do not need to be connected
        ServerCommandSource src = srv.getCommandSource();
        ServerScoreboard scoreboard = srv.getScoreboard();
        ServerWorld world = src.getWorld();
        mapCoords = ImportExcelCoordinates.read(getConfigFilePath()); //Import csv file here

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
        world.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE).set(200, world.getServer());

        // Create the one and only team we need
        createTeam(scoreboard);

        //Sets the coordinates missing from sheet per map
        switch (mapSelected) {
            case DEFAULT_MAP:
                EXECUTE_POS = new BlockPos(6, -29, -2);
                EVIL_ROOM_POS = new BlockPos(126, -28, 57);
                MINIGAMES_POS = new BlockPos(-120, -27, -13);
                break;
            case SCHOOL_MAP:
                //TODO Change these one map is done
                EXECUTE_POS = new BlockPos(1, -29, -2);
                EVIL_ROOM_POS = new BlockPos(1, -28, 57);
                MINIGAMES_POS = new BlockPos(1, -27, -13);
                break;
        }

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
        ArrayList<ServerPlayerEntity> players = new ArrayList<>(playerMgr.getPlayerList());
        Collections.shuffle(players); //Randomises order of player list (default is server join order)
        indexBounds.clear();
        int startPoint = ThreadLocalRandom.current().nextInt(0, (11+1)); //Determines start point for colour selection
        ServerPlayerEntity storyTeller = src.getPlayer(); // Gets the person that called the command. Whoever called it is Storyteller

        //TODO add list of barrel coordinates to csv and copt contents to each one

        if (storyTeller != null) {
            // Remove all tags before adding new ones
            players.remove(storyTeller);
            resetPlayer(storyTeller);
            storyTeller.getInventory().setStack(36,ItemStack.EMPTY); //Remove boots
            storyTeller.addCommandTag(STORYTELLER); //Add tag for storyteller
            srv.sendMessage(Text.literal("Storyteller is: " + storyTeller.getStyledDisplayName().toString()));
            storyTeller.changeGameMode(GameMode.CREATIVE);
            scoreboard.addScoreHolderToTeam(storyTeller.getNameForScoreboard(), scoreboard.getTeam(TEAM_ALL));
            src.sendFeedback(() -> message, false);
        } else {
            src.sendFeedback(() -> Text.literal("Failed to find storyteller. Do not execute this command from the server window"), false);
            return 0;
        }

        //List<ServerPlayerEntity> spectators = new LinkedList<>();
        for (ServerPlayerEntity p : players){
            Set<String> tags = p.getCommandTags();
            if (tags.contains(SPEC)) {
                p.changeGameMode(GameMode.SPECTATOR);
                players.remove(p); //Remove spectators from player list
                scoreboard.addScoreHolderToTeam(p.getNameForScoreboard(), scoreboard.getTeam(TEAM_ALL));
            }
        }

        storyTeller.setSpawnPoint(World.OVERWORLD, EXECUTE_POS,0,true,true); //Should work now?

        // Assign colours to players
        int currentColourIndex = startPoint;
        if (players.isEmpty()) {
            src.sendFeedback(() -> Text.literal("No pony online :("), false);
        } else {
            for (ServerPlayerEntity player : players) {
                resetPlayer(player);
                player.getInventory().setStack(36,ItemStack.EMPTY); //Remove boots
                player.addCommandTag(PLAYER);
                player.addCommandTag(ALIVE);
                player.changeGameMode(GameMode.ADVENTURE);
                //playerMgr.removeFromOperators(player.getGameProfile()); //Don't auto remove from ops yet for testing

                //Assign Colours HERE
                String assignedColour = POSSIBLE_COLOURS.get(currentColourIndex);
                player.addCommandTag(assignedColour); //Add colour tag to player
                scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), scoreboard.getTeam(TEAM_ALL)); //Add player to all player team
                setColourBoots(player,assignedColour);
                indexBounds.add(POSSIBLE_COLOURS.indexOf(assignedColour)); //Used for vote lock in to decide bounds.
                //TODO Test this to confirm, should work now tho
                player.setSpawnPoint(World.OVERWORLD,mapCoords.get(assignedColour).homeInside,0,true,true); //Set player spawn point
                //Places signs, levers update in the updatePlayer function
                switch (mapSelected) {
                    case DEFAULT_MAP:
                        switch (assignedColour) {
                            case "black", "yellow", "orange":
                                placeSign(world, mapCoords.get(assignedColour).sign, Direction.SOUTH, player.getName(), assignedColour);
                                break;
                            case "pink", "red", "purple":
                                placeSign(world, mapCoords.get(assignedColour).sign, Direction.WEST, player.getName(), assignedColour);
                                break;
                            case "brown", "green", "white":
                                placeSign(world, mapCoords.get(assignedColour).sign, Direction.NORTH, player.getName(), assignedColour);
                                break;
                            case "blue", "cyan", "gray":
                                placeSign(world, mapCoords.get(assignedColour).sign, Direction.EAST, player.getName(), assignedColour);
                                break;
                        }
                        break;
                    case SCHOOL_MAP:
                        placeSign(world, mapCoords.get(assignedColour).sign, Direction.SOUTH, player.getName(), assignedColour);
                        break;
                }
                updateVoteStatus(world,player); //player levers and update lamps

                if (currentColourIndex == 11)
                    currentColourIndex = 0;
                else
                    currentColourIndex++;
            }
        }
        createOrSetAliveDisplay(scoreboard, srv); //Update/create scoreboard for start of game.
        srv.sendMessage(Text.literal(players.toString()));
        srv.sendMessage(Text.literal(indexBounds.toString()));

        //Remove votes and signs for missing players
        for (String c:POSSIBLE_COLOURS){
            if (getPlayerFromColour(c,srv) == null){
                world.setBlockState(mapCoords.get(c).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState()); //Set player indicator to netherite
                world.setBlockState(mapCoords.get(c).lampsVoteMarker, Blocks.COAL_BLOCK.getDefaultState()); //Disable ghost vote after use by setting to coal
                world.setBlockState(mapCoords.get(c).lever, Blocks.AIR.getDefaultState()); //Remove lever
                world.setBlockState(mapCoords.get(c).sign, Blocks.AIR.getDefaultState()); //Remove empty signs
            }
        }

        Grimoire grimoire = new Grimoire(players);
        grimoire.randomRolesAssign(players, null);
        return 1;
    }

    static int beginGame(ServerCommandSource src) {
        //TODO
        // - give roles to players on named paper
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();

        for (ServerPlayerEntity p: playerMgr.getPlayerList()){ //Give players writable book
            if (p.getCommandTags().contains(PLAYER))
                p.getInventory().insertStack( new ItemStack(Items.WRITABLE_BOOK));
        }
        teleportPlayers("home", srv); //teleports players to homes
        sendMessageToPlayers(getPlayerOrder(srv),playerMgr.getPlayerList()); //Sends player order to all players
        nightFalls(src);

        return 1;
    }

    static int nightFalls(ServerCommandSource src){
        MinecraftServer srv = src.getServer();
        ServerWorld world = src.getWorld();
        PlayerManager playerMgr = srv.getPlayerManager();
        ServerScoreboard scoreboard = world.getScoreboard();
        world.setTimeOfDay(18000L);
        playersLockedToSeats = false;
        executionInProgress = false;
        timerBarActive = false; //Clear timer for day end
        //Remove all temp tags from players
        removeTagAllPlayers(ACCUSED,srv);
        removeTagAllPlayers(DEATH_FLAG, srv);
        removeTagAllPlayers(REVIVE_FLAG, srv);
        removeTagAllPlayers(CURRENT_EXECUTEE, srv);
        removeTagAllPlayers(MARKED, srv);
        Team team = scoreboard.getTeam(TEAM_ALL);
        if (team != null) { //Make nametags visible at morning.
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
        }
        sendMessageToPlayers(NIGHT_MESSAGE.copy().append(RETURN_MESSAGE),playerMgr.getPlayerList()); //Send msg to all players
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
        int deaths = getTagCount(DEATH_FLAG, srv);
        for (int i = 0;i < deaths;i++){
            ServerPlayerEntity deadPlayer = getPlayerFromColour(DEATH_FLAG,srv);
            if (deadPlayer != null) {
                sendMessageToPlayers(getEventText(deadPlayer,DEATH_FLAG) ,playerList);
                killPlayer(deadPlayer);
                deadPlayer.removeCommandTag(DEATH_FLAG);
            }
        }
        int revives = getTagCount(REVIVE_FLAG, srv);
        for (int i = 0;i < revives;i++){
            ServerPlayerEntity alivePlayer = getPlayerFromColour(REVIVE_FLAG,srv);
            if (alivePlayer != null) {
                sendMessageToPlayers(getEventText(alivePlayer,REVIVE_FLAG) ,playerList);
                revivePlayer(alivePlayer);
                alivePlayer.removeCommandTag(REVIVE_FLAG);
            }
        }
        for (ServerPlayerEntity p : playerList){
            showTitle(p,DAY_MESSAGE);
            //p.playSound(SoundEvents.BLOCK_BELL_USE);
            p.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE,SoundCategory.BLOCKS,1.0f,0.5f);
        }
        highestVote = 0; //reset highest vote

        createOrSetAliveDisplay(scoreboard, srv); //Updates scoreboard
        return 1;
    }

    static void voteLockIn(ServerCommandSource src) {
        // remove redstone block
        MinecraftServer srv = src.getServer();
        ServerWorld world = src.getWorld();
        int delayPerBlock = 40; // 1 second = 20 ticks
        int voteThreshold, startColourIndex, count = 1, alivePlayers = getTagCount(ALIVE, srv), playerTotal = getTagCount(PLAYER, srv);

        List<DelayedBlockSetter> taskList = new ArrayList<>();
        //Get vote threshold for alive players
        voteThreshold = (alivePlayers / 2) + (alivePlayers % 2);

        final ServerPlayerEntity accusedPlayer = getPlayerFromColour(ACCUSED, srv);
        if (accusedPlayer == null){
            src.sendFeedback(() -> Text.literal("No player accused, please accuse someone!"), false);
            return;
        }
        String accusedColour = getColourFromPlayer(accusedPlayer);

        int accusedIndex = POSSIBLE_COLOURS.indexOf(accusedColour);
        startColourIndex = accusedIndex+1; //+1 to start from player after accused

        if (startColourIndex > 11){
            startColourIndex = 0; //sets start to 0 if > 11 then checks if this is within bounds
        }
        if (!indexBounds.contains(startColourIndex)){
            startColourIndex = indexBounds.get(0); //Sets index to start of colour index bounds
        }

        for (int i = startColourIndex; count <= playerTotal+1; i++) { //need to be <= or else the accused doesn't get a vote (+1 cus it starts at 1 to have initial delay
            int delay = count * delayPerBlock; //Add delay between vote locks
            if (i > 11){
                i = 0;
            }
            if (!indexBounds.contains(i)){
                i = indexBounds.get(0); //Sets index to start of colour index bounds
            }
            BlockPos redstoneBlock = mapCoords.get(POSSIBLE_COLOURS.get(i)).triggersLampPiston;
            taskList.add(new DelayedBlockSetter(world, redstoneBlock, Blocks.AIR.getDefaultState(), delay)); //Set redstone block to air
            count++;
        }

        int finalStartColourIndex = startColourIndex;
        Runnable onAllDone = () -> {
            src.sendFeedback(() -> Text.literal("Starting count..."), false);
            List<ServerPlayerEntity> playerList = src.getWorld().getPlayers();
            int playerCount = 0, aliveVotesTotal = 0, ghostVotesTotal = 0;
            int playerSize = getTagCount(PLAYER, srv);

            for (int i = finalStartColourIndex; playerCount < playerSize; i++) {
                if (i > 11){
                    i = 0;
                }
                if (!indexBounds.contains(i)){
                    i = indexBounds.get(0); //Sets index to start of colour index bounds
                }
                String colour = POSSIBLE_COLOURS.get(i);
                BlockPos pos = mapCoords.get(colour).triggersLampPiston;
                BlockPos lockedVote;
                switch (mapSelected) {
                    case DEFAULT_MAP:
                        lockedVote = pos.up(2); //Position of locked in vote lamp
                        break;
                    case SCHOOL_MAP:
                        lockedVote = pos.east(2); //Position of locked in vote lamp Change direction based on orientation
                        src.sendFeedback(() -> Text.literal("FUCK YOU IDE"), false);
                        break;
                    default:
                        lockedVote = pos.up(2);
                }

                BlockState lockedVoteState = world.getBlockState(lockedVote);
                if (lockedVoteState.getBlock() == Blocks.WAXED_COPPER_BULB && lockedVoteState.get(Properties.LIT)) {
                    aliveVotesTotal++;
                } else if (lockedVoteState.getBlock() == Blocks.SEA_LANTERN) {
                    ghostVotesTotal++;
                    // remove ghost vote and tag from player
                    world.setBlockState(lockedVote,Blocks.COAL_BLOCK.getDefaultState());
                    ServerPlayerEntity playerVote = getPlayerFromColour(colour,srv);
                    if (playerVote != null) {
                        playerVote.addCommandTag(DEAD);
                        playerVote.removeCommandTag(GHOST);
                    }
                }
                world.setBlockState(pos, Blocks.REDSTONE_BLOCK.getDefaultState()); //Set back redstone blocks

                playerCount++;
            }
            int displayTotalVotes = aliveVotesTotal + ghostVotesTotal; //total votes
            int displayGhostVotes = ghostVotesTotal; //ghost votes used
            ServerPlayerEntity markedPlayer = getPlayerFromColour(MARKED,srv);

            sendMessageToPlayers(Text.literal("A total of " + displayTotalVotes + " votes were received, including " + displayGhostVotes + " ghost votes."),playerList);
            if ((displayTotalVotes > highestVote) && (displayTotalVotes >= voteThreshold)){ //On beating highest vote and vote threshold mark accused player and remove old.
                highestVote = displayTotalVotes; //Change the highest vote to this vote
                //Mark player for execution
                accusedPlayer.addCommandTag(MARKED);
                if (markedPlayer != null) { //Remove any previous marked players
                    markedPlayer.removeCommandTag(MARKED);
                }
                src.sendFeedback(accusedPlayer::getStyledDisplayName, false);

                Text name = accusedPlayer.getStyledDisplayName();
                MutableText message = name.copy();
                message.append(" has now been marked for execution");
                sendMessageToPlayers(message, playerList);
            }
            if (displayTotalVotes == highestVote){ //On matching highest vote, remove all marked players
                if (markedPlayer != null) {
                    markedPlayer.removeCommandTag(MARKED);
                }
            }
            accusedPlayer.removeCommandTag(ACCUSED);
        };
        TickScheduler.scheduleGroup(taskList, onAllDone);
    }

    static int beginExecution(ServerCommandSource src){
        MinecraftServer srv = src.getServer();
        ServerPlayerEntity player = getPlayerFromColour(MARKED,srv);
        removeTagAllPlayers(ACCUSED,srv); //Removed all accused players
        if (player != null) {
            player.removeCommandTag(MARKED);
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
        team.setShowFriendlyInvisibles(true); //makes invisible players visible
    }

    static void createOrSetAliveDisplay(ServerScoreboard scoreboard, MinecraftServer srv){
        int alivePlayers = getTagCount(ALIVE, srv);
        int deadPlayers = getTagCount(GHOST, srv) + getTagCount(DEAD, srv);
        int voteThreshold = (alivePlayers / 2) + (alivePlayers % 2);
        ScoreboardObjective objective = scoreboard.getNullableObjective(INFO_OBJECTIVE);
        NumberFormat numberFormat = StyledNumberFormat.YELLOW; //Can be RED, YELLOW or EMPTY
        if (objective == null){
            objective = scoreboard.addObjective(
                    INFO_OBJECTIVE, // Objective name (unique id)
                    ScoreboardCriterion.DUMMY, // Criterion type (dummy = manual numbers)
                    Text.literal("Player Info"), // Display name (shown in sidebar, below name, etc.)
                    ScoreboardCriterion.RenderType.INTEGER, // Render type (number type)
                    true, //Whether the value updates live
                    numberFormat //Format of numbers, colour and display
            );
        }
        ScoreAccess aliveScoreAccess = scoreboard.getOrCreateScore(ScoreHolder.fromName(ALIVE_SCORE_HOLDER),objective);
        ScoreAccess deadScoreAccess = scoreboard.getOrCreateScore(ScoreHolder.fromName(DEAD_SCORE_HOLDER),objective);
        ScoreAccess voteScoreAccess = scoreboard.getOrCreateScore(ScoreHolder.fromName(VOTE_SCORE_HOLDER),objective);
        aliveScoreAccess.setDisplayText(Text.literal("Alive "));
        aliveScoreAccess.setScore(alivePlayers);
        deadScoreAccess.setDisplayText(Text.literal("Dead "));
        deadScoreAccess.setScore(deadPlayers);
        voteScoreAccess.setDisplayText(Text.literal("Vote Threshold "));
        voteScoreAccess.setScore(voteThreshold);
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR,objective);
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

        srv.sendMessage(Text.literal("Boss bar timer started."));
    }

    private void onWorldTick(ServerWorld world) {
        List<ServerPlayerEntity> playerList = world.getServer().getPlayerManager().getPlayerList(); //gets all players every tick
        for (String i : mapCoords.keySet()) {
            BlockPos leverPos = mapCoords.get(i).lever; // Get the block state at that position
            BlockState leverState = world.getBlockState(leverPos); // Get the block state at that position
            // Check if lever powered (true = powered)
            if (leverState.isOf(Blocks.LEVER)){
                BlockPos voteLampPos = mapCoords.get(i).lampsVoteMarker;
                BlockState voteLampState = world.getBlockState(mapCoords.get(i).lampsVoteMarker);
                //Update lamp state based on lever state
                if (voteLampState.contains(Properties.LIT)){
                    BlockState updatedBulbState = voteLampState.with(Properties.LIT, leverState.get(Properties.POWERED));
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
        }

        if (timerBarActive) { //Timer bar for talk time
            timerBarTicks++;
            float percent = (float) timerBarTicks / timerDurationTicks;
            float progress = 1.0f - percent;
            if (timerBar != null) {
                timerBar.setPercent(Math.max(progress, 0f));
                //day lasts ~12000 ticks, advances time to fill that during the day
                if (discussionTime) {
                    world.setTimeOfDay(Math.round(1000 + (11000*percent)));
                    if (progress < 0.5f && sendHalfwayMessage){
                        sendHalfwayMessage = false;
                        sendMessageToPlayers(Text.literal("Your time is halfway through, 3 minutes remaining."),playerList);
                    }
                }
                if (progress < 0.5f && progress > 0.3f){
                    timerBar.setColor(BossBar.Color.YELLOW);
                }
                if (progress < 0.1f && progress > 0f){
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

        if (playerList != null) {
            //Code for player death particles and particle effects
            for (ServerPlayerEntity p : playerList) { //uses full server list to avoid calling null
                Set<String> tags = p.getCommandTags();
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 1, false, false));
                //TODO Change this add to coloured particles? maybe a beacon highlight in vote phase.
                if (tags.contains(DEAD) || tags.contains(GHOST) || (tags.contains(STORYTELLER))) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 1, false, false));
                    if (tags.contains(SPEC)) {
                        world.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY(), p.getZ(), 1, 0.4, 0, 0.4, 0.001);
                    }
                }
                if (tags.contains(ALIVE)) {
                    p.removeStatusEffect(StatusEffects.INVISIBILITY); //Remove invisible for alive players
                }

                if (tags.contains(ACCUSED) || tags.contains(MARKED)) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 1, false, false));
                } else {
                    p.removeStatusEffect(StatusEffects.GLOWING);
                }
            }
            //Code for execution checks
            if (executionInProgress) {
                boolean executionFinished = false;
                ServerPlayerEntity executee = getPlayerFromColour(CURRENT_EXECUTEE,world.getServer());
                if (executee != null){
                    switch (mapSelected) {
                        case DEFAULT_MAP:
                            Vec3d murderZone = new Vec3d(EXECUTE_POS.down(1).getX()+0.5, EXECUTE_POS.down(1).getY()+0.3, EXECUTE_POS.down(1).getZ()+0.5); //Get centre of block
                            double distance = executee.getPos().squaredDistanceTo(murderZone); //should work with vec3d now
                            if (distance > 1.2) {
                                executee.teleport(murderZone.getX(),murderZone.getY(),murderZone.getZ(),false); //needs accurate tp
                                executee.sendMessage(Text.literal("Nice try :)"), false);
                            }

                            BlockState anvilPosState = world.getBlockState(EXECUTE_POS);
                            if (anvilPosState.isOf(Blocks.ANVIL) || anvilPosState.isOf(Blocks.CHIPPED_ANVIL) || anvilPosState.isOf(Blocks.DAMAGED_ANVIL)) {
                                world.setBlockState(EXECUTE_POS, Blocks.AIR.getDefaultState());
                                executionFinished = true; //used so
                            }
                            break;
                        case SCHOOL_MAP:
                            if (executee.getBlockPos().getY() < -30) { //checks if played being executed is below certain y level.
                                executionFinished = true;
                                //also place all pit cover blocks back
                            }
                            break;
                    }
                    if (executionFinished) { //code that overlaps for all maps
                        killPlayer(executee);
                        //spawn redstone blood particles
                        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState()), EXECUTE_POS.getX(), EXECUTE_POS.getY(), EXECUTE_POS.getZ(), 15, 0.5, 0, 0.5, 0.5);
                        //sends message in chat for kill
                        world.getServer().sendMessage(getEventText(executee,CURRENT_EXECUTEE));
                        sendMessageToPlayers(getEventText(executee,CURRENT_EXECUTEE), playerList); //Sends execution message to all players
                        executee.removeCommandTag(CURRENT_EXECUTEE);
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

    private void onServerTick(MinecraftServer srv){

        if (playersLockedToSeats && srv.getPlayerManager().getPlayerList() != null) { //checks if players locked is true and players list is not null
            for (ServerPlayerEntity p:srv.getPlayerManager().getPlayerList()) {
                Set<String> tags = p.getCommandTags();
                if (!tags.contains(STORYTELLER) || !tags.contains(SPEC)){
                    String playerColour = getColourFromPlayer(p);
                    //BlockPos playerPos = p.getBlockPos();
                    Vec3d playerPos = p.getPos();
                    BlockPos targetPos;

                    if (!tags.contains(CURRENT_EXECUTEE)) { //Check player is not currently being executed
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
                    accusePlayer(serverTarget); //TODO seems to trigger twice? need to fix, non-urgent
                    player.sendMessage(Text.literal("You tagged ").append(target.getName()), false);
                    world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.6F, 0.7F);
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
                player.sendMessage(getPlayerOrder(Objects.requireNonNull(world.getServer())), false);
                return ActionResult.SUCCESS;
            }
            //Check for heart pottery shard
            if (itemInHand.getItem() == Items.HEART_POTTERY_SHERD) {
                String colour = getColourFromPlayer(player);
                tp(player, mapCoords.get(colour).homeInside);
                return ActionResult.SUCCESS;
            }

            //Check for recovery compass
            if (itemInHand.getItem() == Items.RECOVERY_COMPASS) {
                gotoMinigames(player);
                return ActionResult.SUCCESS;
            }

            //Check for grimoire
            if (itemInHand.getItem() == Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE) {
                SelectionInventory.openMenu(player);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("botc_setupGame")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> setupGame(context.getSource())));

        dispatcher.register(literal("botc_startGame")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> beginGame(context.getSource())));

        dispatcher.register(literal("botc_importCSV")
                .requires(source -> source.hasPermissionLevel(2))
                .executes((context) -> {
                    mapCoords = ImportExcelCoordinates.read(getConfigFilePath()); //Import coordinates for map from Excel sheet
                    return 1;
                }));

        dispatcher.register(literal("botc_changeMap").then(
                CommandManager.argument("map", StringArgumentType.string())
                        .requires(source -> source.hasPermissionLevel(2))
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
                        })
        ));

        dispatcher.register(literal("botc_addSpectator").then(
                CommandManager.argument("player_name", StringArgumentType.string())
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(PlayerUtils::onAddSpectator)
        ));

        dispatcher.register(literal("botc_tpPlayers").then(
                CommandManager.argument("tp_location", StringArgumentType.string())
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests((context, builder) -> {
                            for (String o:tpOptions)
                                builder.suggest(o); //Suggest teleport locations
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String option = StringArgumentType.getString(context, "tp_location");
                            option = option.toLowerCase(); //lowercase to account for typos

                            if (tpOptions.contains(option)) {
                                teleportPlayers(option, context.getSource().getServer());
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("Invalid teleport location"), false);
                            }
                            return 1;
                        })
        ));

        dispatcher.register(literal("leaveMinigames")
                .executes(PlayerUtils::leaveMinigames));

        dispatcher.register(literal("gotoMinigames")
                .executes(context -> PlayerUtils.gotoMinigames(context.getSource().getPlayer()))
        );

        dispatcher.register(literal("botc_startTimer").then( //Starts a timer boss bar for [argument] minutes
                CommandManager.argument("Time (minutes)", StringArgumentType.string())
                        .requires(source -> source.hasPermissionLevel(2))
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
                        })
        ));

        dispatcher.register(literal("botc_executePlayer").then(
                CommandManager.argument("player", EntityArgumentType.player())
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            executePlayer(player);
                            return 1;
                        })
        ));

        dispatcher.register(literal("botc_demonKillMark").then(
                CommandManager.argument("player", EntityArgumentType.player()) //Command to mark player as the demon kill tonight.
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            markPlayerDemonKill(player);
                            context.getSource().sendFeedback(() -> Text.literal("Marked player for demon kill: ").append(player.getStyledDisplayName()), false);
                            return 1;
                        })
        ));

        dispatcher.register(literal("botc_reviveMark").then(
                CommandManager.argument("player", EntityArgumentType.player()) //Command to mark player to be revived in the day.
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            markPlayerRevived(player);
                            context.getSource().sendFeedback(() -> Text.literal("Marked player for revival upon morning: ").append(player.getStyledDisplayName()), false);
                            return 1;
                        })
        ));

        dispatcher.register(literal("botc_accuse").then(
                CommandManager.argument("player", EntityArgumentType.player()) // accuse player for execution, in case right click selector doesn't work.
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            accusePlayer(player);
                            context.getSource().sendFeedback(() -> Text.literal("Accused: ").append(player.getStyledDisplayName()), false);
                            return 1;
                        })
        ));

        dispatcher.register(literal("botc_startDay")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> startDay(context.getSource())));
        dispatcher.register(literal("botc_nightFalls")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> nightFalls(context.getSource())));

        dispatcher.register(literal("botc_voteLockIn")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                            LOGGER.info("Beginning vote lock in");
                            voteLockIn(context.getSource());
                            LOGGER.info("Completed /voteLockIn.");
                            return 1;
                        }
                ));

        dispatcher.register(literal("botc_toggleSeatLock")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                            playersLockedToSeats = !playersLockedToSeats;
                            context.getSource().sendFeedback(() -> Text.literal("Toggled player seat lock to " + playersLockedToSeats), false);
                            return 1;
                        }
                ));

        dispatcher.register(literal("botc_showPlayerOrder")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer(); //gets player running command
                    if (player != null) {
                        player.sendMessage(getPlayerOrder(context.getSource().getServer()));
                    } else {
                        context.getSource().sendFeedback(() -> Text.literal("No player to send text to, do not run in server console."), false);
                    }
                    return 1;
                }));

        dispatcher.register(literal("botc_getItems")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer(); //gets player running command
                    if (player != null) {
                        givePlayerStorytellerItems(player);
                    } else {
                        context.getSource().sendFeedback(() -> Text.literal("No player to give items, do not run in server console."), false);
                    }
                    return 1;
                }));

        dispatcher.register(literal("botc_beginExecution")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> beginExecution(context.getSource())));

        dispatcher.register(literal("botc_openMenu").executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player != null) {
                SelectionInventory.openMenu(player);
            }
            return 1;
        }));

        dispatcher.register(literal("botc_grimoireTest").executes(context -> {
            GrimoireTest grimoireTest = new GrimoireTest();
            grimoireTest.randomRolesAssign("trouble brewing");
            return 1;
        }));
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");
        //Run on each world end tick
        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTick); //(ServerWorld world) -> onWorldTick(world)
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