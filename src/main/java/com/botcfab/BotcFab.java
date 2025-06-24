package com.botcfab;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
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

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BotcFab implements ModInitializer {
    public static final String MOD_ID = "botc-fab";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    //Removed all other teams, screw coloured names
    private static final String TEAM_ALL = "team_all";
    //Variable for tag definitions
    private static final String STORYTELLER = "storyteller";
    private static final String PLAYER = "player";
    private static final String SPEC = "spectator";
    private static final String ALIVE = "alive";
    private static final String MARKED = "marked";
    private static final String DEAD = "dead";
    private static final String GHOST = "ghost";
    private static final String DEATH_FLAG = "death_flag";
    private static final String REVIVE_FLAG = "revive_flag";
    private static final String ACCUSED = "accused";
    private static final String CURRENT_EXECUTEE = "current_executee";
    private static final List<String> ALL_TAGS = Arrays.asList(STORYTELLER, SPEC,ALIVE,MARKED,DEAD,GHOST,DEATH_FLAG,REVIVE_FLAG,ACCUSED,CURRENT_EXECUTEE);

    private static final String INFO_OBJECTIVE = "info";
    private static final String ALIVE_SCORE_HOLDER = "Alive";
    private static final String DEAD_SCORE_HOLDER = "Dead";
    private static final String VOTE_SCORE_HOLDER = "Vote Threshold";
    private static final List<String> POSSIBLE_COLOURS = Arrays.asList(
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
    private static final List<Integer> COLOUR_HEX = Arrays.asList(0x000000, 0xFFEE00, 0xFF8D00, 0xFFAFC7, 0xE50000,
            0x760088, 0x613915, 0x028121, 0xFFFFFF, 0x004CFF, 0x73D7EE, 0x888888); //Colour picked from progress pride flag
    private final String path = ".\\BOTC-coords-sheet.csv"; //Attempt to give standard file path, works in "run" folder
    //private final String path = "C:\\Users\\Ruby\\IdeaProjects\\botc-fabric-copytest\\BOTC-coords-sheet.csv"; //Absolute file path

    //Huge penis of coordinates with ref, e.g. mapCoords.get("Yellow").ghost
    Map<String, CoordinateMapper> mapCoords = new HashMap<>();
    private List<ServerPlayerEntity> players = new ArrayList<>();
    private ArrayList<Integer> indexBounds = new ArrayList<>();
    //Highest vote count
    private int highestVote;

    //Timer bar variables
    private ServerBossBar timerBar;
    private boolean timerBarActive = false;
    private int timerBarTicks = 0;
    private int timerDurationTicks = 10*20; // 10 seconds default

    //Variables for on tick checks
    private boolean playersLockedToSeats = false;
    private boolean executionInProgress = false;

    //Text for displays
    private final MutableText MEETING_MESSAGE = Text.literal("Please head to the town square");
    private final MutableText RETURN_MESSAGE = Text.literal("Please return to your houses");
    private final MutableText DAY_MESSAGE = Text.literal("The sun rises ☀")
            .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
            .append(Text.literal("\n"));
    private final MutableText NIGHT_MESSAGE = Text.literal("Night falls... 🌙")
            .setStyle(Style.EMPTY.withColor(Formatting.BLUE))
            .append(Text.literal("\n"));
    private final MutableText KILL_TEXT = Text.literal(" has been ") //Make these not final for school map, change to expelled, suspended etc
            .append(Text.literal("killed.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.DARK_RED))));
    private final MutableText REVIVE_TEXT = Text.literal(" has been ")
            .append(Text.literal("revived.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW))));
    private final MutableText EXECUTE_TEXT = Text.literal(" has been ")
            .append(Text.literal("executed.").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED))));

    MutableText message = Text.literal("the ")
            .append(Text.literal("cat").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55))))  // Yellow for "cat"
            .append(Text.literal(" is on "))
            .append(Text.literal("fire").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)))); // Red for "fire"

    //variables for command inputs
    private final List<String> tpOptions = Arrays.asList("home","house","vote","chair","town","dorm");
    private final static String DEFAULT_MAP = "default";
    private final static String SCHOOL_MAP = "school";
    private final static List<String> MAPS = Arrays.asList(DEFAULT_MAP,SCHOOL_MAP);
    private String mapSelected = DEFAULT_MAP; //Defaults to clocktower map

    //TEST VALUES
    private final static BlockPos EXE_BLOCK = new BlockPos(6,-29,-2);

    public static File getConfigFilePath() {
        // Get the Minecraft config directory
        Path configDir = FabricLoader.getInstance().getConfigDir();

        // Make your mod's subfolder (recommended)
        Path modFolder = configDir.resolve("botc-fab"); // e.g., "config/yourmod"
        File folder = modFolder.toFile();
        if (!folder.exists()) folder.mkdirs(); // create if it doesn't exist

        // Final file path
        //TODO Add code for both maps here: returns different csv
        return modFolder.resolve("BOTC-coords-sheet.csv").toFile();
    }

    private void sendMessageToPlayers(Text messageText,List<ServerPlayerEntity> playerList){
        for (ServerPlayerEntity p:playerList){
            p.sendMessage(messageText);
        }
    }

    private String getColourFromPlayer(ServerPlayerEntity player){
        Set<String> tags = player.getCommandTags();
        for (String i : POSSIBLE_COLOURS){
            if (tags.contains(i)){
                return i;
            }
        }
        return "";
    }

    private ServerPlayerEntity getPlayerFromColour(String colour){
        if (players != null){
            for (ServerPlayerEntity p : players) {
                Set<String> tags = p.getCommandTags();
                if (tags.contains(colour)) {
                    return p;
                }
            }
        }
        return null;
    }

    private int getColourHex(String colour){
        if (POSSIBLE_COLOURS.contains(colour)) {
            return COLOUR_HEX.get(POSSIBLE_COLOURS.indexOf(colour));
        } else {
            return 0;
        }
    }

    private void showTitle(ServerPlayerEntity player, Text titleText){
        player.networkHandler.sendPacket(new TitleS2CPacket(titleText));
    }

    private void placeLever(ServerWorld world, BlockPos pos, Direction facing, BlockFace wallSide) {
        if (world == null || world.isClient) return;

        LeverBlock lever = (LeverBlock) Blocks.LEVER;
        LOGGER.info("Setting lever state...");
        // Create the desired lever block state
        var state = lever.getDefaultState()
                //.with(Properties.HORIZONTAL_FACING, facing)
                .with(LeverBlock.FACING, facing)
                .with(LeverBlock.FACE, wallSide)
                .with(Properties.POWERED, false); // default off

        // Place it in the world
        if (world.setBlockState(pos, state, Block.NOTIFY_ALL)) {
            LOGGER.info("lever placed success");
        } else LOGGER.info("lever failed");
    }

    private void placeSign(ServerWorld world, BlockPos pos, Direction facing, Text text, String colour) {
        if (world == null || world.isClient) return;

        //WallSignBlock sign = (WallSignBlock) Blocks.SPRUCE_WALL_SIGN;

        // Place a spruce wall sign facing arg direction
        world.setBlockState(pos, Blocks.SPRUCE_WALL_SIGN.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing));

        SignBlockEntity sign = (SignBlockEntity) world.getBlockEntity(pos);
        if (sign != null) {
            Text[] signPlayerName = new Text[]{
                    Text.literal(""),
                    text, // Replace line 2 with player name, leave other lines empty
                    Text.literal(""),
                    Text.literal("")
            };

            //Format the sign text player name with colour
            SignText formatText = new SignText(signPlayerName, signPlayerName, DyeColor.byName(colour, DyeColor.BLACK), true);
            sign.setText(formatText, true);
            sign.setWaxed(true);

            // Mark updated
            sign.markDirty();
            world.updateListeners(pos, sign.getCachedState(), sign.getCachedState(), 3);
        }
    }

    private void updateVoteStatus(ServerWorld world, ServerPlayerEntity player){
        //Update vote marker and lamp for all players by checking tags
        Set<String> tags = player.getCommandTags();
        String playerColour = getColourFromPlayer(player);
        //Replace lamps
        if (tags.contains(ALIVE)){
            world.setBlockState(mapCoords.get(playerColour).blockUnderLever, Blocks.GOLD_BLOCK.getDefaultState());
            world.setBlockState(mapCoords.get(playerColour).lampsVoteMarker, Blocks.WAXED_COPPER_BULB.getDefaultState());
            //Add back lever in case of revivals
            switch (mapSelected) {
                case DEFAULT_MAP: //Levers face different ways on maps
                    switch (playerColour) {
                        case "black", "cyan", "white":
                            placeLever(world, mapCoords.get(playerColour).lever, Direction.EAST, BlockFace.FLOOR);
                            break;
                        case "yellow", "pink", "gray":
                            placeLever(world, mapCoords.get(playerColour).lever, Direction.SOUTH, BlockFace.FLOOR);
                            break;
                        case "orange", "red", "brown":
                            placeLever(world, mapCoords.get(playerColour).lever, Direction.WEST, BlockFace.FLOOR);
                            break;
                        case "purple", "green", "blue":
                            placeLever(world, mapCoords.get(playerColour).lever, Direction.NORTH, BlockFace.FLOOR);
                            break;
                    }
                    break;
                case SCHOOL_MAP: //
                    placeLever(world, mapCoords.get(playerColour).lever, Direction.EAST, BlockFace.FLOOR); //Make sure direction is correct later
            }

        }
        if (tags.contains(GHOST)){
            world.setBlockState(mapCoords.get(playerColour).blockUnderLever, Blocks.IRON_BLOCK.getDefaultState());
            world.setBlockState(mapCoords.get(playerColour).lampsVoteMarker, Blocks.WAXED_OXIDIZED_COPPER.getDefaultState());
        }
        if (tags.contains(DEAD)){
            world.setBlockState(mapCoords.get(playerColour).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState()); //Set player indicator to netherite
            world.setBlockState(mapCoords.get(playerColour).lampsVoteMarker, Blocks.COAL_BLOCK.getDefaultState()); //Disable ghost vote after use by setting to coal
            world.setBlockState(mapCoords.get(playerColour).lever, Blocks.AIR.getDefaultState()); //Remove lever
        }
    }

    private void createTeam(@NotNull ServerScoreboard scoreboard, String teamName) {
        // Check if the team already exists. Should only need to run once per server
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            // Create the team if it doesn't exist
            team = scoreboard.addTeam(teamName);
            System.out.println("Created new team: " + teamName);
        } else {
            System.out.println("Team already exists: " + teamName);
        }
        team.setShowFriendlyInvisibles(true); //makes invisible players visible
    }

    private void resetPlayer(@NotNull PlayerEntity player) { //Removes all game based tags from a player
        for (String tag : ALL_TAGS) {
            player.removeCommandTag(tag);
        }
        for (String tag : POSSIBLE_COLOURS) {
            player.removeCommandTag(tag);
        }
    }

    private void removeTagAllPlayers(String tag){
        if (players != null) {
            for (ServerPlayerEntity p : players) {
                p.removeCommandTag(tag);
            }
        }
    }

    private int getTagCount(String tag){
        int count = 0;
        if (players != null) {
            for (ServerPlayerEntity p : players) {
                Set<String> tags = p.getCommandTags();
                if (tags.contains(tag))
                    count++;
            }
        }
        return count;
    }

    //int colourHex = getColourHex(c);
    private void setColourBoots(ServerPlayerEntity p, String c){ //Give player boots with assigned colour

        //String colourRGB = Color.decode(Integer.toString(colourHex)).toString();

        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        List<DyeItem> dyes = List.of(DyeItem.byColor(DyeColor.byName(c,DyeColor.LIME))); //Defaults to lime if colour not got from String
        ItemStack dyedBoots = DyedColorComponent.setColor(boots, dyes);
        p.getInventory().setStack(36,dyedBoots); //36 is slot for boots, IDK why help
    }

    private void accusePlayer(ServerPlayerEntity player){
        removeTagAllPlayers(ACCUSED);
        player.addCommandTag(ACCUSED);
    }

    private void markPlayerDemonKill(ServerPlayerEntity player){
        Set<String> tags = player.getCommandTags();
        if (!tags.contains(GHOST) && !tags.contains(DEAD)) { //If player already dead, do not tag for announce.
            player.addCommandTag(DEATH_FLAG);
        }
    }

    private void markPlayerRevived(ServerPlayerEntity player){
        Set<String> tags = player.getCommandTags();
        if (!tags.contains(ALIVE)) { //If player still alive do not mark for revival
            player.addCommandTag(REVIVE_FLAG);
        }
    }

    private void killPlayer(ServerPlayerEntity player){
        ServerWorld world = player.getServerWorld();
        Set<String> tags = player.getCommandTags();
        if (!tags.contains(GHOST) && !tags.contains(DEAD)) {
            player.addCommandTag(GHOST);
            player.removeCommandTag(ALIVE);
            updateVoteStatus(world,player);
        }
        else {
            world.getServer().getCommandSource().sendFeedback(() -> Text.literal("They're already dead :("),false);
        }
    }

    private void revivePlayer(ServerPlayerEntity player){
        ServerWorld world = player.getServerWorld();
        Set<String> tags = player.getCommandTags();
        if (tags.contains(GHOST) || tags.contains(DEAD)) {
            player.removeCommandTag(GHOST);
            player.removeCommandTag(DEAD);
            player.addCommandTag(ALIVE);
            updateVoteStatus(world,player);
        }
        else {
            world.getServer().getCommandSource().sendFeedback(() -> Text.literal("They're already alive :)"),false);
        }
    }

    private void tp(ServerPlayerEntity player, BlockPos destination){ //teleports player to BlockPos by converting to x,y,z
        player.teleport(destination.getX()+0.5,destination.getY(),destination.getZ()+0.5,false); //0.5 for centre of block
    }

    private void teleportPlayers(String location){
        //location either "home" or "vote"
        String colour;
        switch (location){
            case "home","house","dorm":
                for (ServerPlayerEntity p : players) {
                    colour = getColourFromPlayer(p);
                    tp(p,mapCoords.get(colour).homeInside); //teleport player to coords
                }
                break;
            case "vote","chair","town":
                for (ServerPlayerEntity p : players) {
                    colour = getColourFromPlayer(p);
                    tp(p,mapCoords.get(colour).chair.up(1)); //Needs to go up 1 so space isn't occupied
                }
                break;
        }
    }

    private MutableText getEventText(ServerPlayerEntity player, String event){
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

    private ActionResult onRightClickItem(PlayerEntity player, World world, Hand hand) { //Checks if player right-clicked with paper, then sends player order
        ItemStack itemInHand = player.getStackInHand(hand);
        // Check if the player is holding the specific item and Hand is not empty
        if (!itemInHand.isEmpty() && itemInHand.getItem() == Items.PAPER) {
            player.sendMessage(getPlayerOrder(), false);
            return ActionResult.SUCCESS;
        }
        //Check for heart pottery shard named "Emergency Teleport"
        if (!itemInHand.isEmpty() && itemInHand.getItem() == Items.HEART_POTTERY_SHERD) {
            player.sendMessage(getPlayerOrder(), false);
            String colour = getColourFromPlayer((ServerPlayerEntity) player);
            tp((ServerPlayerEntity) player, mapCoords.get(colour).homeInside);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private void createOrSetAliveDisplay(ServerScoreboard scoreboard){
        int alivePlayers = getTagCount(ALIVE);
        int deadPlayers = getTagCount(GHOST) + getTagCount(DEAD);
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

    private void onGameInit(MinecraftServer srv) { //Run this on server startup, players do not need to be connected
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
        createTeam(scoreboard, TEAM_ALL);

        // Loop through all colours in map
        for (String i : mapCoords.keySet()) {
            // Set all lamps and status markers to netherite and levers to air
            world.setBlockState(mapCoords.get(i).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState());
            world.setBlockState(mapCoords.get(i).lever, Blocks.AIR.getDefaultState());
            world.setBlockState(mapCoords.get(i).lampsVoteMarker, Blocks.COAL_BLOCK.getDefaultState());
        }

        src.sendFeedback(() -> Text.literal("Finished"), false);
    }

    private int setupGame(CommandContext<ServerCommandSource> context) {
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        ServerScoreboard scoreboard = srv.getScoreboard();
        ServerWorld world = context.getSource().getWorld();
        players.clear();
        players.addAll(playerMgr.getPlayerList());
        //TODO Test this
        Collections.shuffle(players); //Randomises order of player list (default is server join order)
        indexBounds.clear();
        int startPoint = ThreadLocalRandom.current().nextInt(0, 11+1); //Determines start point for colour selection
        ServerPlayerEntity storyTeller = src.getPlayer(); // Gets the person that called the command. Whoever called it is Storyteller

        // Set everyone else to be a player
        // Remove the storyTeller from the list of players. Remaining list is all players

        System.out.println(players);

        if (storyTeller != null) {
            // Remove all tags before adding new ones
            players.remove(storyTeller);
            resetPlayer(storyTeller);
            storyTeller.getInventory().setStack(36,ItemStack.EMPTY); //Remove boots
            storyTeller.addCommandTag(STORYTELLER); //Add tag for storyteller
            srv.sendMessage(Text.literal("Storyteller is: " + storyTeller.getStyledDisplayName().toString()));
            storyTeller.changeGameMode(GameMode.CREATIVE);
            setColourBoots(storyTeller,"no");
            scoreboard.addScoreHolderToTeam(storyTeller.getNameForScoreboard(), scoreboard.getTeam(TEAM_ALL));
            src.sendFeedback(() -> message, false);
        } else {
            src.sendFeedback(() -> Text.literal("Failed to find storyteller. Do not execute this command from the server window"), false);
            return 0;
        }
        System.out.println(players); //Debugging

        //List<ServerPlayerEntity> spectators = new LinkedList<>();
        for (ServerPlayerEntity p : players){
            Set<String> tags = p.getCommandTags();
            if (tags.contains(SPEC)) {
                p.changeGameMode(GameMode.SPECTATOR);
                players.remove(p); //Remove spectators from player list
                scoreboard.addScoreHolderToTeam(p.getNameForScoreboard(), scoreboard.getTeam(TEAM_ALL));
            }
        }
        System.out.println(players); //Debugging

        storyTeller.setSpawnPoint(World.OVERWORLD,EXE_BLOCK,0,true,true); //Should work now?

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

                if (currentColourIndex == 12)
                    currentColourIndex = 0;
                else
                    currentColourIndex++;
            }
        }
        createOrSetAliveDisplay(scoreboard); //Update/create scoreboard for start of game.
        srv.sendMessage(Text.literal(players.toString()));

        //Remove votes and signs for missing players
        for (String c:POSSIBLE_COLOURS){
            if (getPlayerFromColour(c) == null){
                world.setBlockState(mapCoords.get(c).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState()); //Set player indicator to netherite
                world.setBlockState(mapCoords.get(c).lampsVoteMarker, Blocks.COAL_BLOCK.getDefaultState()); //Disable ghost vote after use by setting to coal
                world.setBlockState(mapCoords.get(c).lever, Blocks.AIR.getDefaultState()); //Remove lever
                world.setBlockState(mapCoords.get(c).sign, Blocks.AIR.getDefaultState()); //Remove empty signs
            }
        }

        return 1;
    }

    private int beginGame(CommandContext<ServerCommandSource> context) {
        //TODO
        // - give roles to players on named paper
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();

        teleportPlayers("home"); //teleports players to homes
        sendMessageToPlayers(getPlayerOrder(),playerMgr.getPlayerList()); //Sends player order to all players
        nightFalls(context);

        return 1;
    }

    private int onAddSpectator(CommandContext<ServerCommandSource> context) {
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        String specName = StringArgumentType.getString(context, "player_name"); //Gets spectator player name from command
        ServerPlayerEntity specTarget = playerMgr.getPlayer(specName);
        if (specTarget == null) {

            System.out.println("Couldn't find player");
            return 0;
        }

        resetPlayer(specTarget);
        specTarget.addCommandTag(SPEC);
        specTarget.changeGameMode(GameMode.SPECTATOR);
        players.remove(specTarget);

        src.sendFeedback(() -> Text.literal("Called /addSpectator with value 1 = %s ".formatted(specName)), false);
        return 1;
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
                ServerPlayerEntity executee = getPlayerFromColour(CURRENT_EXECUTEE);
                if (executee != null){
                    switch (mapSelected) {
                        case DEFAULT_MAP:
                            Vec3d murderZone = new Vec3d(EXE_BLOCK.down(1).getX()+0.5,EXE_BLOCK.down(1).getY()+0.3,EXE_BLOCK.down(1).getZ()+0.5); //Get centre of block
                            double distance = executee.getPos().squaredDistanceTo(murderZone); //should work with vec3d now
                            if (distance > 1.2) {
                                executee.teleport(murderZone.getX(),murderZone.getY(),murderZone.getZ(),false); //needs accurate tp
                                executee.sendMessage(Text.literal("Nice try :)"), false);
                            }

                            BlockState anvilPosState = world.getBlockState(EXE_BLOCK);
                            if (anvilPosState.isOf(Blocks.ANVIL) || anvilPosState.isOf(Blocks.CHIPPED_ANVIL) || anvilPosState.isOf(Blocks.DAMAGED_ANVIL)) {
                                world.setBlockState(EXE_BLOCK, Blocks.AIR.getDefaultState());
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
        if (timerBarActive) { //Timer bar for talk time
            timerBarTicks++;
            float progress = 1.0f - (float) timerBarTicks / timerDurationTicks;
            if (timerBar != null) {
                timerBar.setPercent(Math.max(progress, 0f));
            }

            if (timerBarTicks >= timerDurationTicks) { //On timer finish
                timerBarActive = false;
                timerBarTicks = 0;
                if (timerBar != null) {
                    timerBar.setPercent(0f);
                    timerBar.setVisible(false); //Make timer invisible as finished
                    for (ServerPlayerEntity p : srv.getPlayerManager().getPlayerList()) {
                        showTitle(p,Text.literal("TIME UP"));
                    }
                }
            }
        }
        if (playersLockedToSeats && players != null) { //checks if players locked is true and players list is not null
            for (ServerPlayerEntity p:players) {
                Set<String> tags = p.getCommandTags();
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

    private int nightFalls(CommandContext<ServerCommandSource> context){
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        ServerWorld world = src.getWorld();
        PlayerManager playerMgr = srv.getPlayerManager();
        ServerScoreboard scoreboard = world.getScoreboard();
        world.setTimeOfDay(18000L);
        playersLockedToSeats = false;
        executionInProgress = false;
        //Remove all temp tags from players
        removeTagAllPlayers(ACCUSED);
        removeTagAllPlayers(DEATH_FLAG);
        removeTagAllPlayers(REVIVE_FLAG);
        removeTagAllPlayers(CURRENT_EXECUTEE);
        removeTagAllPlayers(MARKED);
        Team team = scoreboard.getTeam(TEAM_ALL);
        if (team != null) { //Make nametags visible at morning.
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
        }
        sendMessageToPlayers(NIGHT_MESSAGE.append(RETURN_MESSAGE),playerMgr.getPlayerList()); //Send msg to all players
        return 1;
    }

    private int startDay(CommandContext<ServerCommandSource> context){
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        ServerWorld world = src.getWorld();
        List<ServerPlayerEntity> playerList = srv.getPlayerManager().getPlayerList();
        ServerScoreboard scoreboard = srv.getScoreboard();
        Team team = scoreboard.getTeam(TEAM_ALL);
        playersLockedToSeats = false;
        world.setTimeOfDay(1000L);
        if (team != null) { //Make nametags visible at morning.
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        }

        sendMessageToPlayers(DAY_MESSAGE.append(MEETING_MESSAGE),playerList);
        //Send player death message and update status
        int deaths = getTagCount(DEATH_FLAG);
        for (int i = 0;i < deaths;i++){
            ServerPlayerEntity deadPlayer = getPlayerFromColour(DEATH_FLAG);
            if (deadPlayer != null) {
                sendMessageToPlayers(getEventText(deadPlayer,DEATH_FLAG) ,playerList);
                killPlayer(deadPlayer);
                deadPlayer.removeCommandTag(DEATH_FLAG);
            }
        }
        int revives = getTagCount(REVIVE_FLAG);
        for (int i = 0;i < revives;i++){
            ServerPlayerEntity alivePlayer = getPlayerFromColour(REVIVE_FLAG);
            if (alivePlayer != null) {
                sendMessageToPlayers(getEventText(alivePlayer,REVIVE_FLAG) ,playerList);
                revivePlayer(alivePlayer);
                alivePlayer.removeCommandTag(REVIVE_FLAG);
            }
        }
        for (ServerPlayerEntity p : playerList){
            showTitle(p,DAY_MESSAGE);
        }
        highestVote = 0; //reset highest vote

        createOrSetAliveDisplay(scoreboard); //Updates scoreboard
        return 1;
    }

    private void onVoteLockIn(CommandContext<ServerCommandSource> context) {
        // remove redstone block
        ServerCommandSource src = context.getSource();
        ServerWorld world = context.getSource().getWorld();
        int delayPerBlock = 40; // 1 second = 20 ticks
        int voteThreshold, startColourIndex, count = 0, alivePlayers = getTagCount(ALIVE);

        List<DelayedBlockSetter> taskList = new ArrayList<>();
        //Get vote threshold for alive players
        voteThreshold = (alivePlayers / 2) + (alivePlayers % 2);

        final ServerPlayerEntity accusedPlayer = getPlayerFromColour(ACCUSED);
        if (accusedPlayer == null){
            src.sendFeedback(() -> Text.literal("No player accused, please accuse someone!"), false);
            return;
        }
        String accusedColour = getColourFromPlayer(accusedPlayer);

        startColourIndex = POSSIBLE_COLOURS.indexOf(accusedColour)+1; //+1 to start from player after accused
        if (!indexBounds.contains(startColourIndex)) {
            startColourIndex = indexBounds.get(0); //This is a list of current colours in the game, so it can loop back to the first if it hits the end.
        }

        for (int i = startColourIndex; count <= players.size(); i++) { //need to be <= or else the accused doesn't get a vote
            int delay = count * delayPerBlock; //Add delay between vote locks
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

            for (int i = finalStartColourIndex; playerCount < players.size(); i++) {
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
                    ServerPlayerEntity playerVote = getPlayerFromColour(colour);
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
            ServerPlayerEntity markedPlayer = getPlayerFromColour(MARKED);

            sendMessageToPlayers(Text.literal("A total of " + displayTotalVotes + " votes were received, including " + displayGhostVotes + " ghost votes."),playerList);
            if ((displayTotalVotes > highestVote) && (displayTotalVotes >= voteThreshold)){ //On beating highest vote and vote threshold mark accused player and remove old.
                highestVote = displayTotalVotes; //Change the highest vote to this vote
                //Mark player for execution
                accusedPlayer.addCommandTag(MARKED);
                if (markedPlayer != null) { //Remove any previous marked players
                    markedPlayer.removeCommandTag(MARKED);
                }
                src.sendFeedback(accusedPlayer::getStyledDisplayName, false);

                MutableText message = MutableText.of((TextContent) accusedPlayer.getStyledDisplayName()).append(" has now been marked for execution");
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

    private void executePlayer(ServerPlayerEntity player){
        player.addCommandTag(CURRENT_EXECUTEE);
        ServerWorld world = player.getServerWorld();
        //TODO TEST THIS
        playersLockedToSeats = false;
        executionInProgress = true;
        switch (mapSelected) {
            case DEFAULT_MAP:
                tp(player,EXE_BLOCK.down(1)); //tp executed player to the block
                world.setBlockState(EXE_BLOCK.up(50), Blocks.ANVIL.getDefaultState()); //create anvil 50 blocks up above execution
                break;
            case SCHOOL_MAP:
                //TODO open pit here and teleport player
                break;
        }
    }

    private int beginExecution(CommandContext<ServerCommandSource> context){
        ServerCommandSource src = context.getSource();
        ServerPlayerEntity player = getPlayerFromColour(MARKED);
        removeTagAllPlayers(ACCUSED); //Removed all accused players
        if (player != null) {
            player.removeCommandTag(MARKED);
            executePlayer(player);
        } else src.sendFeedback(() -> Text.literal("No pony is marked for execution so no pony was killed"),false);
        return 1;
    }

    private void startTimer(MinecraftServer srv,int duration){ //Starts a timer and shows boss bar as remaining, duration in seconds
        if (timerBar == null) {
            timerBar = new ServerBossBar(Text.literal("Time remaining:"), BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
        }
        timerBar.setPercent(1.0f); //100%
        timerBar.setVisible(true);
        for (ServerPlayerEntity player : srv.getPlayerManager().getPlayerList()) {
            timerBar.addPlayer(player);
        }

        timerBarTicks = 0;
        timerDurationTicks = duration*20; //*20 to get seconds value in ticks
        timerBarActive = true;

        srv.sendMessage(Text.literal("Boss bar timer started."));
    }

    private MutableText getPlayerOrder(){
        MutableText PlayerOrderMessage = Text.literal("Player Order: \n");
        for (ServerPlayerEntity p:players){
            String colour = getColourFromPlayer(p);
            PlayerOrderMessage
                    .append(p.getStyledDisplayName().copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(colour)))))  // Player name with colour
                    .append(Text.literal("\n")); //New line
        }
        return PlayerOrderMessage;
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        //TODO Find a way for commands to be op only

        LOGGER.info("Hello Fabric world!");
        //Run on each world end tick
        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTick); //(ServerWorld world) -> onWorldTick(world)
        TickScheduler.register();

        //Run on each server start tick
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        //Code that runs on server start.
        ServerLifecycleEvents.SERVER_STARTED.register(this::onGameInit);

        //Register events
        UseEntityCallback.EVENT.register(this::onRightClickEntity);
        UseItemCallback.EVENT.register(this::onRightClickItem);

        //Register chat commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("setupGame")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(this::setupGame)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("startGame")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(this::beginGame)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("importCSV")
                .requires(source -> source.hasPermissionLevel(2))
                .executes((context) -> {
                    mapCoords = ImportExcelCoordinates.read(getConfigFilePath()); //Import coordinates for map from Excel sheet
                    return 1;
                    })));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("changeMap").then(
                CommandManager.argument("map", StringArgumentType.string())
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests((context, builder) -> {
                            // Suggest maps
                            for (String m:MAPS)
                                builder.suggest(m);
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
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("addSpectator").then(
                CommandManager.argument("player_name", StringArgumentType.string())
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(this::onAddSpectator)
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("tpPlayers").then(
                CommandManager.argument("tp_location", StringArgumentType.string())
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests((context, builder) -> {
                            // Suggest teleport locations
                            for (String o:tpOptions)
                                builder.suggest(o); //Not sure if this works so use code below if not
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String option = StringArgumentType.getString(context, "tp_location");
                            option = option.toLowerCase(); //lowercase to account for typos

                            if (tpOptions.contains(option)) {
                                teleportPlayers(option);
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("Invalid teleport location"), false);
                            }
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("startTimer").then(
                CommandManager.argument("stringTime", StringArgumentType.string())
                        //Starts a timer boss bar for [argument] minutes
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            String stringTime = StringArgumentType.getString(context, "stringTime");// Get the timer duration as string from argument
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
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("executePlayer").then(
                CommandManager.argument("player", EntityArgumentType.player())
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            executePlayer(player);
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("demonKillMark").then(
                CommandManager.argument("player", EntityArgumentType.player()) //Command to mark player as the demon kill tonight.
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            markPlayerDemonKill(player);
                            context.getSource().sendFeedback(() -> Text.literal("Marked player for demon kill: ").append(player.getStyledDisplayName()), false);
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("reviveMark").then(
                CommandManager.argument("player", EntityArgumentType.player()) //Command to mark player to be revived in the day.
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            markPlayerRevived(player);
                            context.getSource().sendFeedback(() -> Text.literal("Marked player for revival upon morning: ").append(player.getStyledDisplayName()), false);
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("accuse").then(
                CommandManager.argument("player", EntityArgumentType.player()) // accuse player for execution, in case right click selector doesn't work.
                        .requires(source -> source.hasPermissionLevel(2))
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            accusePlayer(player);
                            context.getSource().sendFeedback(() -> Text.literal("Accused: ").append(player.getStyledDisplayName()), false);
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("startDay")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(this::startDay)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("nightFalls")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(this::nightFalls)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("voteLockIn")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    LOGGER.info("Beginning vote lock in");
                    onVoteLockIn(context);
                    LOGGER.info("Completed /onVoteLockIn.");
                    return 1;
                }
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("toggleSeatLock")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    playersLockedToSeats = !playersLockedToSeats;
                    context.getSource().sendFeedback(() -> Text.literal("Toggled player seat lock to " + playersLockedToSeats), false);
                    return 1;
                }
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("showPlayerOrder").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer(); //gets player running command
                    if (player != null) {
                        player.sendMessage(getPlayerOrder());
                    } else {
                        context.getSource().sendFeedback(() -> Text.literal("No player to send text to, do not run in server console."), false);
                    }
                    return 1;
                }
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("beginExecution")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(this::beginExecution)));
    }
}