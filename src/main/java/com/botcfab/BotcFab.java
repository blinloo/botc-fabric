package com.botcfab;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.Orientation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TitleCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.NotNull;
import net.minecraft.state.property.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BotcFab implements ModInitializer {
    public static final String MOD_ID = "botc-fab";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
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
    private static final String ACCUSED = "accused";
    private static final List<String> ALL_TAGS = Arrays.asList(STORYTELLER, SPEC,ALIVE,MARKED,DEAD,GHOST,DEATH_FLAG,ACCUSED);

    private static final List<String> POSSIBLE_COLOURS = Arrays.asList(
            "Black",
            "Yellow",
            "Orange",
            "Pink",
            "Red",
            "Purple",
            "Brown",
            "Green",
            "White",
            "Blue",
            "Cyan",
            "Grey");
    //private final String path = ".\\BOTC-coords-sheet.csv"; //Attempt to give standard file path
    private final String path = "C:\\Users\\Ruby\\IdeaProjects\\botc-fabric\\BOTC-coords-sheet.csv"; //Absolute file path

    //Huge penis of coordinates with ref, e.g. mapCoords.get("Yellow").ghost
    Map<String, CoordinateMapper> mapCoords = new HashMap<>();
    List<ServerPlayerEntity> players;
    //Highest vote count
    int highestVote;

    //Text for displays
    private final MutableText DAY_MESSAGE = Text.literal("The sun rises ☀️")
            .formatted(Formatting.GOLD)
            .append(Text.literal("\\nPlease head to the town square"));
    private final MutableText NIGHT_MESSAGE = Text.literal("Night falls... 🌙")
            .formatted(Formatting.DARK_BLUE)
            .append(Text.literal("\\nPlease return to your houses"));

    private BlockPos convertLocation(String coords) {
        List<Integer> converted = new ArrayList<>();
        for (String s : coords.split(",")) {
            converted.add(Integer.parseInt(s));
        }
        return new BlockPos(converted.get(0), converted.get(1), converted.get(2));
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
        for (ServerPlayerEntity p : players){
            Set<String> tags = p.getCommandTags();
            if (tags.contains(colour)){
                return p;
            }
        }
        return null;
    }

    private void showTitle(ServerPlayerEntity player, Text titleText){
        player.networkHandler.sendPacket(new TitleS2CPacket(titleText));
    }

    private void placeLever(ServerWorld world, BlockPos pos, Direction facing, Orientation wallSide) {
        if (world == null || world.isClient) return;

        LeverBlock lever = (LeverBlock) Blocks.LEVER;

        // Create the desired lever block state
        var state = lever.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing)
                .with(Properties.ORIENTATION, wallSide)
                .with(Properties.POWERED, false); // default off

        // Place it in the world
        world.setBlockState(pos, state);
    }

    private void placeSign(ServerWorld world, BlockPos pos, Direction facing, Text text) {
        if (world == null || world.isClient) return;

        //WallSignBlock sign = (WallSignBlock) Blocks.SPRUCE_WALL_SIGN;

        // Place a spruce wall sign facing NORTH (attached to the SOUTH side of a block)
        world.setBlockState(pos, Blocks.SPRUCE_WALL_SIGN.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing));

        SignText formatText = new SignText();
        //Sets text to sign format and makes it glow.
        formatText.withMessage(1,text); //Adds text to sign, line 2, usually player name
        formatText.withGlowing(true); //Sets text to glowing
        formatText.withColor(DyeColor.valueOf(text.toString()));

        // Access the block entity and set the text on the second line
        if (world.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity) {
            signBlockEntity.setText(formatText,true);
            signBlockEntity.setWaxed(true);
            signBlockEntity.markDirty();
            world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    private void updateDeadPlayer(ServerWorld world, ServerPlayerEntity player) {
        //Add tag for dead
        //Change vote to Ghost vote, block + lantern
        //Add invisibility
        player.removeCommandTag(ALIVE);
        player.addCommandTag(GHOST);
        updateVoteStatus(world, player);
    }

    private void removeGhostVote(ServerWorld world, String playerColour){
        world.setBlockState((mapCoords.get(playerColour).lampsVoteMarker).down(1), Blocks.COAL_BLOCK.getDefaultState()); //Disable ghost vote after use by setting to coal
        world.setBlockState(mapCoords.get(playerColour).lever, Blocks.AIR.getDefaultState()); //Remove lever
        world.setBlockState(mapCoords.get(playerColour).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState()); //Set player indicator to charcoal
    }

    private void updateVoteStatus(ServerWorld world, ServerPlayerEntity player){
        //Update vote marker and lamp for all players by checking tags
        Set<String> tags = player.getCommandTags();
        String playerColour = getColourFromPlayer(player);
        //Replace lamps
        if (tags.contains(ALIVE)){
            world.setBlockState(mapCoords.get(playerColour).blockUnderLever, Blocks.GOLD_BLOCK.getDefaultState());
            world.setBlockState(mapCoords.get(playerColour).lampsVoteMarker, Blocks.WAXED_COPPER_BULB.getDefaultState());
            switch (playerColour) { //Add back lever in case of revivals
                case "Black", "Cyan", "White":
                    placeLever(world, mapCoords.get(playerColour).lever, Direction.EAST, Orientation.DOWN_EAST);
                    break;
                case "Yellow", "Pink", "Grey":
                    placeLever(world, mapCoords.get(playerColour).lever, Direction.SOUTH, Orientation.DOWN_SOUTH);
                    break;
                case "Orange", "Red", "Brown":
                    placeLever(world, mapCoords.get(playerColour).lever, Direction.WEST, Orientation.DOWN_WEST);
                    break;
                case "Purple", "Green", "Blue":
                    placeLever(world, mapCoords.get(playerColour).lever, Direction.NORTH, Orientation.DOWN_NORTH);
                    break;
            }
        }
        if (tags.contains(GHOST)){
            world.setBlockState(mapCoords.get(playerColour).blockUnderLever, Blocks.IRON_BLOCK.getDefaultState());
            world.setBlockState(mapCoords.get(playerColour).lampsVoteMarker, Blocks.WAXED_OXIDIZED_COPPER.getDefaultState());
        }
        if (tags.contains(DEAD)){
            world.setBlockState(mapCoords.get(playerColour).blockUnderLever, Blocks.NETHERITE_BLOCK.getDefaultState());
            world.setBlockState(mapCoords.get(playerColour).lampsVoteMarker, Blocks.COAL_BLOCK.getDefaultState());
            world.setBlockState(mapCoords.get(playerColour).lever, Blocks.AIR.getDefaultState());
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
                team.setColor(Formatting.GRAY);
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
    }

    private void removeTagAllPlayers(String tag){
        for (ServerPlayerEntity p : players){
            p.removeCommandTag(tag);
        }
    }

    private int getAliveCount(){
        int count = 0;
        for (ServerPlayerEntity p : players){
            Set<String> tags = p.getCommandTags();
            if (tags.contains(ALIVE))
                count++;
        }
        return count;
    }

    private int onGameInit(MinecraftServer srv) { //Run this on server startup, players do not need to be connected
//        ServerCommandSource src = context.getSource();CommandContext<ServerCommandSource> context
//        MinecraftServer srv = src.getServer();
//        PlayerManager playerMgr = srv.getPlayerManager();
//        players = playerMgr.getPlayerList();
//        ServerWorld world = context.getSource().getWorld();

        ServerCommandSource src = srv.getCommandSource();
        ServerScoreboard scoreboard = srv.getScoreboard();
        ServerWorld world = src.getWorld();
        List<String> allTeams = new ArrayList<>(Arrays.asList(TEAM_STORYTELLER, TEAM_SPECTATOR));
        allTeams.addAll(TEAM_COLOURS); //Adds colour teams to all teams list

        System.out.println("INITIALISED");
        src.sendFeedback(() -> Text.literal("Starting initialise code now"), false);
        world.setMobSpawnOptions(false);

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
        return 1;
    }

    private int setupGame(CommandContext<ServerCommandSource> context) {
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        players = playerMgr.getPlayerList();
        ServerScoreboard scoreboard = srv.getScoreboard();
        ServerWorld world = context.getSource().getWorld();
        List<String> allTeams = Arrays.asList(TEAM_STORYTELLER, TEAM_SPECTATOR);
        allTeams.addAll(TEAM_COLOURS); //Adds colour teams to all teams list
        int startPoint = ThreadLocalRandom.current().nextInt(1, 12 + 1); //Determines start point for colour selection
        ServerPlayerEntity storyTeller = src.getPlayer(); // Gets the person that called the command. Whoever called it is Storyteller

        // Set everyone else to be a player
        // Remove the storyTeller from the list of players. Remaining list is all players
        players.remove(storyTeller);
        System.out.println(players);

        if (storyTeller != null) {
            // Remove all tags before adding new ones
            resetPlayer(storyTeller);
            storyTeller.addCommandTag(STORYTELLER); //Add tag for story teller
            src.sendFeedback(() -> Text.literal("Storyteller is: " + storyTeller.getName().getString()), false);
            storyTeller.changeGameMode(GameMode.CREATIVE);
            scoreboard.addScoreHolderToTeam(storyTeller.getNameForScoreboard(), scoreboard.getTeam(TEAM_STORYTELLER));
        } else {
            src.sendFeedback(() -> Text.literal("Failed to find storyteller. Do not execute this command from the server window"), false);
            return 0;
        }
        System.out.println(players); //Debugging

        List<ServerPlayerEntity> spectators = null;
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
            src.sendFeedback(() -> Text.literal("No one online :("), false);
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
                player.setSpawnPoint(world.getRegistryKey(),mapCoords.get(assignedColour).homeInside,0,true,true); //Set player spawn point

                world.setBlockState(mapCoords.get(assignedColour).blockUnderLever, Blocks.GOLD_BLOCK.getDefaultState());
                //Places lever for player and add signs)
                switch (assignedColour) {
                    case "Black", "Cyan", "White":
                        placeLever(world, mapCoords.get(assignedColour).lever, Direction.EAST, Orientation.DOWN_EAST);
                        placeSign(world, mapCoords.get(assignedColour).chair, Direction.WEST, player.getName());
                        break;
                    case "Yellow", "Pink", "Grey":
                        placeLever(world, mapCoords.get(assignedColour).lever, Direction.SOUTH, Orientation.DOWN_SOUTH);
                        placeSign(world, mapCoords.get(assignedColour).chair, Direction.NORTH, player.getName());
                        break;
                    case "Orange", "Red", "Brown":
                        placeLever(world, mapCoords.get(assignedColour).lever, Direction.WEST, Orientation.DOWN_WEST);
                        placeSign(world, mapCoords.get(assignedColour).chair, Direction.EAST, player.getName());
                        break;
                    case "Purple", "Green", "Blue":
                        placeLever(world, mapCoords.get(assignedColour).lever, Direction.NORTH, Orientation.DOWN_NORTH);
                        placeSign(world, mapCoords.get(assignedColour).chair, Direction.SOUTH, player.getName());
                        break;
                }

                if (currentColourIndex == 12)
                    currentColourIndex = 1;
                else
                    currentColourIndex++;
            }
        }
        src.sendFeedback(() -> Text.literal(players.toString()), false);

        return 1;
    }

    private int onAddSpectator(CommandContext<ServerCommandSource> context) {
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        String specName = StringArgumentType.getString(context, "specName"); //Gets spectator player name from command
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
    }

    private int nightFalls(CommandContext<ServerCommandSource> context){
        ServerCommandSource src = context.getSource();
        ServerWorld world = context.getSource().getWorld();
        world.setTimeOfDay(18000L);
        //Remove all death_mark and accused tags from players
        for (ServerPlayerEntity p : players){
            p.removeCommandTag(ACCUSED);
            p.removeCommandTag(DEATH_FLAG);
        }

        src.sendFeedback(() -> NIGHT_MESSAGE, false);
        return 1;
    }

    private int startDay(CommandContext<ServerCommandSource> context){
        ServerCommandSource src = context.getSource();
        ServerWorld world = context.getSource().getWorld();
        world.setTimeOfDay(0L);
        for (ServerPlayerEntity p : players){
            updateVoteStatus(world,p);
        }
        highestVote = 0; //reset highest vote

        src.sendFeedback(() -> DAY_MESSAGE, false);
        //Send message about player death
        return 1;
    }

    private int onVoteLockIn(CommandContext<ServerCommandSource> context) {
        // remove redstone block
        ServerCommandSource src = context.getSource();
        src.sendFeedback(() -> Text.literal("converting... "), false);
        ServerWorld world = context.getSource().getWorld();
        int delayPerBlock = 25; // 1 second = 20 ticks
        final int[] totalVotes = new int[3]; // 0 is total, 1 is alive, 2 is ghost
        int voteThreshold = 0;
        int alivePlayers = getAliveCount();
        //List<ServerPlayerEntity> players = world.getPlayers(); //Need to remove storyteller and spectators
        src.sendFeedback(() -> Text.literal("Starting redstone removal..."), false);

        List<DelayedBlockSetter> taskList = new ArrayList<>();

        //Get vote threshold for alive players

        voteThreshold = (alivePlayers / 2) + (alivePlayers % 2);

        final ServerPlayerEntity accusedPlayer = getPlayerFromColour(ACCUSED);
        assert accusedPlayer != null;

        Runnable onAllDone = () -> {
            src.sendFeedback(() -> Text.literal("Starting count..."), false);
            int startColourIndex = 0;
            int ghostVotes = 0;
            int aliveVotes = 0;

            int count = 0;
            String accusedColour = getColourFromPlayer(accusedPlayer);
            startColourIndex = POSSIBLE_COLOURS.indexOf(accusedColour);

            for (int i = startColourIndex; count <= players.size(); i++) {
                String colour = POSSIBLE_COLOURS.get(i);
                BlockPos pos = mapCoords.get(colour).triggersLampPiston;
                BlockPos lockedVote = pos.up(2); //Position of locked in vote lamp

                int delay = count * delayPerBlock; //Add delay between vote locks
                taskList.add(new DelayedBlockSetter(world, pos, Blocks.AIR.getDefaultState(), delay)); //Set redstone block to air

                BlockState lockedVoteState = world.getBlockState(lockedVote);
                if (lockedVoteState.getBlock() == Blocks.WAXED_COPPER_BULB && lockedVoteState.get(Properties.LIT)) {
                    aliveVotes++;
                } else if (lockedVoteState.getBlock() == Blocks.SEA_LANTERN) {
                    ghostVotes++;
                    //Disable ghost vote code
                    removeGhostVote(world, colour);

                }
                if (i == 11){
                    i = -1; //Set to -1 so it continues to loop through colours at 0 when the loop resets.
                }

                count++;
            }
            totalVotes[0] = aliveVotes + ghostVotes;
            totalVotes[1] = aliveVotes;
            totalVotes[2] = ghostVotes;
        };
        TickScheduler.scheduleGroup(taskList, onAllDone);

        src.sendFeedback(() -> Text.literal("A total of " + totalVotes[1] + "votes were received, including " + totalVotes[2] + " ghost votes."), false);
        if ((totalVotes[0] > highestVote) && (totalVotes[0] >= voteThreshold)){
            highestVote = totalVotes[0]; //Change the highest vote to this vote
            //Mark player for execution
            accusedPlayer.addCommandTag(MARKED);
            highestVote = totalVotes[0];
            src.sendFeedback(accusedPlayer::getStyledDisplayName, false);
            src.sendFeedback(() -> Text.literal("has now been marked for execution"),false);
        }
        if (totalVotes[0] == highestVote){
            //Also remove all marked players
            ServerPlayerEntity markedPlayer = getPlayerFromColour(MARKED);
            if (markedPlayer != null) markedPlayer.removeCommandTag(MARKED);
        }
        accusedPlayer.removeCommandTag(ACCUSED);
        //Now update player highlighting



        // Callback after all block removals
//        Runnable onAllDone = () -> {
//            src.sendFeedback(() -> Text.literal("Starting count..."), false);
//            int ghostCount = 0;
//            int aliveCount = 0;
//            for (String coord : REDSTONE_BLOCKS) {
//                BlockPos base = convertLocation(coord);
//                BlockPos above = base.up(2); // 2 blocks above
//                BlockState aboveState = world.getBlockState(above);
//                if (aboveState.getBlock() == Blocks.WAXED_COPPER_BULB && aboveState.get(Properties.LIT)){
//                    aliveCount++;
//                } else if (aboveState.getBlock() == Blocks.SEA_LANTERN) {
//                    ghostCount++;
//                }
//            }
//            int countable = ghostCount;
//            int count2 = aliveCount;
//            src.sendFeedback(() -> Text.literal("Ghost:" + countable + "| alive:" + count2), false);
//        };
        return 1;
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");
        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTick); //(ServerWorld world) -> onWorldTick(world)
        TickScheduler.register();

        //Code that runs on server start.
        ServerLifecycleEvents.SERVER_STARTED.register(this::onGameInit);

        //Register commands
        // Register the botc init command, should now just run on startup
//        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
//            dispatcher.register(CommandManager.literal("botcinit").executes(this::onGameInit));
//        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("setupGame").executes(this::setupGame));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("importCSV").executes((context) -> {
                mapCoords = ImportExcelCoordinates.read(path); //Import coordinates for map from Excel sheet
                return 1;
            }));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("addSpectator").then(
                    CommandManager.argument("player_name", StringArgumentType.string())
                            .suggests(new PlayerSuggestionProvider())
                            .executes(this::onAddSpectator)
            ));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("startDay").executes(this::startDay));
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("nightFalls").executes(this::nightFalls));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("voteLockIn").executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Beginning vote lock in"), false);
                        onVoteLockIn(context);
                        context.getSource().sendFeedback(() -> Text.literal("Completed /onVoteLockIn."), false);
                        return 1;
                    }
            ));
        });


//		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
//			dispatcher.register(CommandManager.literal("smoothdaylight")
//					.then(CommandManager.argument("durationSeconds", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
//							.executes(context -> startSmoothTransition(context, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "durationSeconds"))))
//			);
//		});
    }
}