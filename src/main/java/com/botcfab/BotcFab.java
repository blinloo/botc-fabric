package com.botcfab;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import net.minecraft.state.property.Properties;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BotcFab implements ModInitializer {
    public static final String MOD_ID = "botc-fab";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    //Team variables
    private static final String TEAM_STORYTELLER = "teamStoryteller";
    private static final String TEAM_SPECTATOR = "teamSpectator";
    private static final String TEAM_BLACK = "01Black";
    private static final String TEAM_YELLOW = "02Yellow";
    private static final String TEAM_ORANGE = "03Orange";
    private static final String TEAM_PINK = "04Pink";
    private static final String TEAM_RED = "05Red";
    private static final String TEAM_PURPLE = "06Purple";
    private static final String TEAM_BROWN = "07Brown";
    private static final String TEAM_GREEN = "08Green";
    private static final String TEAM_WHITE = "09White";
    private static final String TEAM_BLUE = "10Blue";
    private static final String TEAM_CYAN = "11Cyan";
    private static final String TEAM_GREY = "12Grey";
    private static final List<String> TEAM_COLOURS = Arrays.asList(
            TEAM_BLACK,
            TEAM_YELLOW,
            TEAM_ORANGE,
            TEAM_PINK,
            TEAM_RED,
            TEAM_PURPLE,
            TEAM_BROWN,
            TEAM_GREEN,
            TEAM_WHITE,
            TEAM_BLUE,
            TEAM_CYAN,
            TEAM_GREY);
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
    private static final String ALIVE_SCORE_HOLDER = "#alive";
    private static final String DEAD_SCORE_HOLDER = "#dead";
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
    private final String path = ".\\BOTC-coords-sheet.csv"; //Attempt to give standard file path
    //private final String path = "C:\\Users\\Ruby\\IdeaProjects\\botc-fabric-copytest\\BOTC-coords-sheet.csv"; //Absolute file path

    //Huge penis of coordinates with ref, e.g. mapCoords.get("Yellow").ghost
    Map<String, CoordinateMapper> mapCoords = new HashMap<>();
    private List<ServerPlayerEntity> players = new ArrayList<>();
    //Highest vote count
    private int highestVote;

    //Timer bar variables
    private ServerBossBar timerBar;
    private boolean timerBarActive = false;
    private int timerBarTicks = 0;
    private int timerDurationTicks = 10*20; // 10 seconds default

    //Variables for on tick checks
    private boolean playersLockedToSeats = false;

    //Text for displays
    private final MutableText MEETING_MESSAGE = Text.literal("Please head to the town square");
    private final MutableText RETURN_MESSAGE = Text.literal("Please return to your houses");
    private final MutableText DAY_MESSAGE = Text.literal("The sun rises ☀")
            .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
            .append(Text.literal("\n"));
    private final MutableText NIGHT_MESSAGE = Text.literal("Night falls... 🌙")
            .setStyle(Style.EMPTY.withColor(Formatting.DARK_BLUE))
            .append(Text.literal("\n"));
    private final MutableText KILL_TEXT = Text.literal(" has been killed.")
            .formatted(Formatting.DARK_RED);
    private final MutableText REVIVE_TEXT = Text.literal(" has been revived.")
            .formatted(Formatting.YELLOW);

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

//        SignText formatText = new SignText();
//        //Sets text to sign format and makes it glow.
//        formatText
//                .withMessage(1,text) //Adds text to sign, line 2, player name
//                .withGlowing(true) //Sets text to glowing
//                .withColor(DyeColor.byName(colour,DyeColor.BLACK)); //adds the dye colour to sign
//
//        // Access the block entity and set the text on the second line
//        if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
//            sign.setText(formatText,true);
//            sign.setWaxed(true);
//            sign.markDirty();
//            world.updateListeners(pos, sign.getCachedState(), sign.getCachedState(), 3);
//            //world.updateListeners(pos, world.getBlockState(pos), sign.getCachedState(), 3);
//        }
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
        //TODO add functionality for new map
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

        //Assigns player name colour to teams and display names
        switch (teamName){
            case TEAM_STORYTELLER,TEAM_SPECTATOR:
                team.setDisplayName(Text.of("Story Teller")); //Black
                team.setColor(Formatting.GRAY);
                break;
            case TEAM_BLACK:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(0))); //Black
                team.setColor(Formatting.BLACK);
                break;
            case TEAM_YELLOW:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(1))); //Yellow
                team.setColor(Formatting.YELLOW);
                break;
            case TEAM_ORANGE:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(2))); //Orange
                team.setColor(Formatting.GOLD);
                break;
            case TEAM_PINK:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(3))); //Pink
                team.setColor(Formatting.LIGHT_PURPLE);
                break;
            case TEAM_RED:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(4))); //Red
                team.setColor(Formatting.RED);
                break;
            case TEAM_PURPLE:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(5))); //Purple
                team.setColor(Formatting.DARK_PURPLE);
                break;
            case TEAM_BROWN:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(6))); //Brown
                team.setColor(Formatting.DARK_RED);
                break;
            case TEAM_GREEN:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(7))); //Green
                team.setColor(Formatting.DARK_GREEN);
                break;
            case TEAM_WHITE:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(8))); //White
                team.setColor(Formatting.WHITE);
                break;
            case TEAM_BLUE:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(9))); //Blue
                team.setColor(Formatting.BLUE);
                break;
            case TEAM_CYAN:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(10))); //Cyan
                team.setColor(Formatting.AQUA);
                break;
            case TEAM_GREY:
                team.setDisplayName(Text.of(POSSIBLE_COLOURS.get(11))); //Grey
                team.setColor(Formatting.DARK_GRAY);
                break;
        }
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
    }

    private void tp(ServerPlayerEntity player, BlockPos destination){ //teleports player to BlockPos by converting to x,y,z
        player.teleport(destination.getX()+0.5,destination.getY(),destination.getZ()+0.5,false); //0.5 for centre of block
    }

    private void teleportPlayers(String location){
        //location either "home" or "vote"
        //TODO write this function!
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
                    tp(p,mapCoords.get(colour).chair);
                } //TODO add code to lock players to seats for nominations
                break;
        }
    }

    private MutableText getEventText(ServerPlayerEntity player, String event){
        MutableText eventMsg;
        MutableText playerName = player.getStyledDisplayName().copy(); //Should be formatted with player colour from team
        eventMsg = switch (event) {
            case REVIVE_FLAG -> KILL_TEXT;
            case DEATH_FLAG -> REVIVE_TEXT;
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
                    world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.0F, 1.0F);
                }
                return ActionResult.SUCCESS; // Return success to stop further processing
            }
        }
        return ActionResult.PASS; // Pass if not right-clicking with the correct item or targeting a player
    }

    private void createOrSetAliveDisplay(ServerScoreboard scoreboard){
        int alivePlayers = getTagCount(ALIVE);
        int deadPlayers = getTagCount(GHOST) + getTagCount(DEAD);
        ScoreboardObjective objective = scoreboard.getNullableObjective(INFO_OBJECTIVE);
        NumberFormat numberFormat = StyledNumberFormat.YELLOW; //Can be RED, YELLOW or EMPTY
        if (objective == null){
        objective = scoreboard.addObjective(
                INFO_OBJECTIVE, // Objective name (unique id)
                ScoreboardCriterion.DUMMY, // Criterion type (dummy = manual numbers)
                Text.literal("--|Player Info|--"), // Display name (shown in sidebar, below name, etc.)
                ScoreboardCriterion.RenderType.INTEGER, // Render type (number type)
                true, //Whether the value updates live
                numberFormat //Format of numbers, colour and display
        );
        }
        ScoreAccess aliveScoreAccess = scoreboard.getOrCreateScore(ScoreHolder.fromName(ALIVE_SCORE_HOLDER),objective);
        ScoreAccess deadScoreAccess = scoreboard.getOrCreateScore(ScoreHolder.fromName(DEAD_SCORE_HOLDER),objective);
        aliveScoreAccess.setDisplayText(Text.literal("Alive : "));
        aliveScoreAccess.setScore(alivePlayers);
        deadScoreAccess.setDisplayText(Text.literal("Dead  : "));
        deadScoreAccess.setScore(deadPlayers);
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR,objective);
    }

    private void onGameInit(MinecraftServer srv) { //Run this on server startup, players do not need to be connected
        ServerCommandSource src = srv.getCommandSource();
        ServerScoreboard scoreboard = srv.getScoreboard();
        ServerWorld world = src.getWorld();
        mapCoords = ImportExcelCoordinates.read(path); //Import csv file here
        List<String> allTeams = new ArrayList<>(Arrays.asList(TEAM_STORYTELLER, TEAM_SPECTATOR));
        allTeams.addAll(TEAM_COLOURS); //Adds colour teams to all teams list

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

        // Create all teams
        for (String teamName : allTeams) {
            createTeam(scoreboard, teamName);
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

    private int setupGame(CommandContext<ServerCommandSource> context) {
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        //players = playerMgr.getPlayerList();
        players.clear();
        players.addAll(playerMgr.getPlayerList());
        ServerScoreboard scoreboard = srv.getScoreboard();
        ServerWorld world = context.getSource().getWorld();
        int startPoint = ThreadLocalRandom.current().nextInt(1, 12 + 1); //Determines start point for colour selection
        ServerPlayerEntity storyTeller = src.getPlayer(); // Gets the person that called the command. Whoever called it is Storyteller

        // Set everyone else to be a player
        // Remove the storyTeller from the list of players. Remaining list is all players

        System.out.println(players);
        //TODO remove boots

        if (storyTeller != null) {
            // Remove all tags before adding new ones
            players.remove(storyTeller);
            resetPlayer(storyTeller);
            storyTeller.addCommandTag(STORYTELLER); //Add tag for storyteller
            src.sendFeedback(() -> Text.literal("Storyteller is: " + storyTeller.getName().getString()), false);
            storyTeller.changeGameMode(GameMode.CREATIVE);
            scoreboard.addScoreHolderToTeam(storyTeller.getNameForScoreboard(), scoreboard.getTeam(TEAM_STORYTELLER));
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
            }
        }
        System.out.println(players); //Debugging

        //storyTeller.setSpawnPoint(world,mapCoords.get()); //Might need to do this later

        // Assign colours to players
        int currentColourIndex = startPoint;
        if (players.isEmpty()) {
            src.sendFeedback(() -> Text.literal("No pony online :("), false);
        } else {
            for (ServerPlayerEntity player : players) {
                resetPlayer(player);
                player.addCommandTag(PLAYER);
                player.addCommandTag(ALIVE);
                player.changeGameMode(GameMode.ADVENTURE);
                playerMgr.removeFromOperators(player.getGameProfile());

                //Assign Colours HERE
                String assignedColour = POSSIBLE_COLOURS.get(currentColourIndex-1);
                String assignedColourTeam = TEAM_COLOURS.get(currentColourIndex-1);
                player.addCommandTag(assignedColour); //Add colour tag to player
                scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), scoreboard.getTeam(assignedColourTeam)); //Add player to colour team
                //TODO spawn point doesn't seem to work?
                player.setSpawnPoint(world.getRegistryKey(),mapCoords.get(assignedColour).homeInside,0,true,true); //Set player spawn point

                //world.setBlockState(mapCoords.get(assignedColour).blockUnderLever, Blocks.GOLD_BLOCK.getDefaultState()); Not needed, does in update function
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
                    currentColourIndex = 1;
                else
                    currentColourIndex++;
            }
        }
        createOrSetAliveDisplay(scoreboard); //Update/create scoreboard for start of game.
        src.sendFeedback(() -> Text.literal(players.toString()), false);

        //Remove votes for missing players
        for (String c:POSSIBLE_COLOURS){
            if (getPlayerFromColour(c) == null){
                world.setBlockState(mapCoords.get(c).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState()); //Set player indicator to netherite
                world.setBlockState(mapCoords.get(c).lampsVoteMarker, Blocks.COAL_BLOCK.getDefaultState()); //Disable ghost vote after use by setting to coal
                world.setBlockState(mapCoords.get(c).lever, Blocks.AIR.getDefaultState()); //Remove lever
            }
        }

        return 1;
    }

    private int onAddSpectator(CommandContext<ServerCommandSource> context) {
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        String specName = StringArgumentType.getString(context, "player_name"); //Gets spectator player name from command
        ServerPlayerEntity specTarget = playerMgr.getPlayer(specName);
        ServerScoreboard scoreboard = srv.getScoreboard();
        if (specTarget == null) {

            System.out.println("Couldn't find player");
            return 0;
        }

        resetPlayer(specTarget);
        specTarget.addCommandTag(SPEC);
        scoreboard.addScoreHolderToTeam(specTarget.getNameForScoreboard(), scoreboard.getTeam(TEAM_SPECTATOR));
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
        }//Code for player death particles and particle effects
        if (playerList != null) {
            for (ServerPlayerEntity p : playerList) { //uses full server list to avoid calling null
                Set<String> tags = p.getCommandTags();
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 1, false, false));
                if (tags.contains(DEAD) || tags.contains(GHOST)) {
                    world.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY(), p.getZ(), 1, 0.4, 0.5, 0.4, 0.0001);
                }
                if (tags.contains(ACCUSED) || tags.contains(MARKED)) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 1, false, false));
                } else {
                    p.removeStatusEffect(StatusEffects.GLOWING);
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
                BlockPos playerPos = p.getBlockPos();
                BlockPos targetPos;

                if (!tags.contains(CURRENT_EXECUTEE)) { //Check player is not currently being executed
                    targetPos = mapCoords.get(playerColour).chair;
                } else {
                    break;
                }

                double distance = playerPos.getSquaredDistance(targetPos);
                if (distance > 1.5) {
                    tp(p,targetPos);
                    p.sendMessage(Text.literal("You were too far away. Teleporting to target..."), false);
                }

            }
        }
    }

    private int nightFalls(CommandContext<ServerCommandSource> context){
        ServerCommandSource src = context.getSource();
        ServerWorld world = context.getSource().getWorld();
        world.setTimeOfDay(18000L);
        playersLockedToSeats = false;
        //Remove all death_mark and accused tags from players
        removeTagAllPlayers(ACCUSED);
        removeTagAllPlayers(DEATH_FLAG);
        removeTagAllPlayers(REVIVE_FLAG);

        src.sendFeedback(() -> NIGHT_MESSAGE.append(RETURN_MESSAGE), false);
        return 1;
    }

    private int startDay(CommandContext<ServerCommandSource> context){
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        ServerScoreboard scoreboard = srv.getScoreboard();
        playersLockedToSeats = false;

        src.sendFeedback(() -> DAY_MESSAGE.append(MEETING_MESSAGE), false);
        //Send player death message and update status
        int deaths = getTagCount(DEATH_FLAG);
        for (int i = 0;i < deaths;i++){
            ServerPlayerEntity deadPlayer = getPlayerFromColour(DEATH_FLAG);
            if (deadPlayer != null) {
                src.sendFeedback(() -> getEventText(deadPlayer,DEATH_FLAG), false);
                killPlayer(deadPlayer);
                deadPlayer.removeCommandTag(DEATH_FLAG);
            }
        }
        int revives = getTagCount(REVIVE_FLAG);
        for (int i = 0;i < revives;i++){
            ServerPlayerEntity alivePlayer = getPlayerFromColour(REVIVE_FLAG);
            if (alivePlayer != null) {
                src.sendFeedback(() -> getEventText(alivePlayer,REVIVE_FLAG), false);
                revivePlayer(alivePlayer);
                alivePlayer.removeCommandTag(REVIVE_FLAG);
            }
        }
        for (ServerPlayerEntity p : players){
            //updateVoteStatus(world,p); // shouldn't be needed this done in kill and revive code
            showTitle(p,DAY_MESSAGE);
        }
        highestVote = 0; //reset highest vote

        createOrSetAliveDisplay(scoreboard); //Updates scoreboard
        return 1;
    }

    private void onVoteLockIn(CommandContext<ServerCommandSource> context) {
        // remove redstone block
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        src.sendFeedback(() -> Text.literal("converting... "), false);
        ServerWorld world = context.getSource().getWorld();
        int delayPerBlock = 25; // 1 second = 20 ticks
        final int[] totalVotes = new int[3]; // 0 is total, 1 is alive, 2 is a ghost
        int voteThreshold;
        int alivePlayers = getTagCount(ALIVE);
        src.sendFeedback(() -> Text.literal("Starting redstone removal..."), false);

        List<DelayedBlockSetter> taskList = new ArrayList<>();

        //Get vote threshold for alive players
        voteThreshold = (alivePlayers / 2) + (alivePlayers % 2);

        final ServerPlayerEntity accusedPlayer = getPlayerFromColour(ACCUSED);
        if (accusedPlayer == null){
            src.sendFeedback(() -> Text.literal("No player accused, please accuse someone!"), false);
            return;
        }

        //TODO This whole bit does not work at all, change to run on every tick and count?
        Runnable onAllDone = () -> {
            src.sendFeedback(() -> Text.literal("Starting count..."), false);
            int startColourIndex;
            int ghostVotes = 0;
            int aliveVotes = 0;

            int count = 0;
            String accusedColour = getColourFromPlayer(accusedPlayer);
            startColourIndex = POSSIBLE_COLOURS.indexOf(accusedColour);

            for (int i = startColourIndex; count <= players.size(); i++) {
                String colour = POSSIBLE_COLOURS.get(i);
                BlockPos pos = mapCoords.get(colour).triggersLampPiston;
                BlockPos lockedVote;
                switch (mapSelected) {
                    case DEFAULT_MAP:
                        lockedVote = pos.up(2); //Position of locked in vote lamp
                        break;
                    case SCHOOL_MAP:
                        lockedVote = pos.east(2); //Position of locked in vote lamp Change direction based on orientation
                        break;
                    default:
                        lockedVote = pos.up(2);
                }

                int delay = count * delayPerBlock; //Add delay between vote locks
                taskList.add(new DelayedBlockSetter(world, pos, Blocks.AIR.getDefaultState(), delay)); //Set redstone block to air

                BlockState lockedVoteState = world.getBlockState(lockedVote);
                if (lockedVoteState.getBlock() == Blocks.WAXED_COPPER_BULB && lockedVoteState.get(Properties.LIT)) {
                    aliveVotes++;
                } else if (lockedVoteState.getBlock() == Blocks.SEA_LANTERN) {
                    ghostVotes++;
                    // remove ghost vote tag from player
                    ServerPlayerEntity playerVote = getPlayerFromColour(colour);
                    if (playerVote != null) {
                        playerVote.addCommandTag(DEAD);
                        playerVote.removeCommandTag(GHOST);
                    }

                }
                if (i == 11){
                    i = -1; //Set to -1 so it continues to loop through colours at 0 when the loop resets.
                }

                count++;
            }
            totalVotes[0] = aliveVotes + ghostVotes; //total votes
            totalVotes[1] = aliveVotes; //alive player votes
            totalVotes[2] = ghostVotes; //ghost votes used
        };
        TickScheduler.scheduleGroup(taskList, onAllDone);
        ServerPlayerEntity markedPlayer = getPlayerFromColour(MARKED);

        src.sendFeedback(() -> Text.literal("A total of " + totalVotes[0] + "votes were received, including " + totalVotes[2] + " ghost votes."), false);
        if ((totalVotes[0] > highestVote) && (totalVotes[0] >= voteThreshold)){ //On beating highest vote and vote threshold mark accused player and remove old.
            highestVote = totalVotes[0]; //Change the highest vote to this vote
            //Mark player for execution
            accusedPlayer.addCommandTag(MARKED);
            if (markedPlayer != null) { //Remove any previous marked players
                markedPlayer.removeCommandTag(MARKED);
            }
            highestVote = totalVotes[0];
            src.sendFeedback(accusedPlayer::getStyledDisplayName, false);
            src.sendFeedback(() -> Text.literal("has now been marked for execution"),false);
        }
        if (totalVotes[0] == highestVote){ //On matching highest vote, remove all marked players
            if (markedPlayer != null) {
                markedPlayer.removeCommandTag(MARKED);
            }
        }
        accusedPlayer.removeCommandTag(ACCUSED);

        for (ServerPlayerEntity p : players) {
            updateVoteStatus(world, p); //Update votes to remove any used ghost votes
        }
    }

    private void executePlayer(ServerPlayerEntity player){
        player.addCommandTag(CURRENT_EXECUTEE);
        ServerWorld world = player.getServerWorld();
        //TODO actually made this work properly,
        // - trigger execution event (eg anvil, pit open)
        // - check when marked player dies
        // - remove and add appropriate tags
        switch (mapSelected) {
            case DEFAULT_MAP:
                break;
            case SCHOOL_MAP:
                break;
        }

        player.kill(world); //just kills the player not really useful
        killPlayer(player);
        player.removeCommandTag(CURRENT_EXECUTEE); //Remove after animation stuff is done
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
        //TODO Timer boss bar, write this function
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
        ServerLifecycleEvents.SERVER_STARTED.register(this::onGameInit);

        //Register events
        UseEntityCallback.EVENT.register(this::onRightClickEntity);

        //Register chat commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("setupGame").executes(this::setupGame)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("importCSV").executes((context) -> {
            mapCoords = ImportExcelCoordinates.read(path); //Import coordinates for map from Excel sheet
            return 1;
        })));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("changeMap").then(
                CommandManager.argument("map", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            // Suggest teleport locations
                            for (String m:MAPS)
                                builder.suggest(m); //Not sure if this works so use code below if not
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String map = StringArgumentType.getString(context, "map");
                            map = map.toLowerCase(); //lowercase to account for typos

                            if (tpOptions.contains(map)) {
                                mapSelected = map;
                            } else {
                                context.getSource().sendFeedback(() -> Text.literal("Invalid map specified"), false);
                            }
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("addSpectator").then(
                CommandManager.argument("player_name", StringArgumentType.string())
                        .suggests(new PlayerSuggestionProvider())
                        .executes(this::onAddSpectator)
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("tpPlayers").then(
                CommandManager.argument("tp_location", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            // Suggest teleport locations
                            for (String o:tpOptions)
                                builder.suggest(o); //Not sure if this works so use code below if not
                            return builder.buildFuture();
//                            return builder
//                                    .suggest("home")
//                                    .suggest("vote")
//                                    .suggest("chair")
//                                    .suggest("town")
//                                    .suggest("house")
//                                    .buildFuture();
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
                        //
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
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            executePlayer(player);
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("demonKillMark").then(
                CommandManager.argument("player", EntityArgumentType.player()) //Command to mark player as the demon kill tonight.
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            markPlayerDemonKill(player);
                            context.getSource().sendFeedback(() -> Text.literal("Marked player for demon kill:"), false);
                            context.getSource().sendFeedback(player::getStyledDisplayName, false);
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("reviveMark").then(
                CommandManager.argument("player", EntityArgumentType.player()) //Command to mark player to be revived in the day.
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            markPlayerRevived(player);
                            context.getSource().sendFeedback(() -> Text.literal("Marked player for revival upon morning:"), false);
                            context.getSource().sendFeedback(player::getStyledDisplayName, false);
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("accuse").then(
                CommandManager.argument("player", EntityArgumentType.player()) // accuse player for execution, in case right click selector doesn't work.
                        .suggests(new PlayerSuggestionProvider())
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");// Get the player from name string
                            accusePlayer(player);
                            context.getSource().sendFeedback(() -> Text.literal("Accused: "), false);
                            context.getSource().sendFeedback(player::getStyledDisplayName, false);
                            return 1;
                        })
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("startDay").executes(this::startDay)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("nightFalls").executes(this::nightFalls)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("voteLockIn").executes(context -> {
                    context.getSource().sendFeedback(() -> Text.literal("Beginning vote lock in"), false);
                    onVoteLockIn(context);
                    context.getSource().sendFeedback(() -> Text.literal("Completed /onVoteLockIn."), false);
                    return 1;
                }
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("toggleSeatLock").executes(context -> {
                    playersLockedToSeats = !playersLockedToSeats;
                    context.getSource().sendFeedback(() -> Text.literal("Toggled player seat lock"), false);
                    return 1;
                }
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("beginExecution").executes(this::beginExecution)));
    }
}