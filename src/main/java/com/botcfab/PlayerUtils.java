package com.botcfab;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.botcfab.BotcFab.*;
import static com.botcfab.ItemUtils.*;

public class PlayerUtils {

    static void resetPlayer(@NotNull PlayerEntity player) { //Removes all game based tags from a player
        for (String tag : ALL_TAGS) {
            player.removeCommandTag(tag);
        }
        for (String tag : POSSIBLE_COLOURS) {
            player.removeCommandTag(tag);
        }
    }

    static void removeTagAllPlayers(String tag, MinecraftServer srv){
        List<ServerPlayerEntity> playerList = srv.getPlayerManager().getPlayerList();
        if (playerList != null) {
            for (ServerPlayerEntity p : playerList) {
                p.removeCommandTag(tag);
            }
        }
    }

    static String getColourFromPlayer(ServerPlayerEntity player){
        Set<String> tags = player.getCommandTags();
        for (String i : POSSIBLE_COLOURS){
            if (tags.contains(i)){
                return i;
            }
        }
        return "";
    }

    static ServerPlayerEntity getPlayerFromColour(String colour, MinecraftServer srv){
        List<ServerPlayerEntity> playerList = srv.getPlayerManager().getPlayerList();
        if (playerList != null){
            for (ServerPlayerEntity p : playerList) {
                Set<String> tags = p.getCommandTags();
                if (tags.contains(colour)) {
                    return p;
                }
            }
        }
        return null;
    }

    static int getTagCount(String tag, MinecraftServer srv){
        List<ServerPlayerEntity> playerList = srv.getPlayerManager().getPlayerList();
        int count = 0;
        if (playerList != null) {
            for (ServerPlayerEntity p : playerList) {
                Set<String> tags = p.getCommandTags();
                if (tags.contains(tag))
                    count++;
            }
        }
        return count;
    }

    static void accusePlayer(ServerPlayerEntity player){
        removeTagAllPlayers(ACCUSED, Objects.requireNonNull(player.getServer()));
        player.addCommandTag(ACCUSED);
    }

    static void markPlayerDemonKill(ServerPlayerEntity player){
        Set<String> tags = player.getCommandTags();
        if (!tags.contains(GHOST) && !tags.contains(DEAD)) { //If player already dead, do not tag for announce.
            player.addCommandTag(DEATH_FLAG);
        }
    }

    static void markPlayerRevived(ServerPlayerEntity player){
        Set<String> tags = player.getCommandTags();
        if (!tags.contains(ALIVE)) { //If player still alive do not mark for revival
            player.addCommandTag(REVIVE_FLAG);
        }
    }

    static void killPlayer(ServerPlayerEntity player){
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

    static void revivePlayer(ServerPlayerEntity player){
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

    static void executePlayer(ServerPlayerEntity player){
        player.addCommandTag(CURRENT_EXECUTEE);
        ServerWorld world = player.getServerWorld();
        MinecraftServer srv = world.getServer();
        ServerScoreboard scoreboard = srv.getScoreboard();
        playersLockedToSeats = false;
        executionInProgress = true;
        switch (mapSelected) {
            case DEFAULT_MAP:
                tp(player, EXECUTE_POS.down(1)); //tp executed player to the block
                world.setBlockState(EXECUTE_POS.up(250), Blocks.ANVIL.getDefaultState()); //create anvil 50 blocks up above execution
                break;
            case SCHOOL_MAP:
                //TODO open pit here and teleport player
                //Teleport player next to pit so they can jump in for fun, if they not already in range
                Vec3d murderZone = new Vec3d(EXECUTE_POS.getX()+0.5, EXECUTE_POS.getY(), EXECUTE_POS.getZ()+0.5); //Get centre of block
                double distance = player.getPos().squaredDistanceTo(murderZone); //should work with vec3d now
                if (distance > 6) {
                    tp(player,EXECUTE_POS.north(3));
                }

                //Open hole
                world.setBlockState(new BlockPos(454,3,156), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(455,3,156), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(456,3,156), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(454,3,157), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(455,3,157), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(456,3,157), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(454,3,158), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(455,3,158), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(456,3,158), Blocks.AIR.getDefaultState());

                //particles
                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.WHITE_WOOL.getDefaultState()), EXECUTE_POS.getX()+0.5, EXECUTE_POS.getY()-0.2, EXECUTE_POS.getZ()+0.5, 15, 1.5, 0.8, 1.5, 0.1);
                break;
        }
        createOrSetAliveDisplay(scoreboard, srv);
    }

    static void updateVoteStatus(ServerWorld world, ServerPlayerEntity player){
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
                case SCHOOL_MAP:
                    placeLever(world, mapCoords.get(playerColour).lever, Direction.NORTH, BlockFace.FLOOR);
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

    static int onAddSpectator(CommandContext<ServerCommandSource> context) {
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

        src.sendFeedback(() -> Text.literal("Called /addSpectator with value 1 = %s ".formatted(specName)), false);
        return 1;
    }

    static MutableText getPlayerOrder(MinecraftServer srv){
        MutableText PlayerOrderMessage = Text.literal("Player Order: \n");
        for (String c:POSSIBLE_COLOURS){
            ServerPlayerEntity p = getPlayerFromColour(c, srv);
            if (p != null){
                Set<String> tags = p.getCommandTags();
                if (tags.contains(DEAD) || tags.contains(GHOST)) {
                    PlayerOrderMessage
                            .append(Text.literal("⬛ ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))  // Square with colour
                            .append(Text.literal("💀 "))
                            .append(p.getStyledDisplayName().copy().setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GRAY)))) //Player name grey for dead
                            .append(Text.literal(" ⬛").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))
                            .append(Text.literal("\n")); //New line
                } else {
                    PlayerOrderMessage
                            .append(Text.literal("⬛ ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))  // Square with colour
                            .append(p.getStyledDisplayName().copy()) //Player name
                            .append(Text.literal(" ⬛").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))
                            .append(Text.literal("\n")); //New line
                }
            }
        }
        return PlayerOrderMessage;
    }

    static void sendMessageToPlayers(Text messageText, List<ServerPlayerEntity> playerList){
        for (ServerPlayerEntity p:playerList){
            p.sendMessage(messageText);
        }
    }


    static void showTitle(ServerPlayerEntity player, Text titleText){
        player.networkHandler.sendPacket(new TitleS2CPacket(titleText));
    }

    static void tp(ServerPlayerEntity player, BlockPos destination){ //teleports player to BlockPos by converting to x,y,z
        player.teleport(destination.getX()+0.5,destination.getY(),destination.getZ()+0.5,false); //0.5 for centre of block
    }

    static void teleportPlayers(String location, MinecraftServer srv){
        //location either "home" or "vote"
        List<ServerPlayerEntity> playerList = srv.getPlayerManager().getPlayerList();
        String colour;
        ServerWorld world = srv.getOverworld();
        switch (location){
            case "home","house","dorm":
                for (ServerPlayerEntity p : playerList) {
                    colour = getColourFromPlayer(p);
                    if (!Objects.equals(colour, "")) {
                        tp(p, mapCoords.get(colour).homeInside); //teleport player to coords
                    }
                }
                break;
            case "vote","chair","town":
                for (ServerPlayerEntity p : playerList) {
                    colour = getColourFromPlayer(p);
                    if (!Objects.equals(colour, "")) {
                        tp(p, mapCoords.get(colour).chair.up(1)); //Needs to go up 1 so space isn't occupied
                    }
                }
                //Leave vc groups for town square
                world.setBlockState(TOWN_VC_TRIGGER_POS, Blocks.REDSTONE_BLOCK.getDefaultState());
                break;
            case "legion","evil":
                for (ServerPlayerEntity p : playerList) {
                    Set<String> tags = p.getCommandTags();
                    if (tags.contains(LEGION)) {
                        tp(p, EVIL_ROOM_POS); //Needs to go up 1 so space isn't occupied
                    }
                }
                break;
        }
    }

    static int leaveMinigames(CommandContext<ServerCommandSource> context){ //Teleports player back to home
        ServerPlayerEntity player = context.getSource().getPlayer(); //might need to change to take input player
        if (player != null) {
            String colour = getColourFromPlayer(player);
            if (!Objects.equals(colour, "")) {
                tp(player, mapCoords.get(colour).homeInside);
            } else {
                return 0;
            }
            return 1;
        } else {
            return 0;
        }
    }

    static int gotoMinigames(ServerPlayerEntity player){ //Teleports player to minigames room
        if (player != null) {
            tp(player, MINIGAMES_POS);
            return 1;
        } else {
            return 0;
        }
    }
}
