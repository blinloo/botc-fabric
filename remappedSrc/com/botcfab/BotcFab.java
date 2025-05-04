package com.botcfab;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class BotcFab implements ModInitializer {
	public static final String MOD_ID = "botc-fab";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final String TEAMPLAYER = "teamPlayer";
	private static final String TEAMSTORY = "teamStoryteller";
	private static final String TEAMSPEC = "teamSpectator";
	private static final String STORYTELLER = "storyteller";
	private static final String PLAYER = "player";
	private static final String SPEC = "spectator";


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

		for (String tag: tags) {
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
		for (Team team: scoreboard.getTeams()) {
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
		} else{
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

								@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

//		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
//			dispatcher.register(CommandManager.literal("test_command").executes(context -> {
//				context.getSource().sendFeedback(() -> Text.literal("Called /test_command."), false);
//				dispatcher.
//				return 1;
//			}));
//		});

		// Register the botc init command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("botcinit").executes(this::onGameInit));
		});

//		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
//			dispatcher.register(CommandManager.literal("addSpectator")
//					.then(CommandManager.argument("specName", StringArgumentType.string())
//							.executes(this::onSetSpectator)));
//		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("addSpectator").then(
					CommandManager.argument("player_name", StringArgumentType.string())
							.suggests(new PlayerSuggestionProvider())
							.executes(this::onAddSpectator)
			));
		});
	}
}