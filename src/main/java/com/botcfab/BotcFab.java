package com.botcfab;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import net.minecraft.state.property.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BotcFab implements ModInitializer {
    public static final String MOD_ID = "botc-fab";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String TEAMPLAYER = "teamPlayer";
    private static final String TEAMSTORY = "teamStoryteller";
    private static final String TEAMSPEC = "teamSpectator";
    private static final String STORYTELLER = "storyteller";
    private static final String PLAYER = "player";
    private static final String SPEC = "spectator";
    private static final List<String> LEVER_LOCATIONS = Arrays.asList("-194,-26,35", "-194,-26,32", "-194,-26,29", "-194,-26,26", "-194,-26,23");
    private static final List<String> REDSTONE_BLOCKS = Arrays.asList(
            "-196,-29,35", "-196,-29,32", "-196,-29,29", "-196,-29,26", "-196,-29,23");
//    private static final List<String> REDSTONE_BLOCKS = Arrays.asList("2,-32,-8",
//        "5,-32,-9",
//        "8,-32,-8",
//        "11,-32,-5",
//        "12,-32,-2",
//        "11,-32,1",
//        "8,-32,4",
//        "5,-32,5",
//        "2,-32,4",
//        "-1,-32,1",
//        "-2,-32,-2",
//        "-1,-32,-5"
//    );
    //
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
    private BlockPos convertLocation(String coords) {
        List<Integer> converted = new ArrayList<>();
        for (String s : coords.split(",")) {
            converted.add(Integer.parseInt(s));
        }
        return new BlockPos(converted.get(0), converted.get(1), converted.get(2));
    }

    private Team getOrCreateTeam(@NotNull ServerScoreboard scoreboard, String teamName) {
        // Check if the team already exists
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            // Create the team if it doesn't exist
            team = scoreboard.addTeam(teamName);
            System.out.println("Created new team: " + teamName);
        } else {
            System.out.println("Team already exists: " + teamName);
        }

        return team;
    }

    private void removeAllTags(@NotNull PlayerEntity player) {
        Set<String> tags = player.getCommandTags();

        for (String tag : tags) {
            player.removeCommandTag(tag);
        }
    }

    private int onGameInit(CommandContext<ServerCommandSource> context) {
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        List<ServerPlayerEntity> players = playerMgr.getPlayerList();
        ServerScoreboard scoreboard = srv.getScoreboard();
        List<String> allTeams = Arrays.asList(TEAMPLAYER, TEAMSTORY, TEAMSPEC);

        // Create the various teams
        for (String teamName : allTeams) {
            getOrCreateTeam(scoreboard, teamName);
        }

        // Make sure all players have their nametags off
        for (Team team : scoreboard.getTeams()) {
            team.setNameTagVisibilityRule(Team.VisibilityRule.NEVER);
        }


        // Gets the person that called the command. Whoever called it is Storyteller
        ServerPlayerEntity storyTeller = src.getPlayer();

        if (storyTeller != null) {
            // Remove all tags before adding new ones
            removeAllTags(storyTeller);
            storyTeller.addCommandTag(STORYTELLER);
            src.sendFeedback(() -> Text.literal("Storyteller is: " + storyTeller.getName().getString()), false);
            storyTeller.changeGameMode(GameMode.CREATIVE);
            scoreboard.addScoreHolderToTeam(storyTeller.getNameForScoreboard(), scoreboard.getTeam(TEAMSTORY));
        } else {
            src.sendFeedback(() -> Text.literal("Failed to find storyteller. Do not execute this command from the server window"), false);
            return 0;
        }
        System.out.println(players);


        // Set everyone else to be a player
        // Remove the storyTeller from the list of players. Remaining list is all players
        players.remove(storyTeller);
        System.out.println(players);
        if (players.isEmpty()) {
            src.sendFeedback(() -> Text.literal("No one online :("), false);
        } else {
            for (ServerPlayerEntity player : players) {
                removeAllTags(player);
                player.addCommandTag(PLAYER);
                player.addCommandTag("alive");
                player.changeGameMode(GameMode.ADVENTURE);
                playerMgr.removeFromOperators(player.getGameProfile());
                scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), scoreboard.getTeam(TEAMPLAYER));
            }
        }
        return 1;
    }

    private int onAddSpectator(CommandContext<ServerCommandSource> context) {
        ServerCommandSource src = context.getSource();
        MinecraftServer srv = src.getServer();
        PlayerManager playerMgr = srv.getPlayerManager();
        String specName = StringArgumentType.getString(context, "specName");
        ServerPlayerEntity specTarget = playerMgr.getPlayer(specName);
        ServerScoreboard scoreboard = srv.getScoreboard();
        if (specTarget == null) {

            System.out.println("Couldn't find player");
            return 0;
        }

        removeAllTags(specTarget);
        specTarget.addCommandTag(SPEC);
        scoreboard.addScoreHolderToTeam(specTarget.getNameForScoreboard(), scoreboard.getTeam(TEAMSPEC));
        specTarget.changeGameMode(GameMode.SPECTATOR);
        src.sendFeedback(() -> Text.literal("Called /addSpectator with value 1 = %s ".formatted(specName)), false);
        return 1;
    }

    private void onWorldTick(ServerWorld world) {
        // check lever flips for vote function
        for (int i = 0; i < LEVER_LOCATIONS.size(); i++) {
            BlockPos leverPos = convertLocation(LEVER_LOCATIONS.get(i)); // Get the BlockPos of the lever
            BlockState leverState = world.getBlockState(leverPos); // Get the block state at that position
            // Check if the block at this position is a lever and it's powered
            if (leverState.isOf(Blocks.LEVER)) {
                BlockPos redBlock = convertLocation(REDSTONE_BLOCKS.get(i));
                // Weathered copper bulb is always going to be two blocks above the redstone block
                BlockPos voteIndicatorPos = redBlock.up(3);
                BlockState voteIndicatorState = world.getBlockState(voteIndicatorPos);
                if (voteIndicatorState.contains(Properties.LIT)) {
                    BlockState updatedBulbState = voteIndicatorState.with(Properties.LIT, leverState.get(Properties.POWERED));
                    world.setBlockState(voteIndicatorPos, updatedBulbState, 3);
                }
                if (voteIndicatorState.isOf(Blocks.WAXED_OXIDIZED_COPPER) || voteIndicatorState.isOf(Blocks.SEA_LANTERN)) {
                    if (leverState.get(Properties.POWERED)) {
                        world.setBlockState(voteIndicatorPos, Blocks.SEA_LANTERN.getDefaultState(), 3);
                    } else {
                        world.setBlockState(voteIndicatorPos, Blocks.WAXED_OXIDIZED_COPPER.getDefaultState(), 3);
                    }
                }

            }
        }
    }

    private int onVoteLockIn(CommandContext<ServerCommandSource> context) {
        // remove redstone block
        ServerCommandSource src = context.getSource();
        src.sendFeedback(() -> Text.literal("converting... "), false);
        ServerWorld world = context.getSource().getWorld();
        int delayPerBlock = 40; // 4 seconds = 80 ticks
        List<ServerPlayerEntity> players = world.getPlayers();
        src.sendFeedback(() -> Text.literal("Starting redstone removal..."), false);

        List<DelayedBlockSetter> taskList = new ArrayList<>();

        for (int i = 0; i < REDSTONE_BLOCKS.size(); i++) {
            int iteration = i;
            BlockPos pos = convertLocation(REDSTONE_BLOCKS.get(iteration));
            int delay = iteration * delayPerBlock;

            taskList.add(new DelayedBlockSetter(world, pos, Blocks.AIR.getDefaultState(), delay));
        }

        // Callback after all block removals
        Runnable onAllDone = () -> {
            src.sendFeedback(() -> Text.literal("Starting count..."), false);
            int ghostCount = 0;
            int aliveCount = 0;
            for (String coord : REDSTONE_BLOCKS) {
                BlockPos base = convertLocation(coord);
                BlockPos above = base.up(2); // 2 blocks above
                BlockState aboveState = world.getBlockState(above);
                if (aboveState.getBlock() == Blocks.WAXED_COPPER_BULB && aboveState.get(Properties.LIT)){
                    aliveCount++;
                } else if (aboveState.getBlock() == Blocks.SEA_LANTERN) {
                    ghostCount++;
                }
            }
            int countable = ghostCount;
            int count2 = aliveCount;
            src.sendFeedback(() -> Text.literal("Ghost:" + countable + "| alive:" + count2), false);
        };
        TickScheduler.scheduleGroup(taskList, onAllDone);


        return 1;
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");
        ServerTickEvents.END_WORLD_TICK.register((ServerWorld world) -> onWorldTick(world));
        TickScheduler.register();
        // Register the botc init command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("botcinit").executes(this::onGameInit));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("addSpectator").then(
                    CommandManager.argument("player_name", StringArgumentType.string())
                            .suggests(new PlayerSuggestionProvider())
                            .executes(this::onAddSpectator)
            ));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("voteLockIn").executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Calling /onVoteLockIn."), false);
                        onVoteLockIn(context);
                        context.getSource().sendFeedback(() -> Text.literal("Called /onVoteLockIn."), false);
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