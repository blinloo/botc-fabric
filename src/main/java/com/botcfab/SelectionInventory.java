package com.botcfab;

import com.botcfab.classes.BotcPlayer;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.botcfab.BotcFab.*;
import static com.botcfab.ItemUtils.*;
import static com.botcfab.PlayerUtils.*;

public class SelectionInventory {

    public static void openMenu(ServerPlayerEntity player) {
        // Create inventory with 9 slots
        Inventory inventory = new SimpleInventory(27);
        List<ItemStack> basicCommands = Arrays.asList(
                setCustomName(new ItemStack(Items.DIRT), Text.literal("Setup Game")),
                setCustomName(new ItemStack(Items.GRASS_BLOCK), Text.literal("Begin game and teleport players")),
                setCustomName(new ItemStack(Items.SUNFLOWER), Text.literal("Start day")),
                setCustomName(new ItemStack(Items.SNOWBALL), Text.literal("Night falls")),
                setCustomName(new ItemStack(Items.WAXED_COPPER_BULB), Text.literal("Lock in votes")),
                setCustomName(new ItemStack(Items.DARK_OAK_HANGING_SIGN), Text.literal("Begin execution of marked player")),
                setCustomName(new ItemStack(Items.ANDESITE_SLAB), Text.literal("Teleport players to chairs")),
                setCustomName(new ItemStack(Items.RED_BED), Text.literal("Teleport players to homes")),
                setCustomName(new ItemStack(Items.BELL), Text.literal("Start discussion time (6min)")),
                setCustomName(new ItemStack(Items.RED_WOOL), Text.literal("Reset night flags (kills/revives)"))
        );
        // Add selectable items slots 0-8
        int index = 0;
        for (ItemStack command:basicCommands){
            inventory.setStack(index,command);
            index++;
        }
        //Slots that require player input 18-26
        List<ItemStack> playerCommands = Arrays.asList(
                setCustomName(new ItemStack(Items.PLAYER_HEAD), Text.literal("Instant kill (no execution)")),
                setCustomName(new ItemStack(Items.DARK_OAK_SIGN), Text.literal("Execute but don't kill")),
                new ItemStack(Items.AIR),
                setCustomName(new ItemStack(Items.SKELETON_SKULL), Text.literal("Instant execute")),
                setCustomName(new ItemStack(Items.LIGHT_WEIGHTED_PRESSURE_PLATE), Text.literal("Instant revive")),
                new ItemStack(Items.AIR),
                setCustomName(new ItemStack(Items.REDSTONE), Text.literal("Mark demon kill")),
                setCustomName(new ItemStack(Items.NETHER_STAR), Text.literal("Mark for revival"))
        );
        index = 18;
        for (ItemStack command:playerCommands){
            inventory.setStack(index,command);
            index++;
        }

        NamedScreenHandlerFactory screenHandlerFactory = new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, playerEntity) ->
                    // Create a vanilla chest-like menu (27 slots)
                    new SelectionInventorySH(syncId,playerInventory,inventory, playerEntity, "command", basicCommands,-1)
                ,
                Text.literal("--==| Select a command |==--")
        );

        // Open the GUI
        player.openHandledScreen(screenHandlerFactory);
    }

    public static void openPlayerSelectMenu(ServerPlayerEntity player, int prevIndex) {
        // Create inventory with 27 slots
        Inventory inventory = new SimpleInventory(27);
        List<ItemStack> players = new ArrayList<>(List.of());
        int slot = 0;
        for (String colour : currentGame.getColours()) {
            if (currentGame != null) {
                BotcPlayer p = currentGame.getPlayerAtColour(colour);
                if (p != null) {//if player exists add their colour to slot
                    ItemStack playerHead = setPlayerHead(p.getPlayer(),colour);
                    players.add(playerHead);
                    inventory.setStack(slot, playerHead); //Set to player head
                } else {
                    inventory.setStack(slot, new ItemStack(Items.BARRIER));
                }
                slot++;
            }
        }

        NamedScreenHandlerFactory screenHandlerFactory = new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, playerEntity) ->
                        // Create a vanilla chest-like menu (27 slots)
                        new SelectionInventorySH(syncId,playerInventory,inventory, playerEntity, "player",players,prevIndex)
                ,
                Text.literal("--==| Select a player |==--")
        );

        // Open the GUI
        player.openHandledScreen(screenHandlerFactory);
    }

    public static void handleSelectionCommand(ServerPlayerEntity player, int index) {
        switch (index) {
            //TODO put BOTC commands here
            case 0: //setup
                player.sendMessage(Text.literal("Setting up game")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), false);
                setupGame(player.getCommandSource());
                break;
            case 1: //begin game
                player.sendMessage(Text.literal("Beginning game and teleporting players")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), false);
                beginGame(player.getCommandSource());
                break;
            case 2: //start day
                player.sendMessage(Text.literal("Starting day")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), false);
                startDay(player.getCommandSource());
                break;
            case 3: //night falls
                player.sendMessage(Text.literal("Ending day")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), false);
                nightFalls(player.getCommandSource());
                break;
            case 4: //Lock in votes
                player.sendMessage(Text.literal("Locking in votes")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                voteLockIn(player.getCommandSource());
                break;
            case 5: //Begin execution
                player.sendMessage(Text.literal("Starting execution")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                beginExecution(player.getCommandSource());
                break;
            case 6: //Tp players to chair
                player.sendMessage(Text.literal("Teleporting to chairs")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                teleportPlayers("vote",currentGame);
                break;
            case 7: //Tp players to homes
                player.sendMessage(Text.literal("Teleporting to home")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                teleportPlayers("home",currentGame);
                break;
            case 8: //Tp players to homes
                player.sendMessage(Text.literal("Started discussion time (6min)")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                startDiscussionTime(player.getCommandSource());
                break;
            case 9: //Clear marked players
                player.sendMessage(Text.literal("Cleared all flags (no players marked for kills/revives)")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                currentGame.resetGameFlags();
                break;
            default:
                player.sendMessage(Text.literal("Unknown selection."), false);
        }
    }

    public static void handleSelectionCommandPlayerInput(ServerPlayerEntity player, int index, BotcPlayer selected) {
        if (selected != null) {
            String selectedName = selected.getNameString();
            String colour = selected.getColour();
            switch (index) {
                case 18: //Instant kill player
                    player.sendMessage(Text.literal("Instant kill: " + selectedName + " (" + colour)
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                    killPlayer(selected);
                    break;
                case 19: //Instant execute but no kill player
                    player.sendMessage(Text.literal("Instant execution (no kill): " + selectedName + " (" + colour)
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                    selected.changeSurviveExecution(true);
                    executePlayer(selected);
                    break;
                case 21: //Instant execute player
                    player.sendMessage(Text.literal("Instant execution: " + selectedName + " (" + colour )
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                    executePlayer(selected);
                    break;
                case 22: //Instant revive player
                    player.sendMessage(Text.literal("Instant revival: " + selectedName + " (" + colour)
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), true);
                    revivePlayer(selected);
                    break;
                case 24: //Demon kill mark for night
                    player.sendMessage(Text.literal("Marked " + selectedName + " for demon kill (" + colour+")")
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), false);
                    markPlayerDemonKill(selected);
                    break;
                case 25: //Revive player for night
                    player.sendMessage(Text.literal("Added " + selectedName + " to revival list (" + colour+")")
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), false);
                    markPlayerRevived(selected);
                    break;
                case 26: //Mark accused
                    accusePlayer(selected,currentGame);
            }
        } else {
            player.sendMessage(Text.literal("Please select a valid colour")
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY)), false);
        }
    }


    public static BotcPlayer handleSelectionColour(int index) {
        String selectedColour = currentGame.getColours().get(index);
        return currentGame.getPlayerAtColour(selectedColour);
    }
}