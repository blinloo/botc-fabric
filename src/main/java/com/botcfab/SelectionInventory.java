package com.botcfab;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Objects;

import static com.botcfab.BotcFab.POSSIBLE_COLOURS;
import static com.botcfab.ItemUtils.*;
import static com.botcfab.PlayerUtils.*;

public class SelectionInventory {
    static String selectedColour = "";
    static ServerPlayerEntity selectedPlayer = null;

    public static void openMenu(ServerPlayerEntity player, String menuType) {
        // Create inventory with 9 slots
        Inventory inventory = new SimpleInventory(27);

        // Add selectable items
        if (menuType.equals("commands")) {
            inventory.setStack(0, setCustomName(new ItemStack(Items.DIRT), Text.literal("Setup Game")));
            inventory.setStack(1, setCustomName(new ItemStack(Items.GRASS_BLOCK), Text.literal("Begin game and teleport players")));
            inventory.setStack(2, setCustomName(new ItemStack(Items.SUNFLOWER), Text.literal("Start day")));
            inventory.setStack(3, setCustomName(new ItemStack(Items.SNOWBALL), Text.literal("Night falls")));
            inventory.setStack(4, setCustomName(new ItemStack(Items.WAXED_COPPER_BULB), Text.literal("Lock in votes")));
            inventory.setStack(5, setCustomName(new ItemStack(Items.DARK_OAK_HANGING_SIGN), Text.literal("Begin execution of marked player")));
            inventory.setStack(6, setCustomName(new ItemStack(Items.ANDESITE_SLAB), Text.literal("Teleport players to chairs")));
            inventory.setStack(7, setCustomName(new ItemStack(Items.RED_BED), Text.literal("Teleport players to homes")));
            inventory.setStack(8, setCustomName(new ItemStack(Items.BELL), Text.literal("Start discussion time (6min)")));
            //Slots that require player input 21-26

            inventory.setStack(25, setCustomName(new ItemStack(Items.REDSTONE), Text.literal("Mark demon kill")));
            inventory.setStack(26, setCustomName(new ItemStack(Items.NETHER_STAR), Text.literal("Mark for revival")));
            int slot = 9; //9-20 is colour selection
            for (String colour : POSSIBLE_COLOURS) {
                ServerPlayerEntity p = getPlayerFromColour(colour, Objects.requireNonNull(player.getServer()));
                if (p != null) {//if player exists add their colour to slot
                    if (p == selectedPlayer){ //Mark selected player with glass
                        inventory.setStack(slot, setCustomName(getGlassFromColour(colour), p.getStyledDisplayName()));
                    }
                    else {
                        inventory.setStack(slot, setCustomName(getWoolFromColour(colour), p.getStyledDisplayName()));
                    }
                }
                slot++;
            }
        } else {
            inventory.setStack(0, new ItemStack(Items.DIAMOND));
            inventory.setStack(1, new ItemStack(Items.GOLD_INGOT));
            inventory.setStack(2, new ItemStack(Items.EMERALD));
        }

        NamedScreenHandlerFactory screenHandlerFactory = new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, playerEntity) ->
                    // Create a vanilla chest-like menu (27 slots)
                    new SelectionInventorySH(syncId,playerInventory,inventory, playerEntity)
                ,
                Text.literal("--==| Select a command |==--")
        );

        // Open the GUI
        player.openHandledScreen(screenHandlerFactory);
    }

    public static void handleSelectionCommand(ServerPlayerEntity player, int index) {
        switch (index) {
            //TODO put BOTC commands here
            case 0: //setup
                player.sendMessage(Text.literal("Setting up game"), false);
                BotcFab.setupGame(player.getCommandSource());
                break;
            case 1: //begin game
                player.sendMessage(Text.literal("Beginning game and teleporting players"), false);
                BotcFab.beginGame(player.getCommandSource());
                break;
            case 2: //start day
                player.sendMessage(Text.literal("Starting day"), false);
                BotcFab.startDay(player.getCommandSource());
                break;
            case 3: //night falls
                player.sendMessage(Text.literal("Ending day"), false);
                BotcFab.nightFalls(player.getCommandSource());
                break;
            case 4: //Lock in votes
                player.sendMessage(Text.literal("Locking in votes"), true);
                BotcFab.voteLockIn(player.getCommandSource());
                break;
            case 5: //Begin execution
                player.sendMessage(Text.literal("Starting execution"), true);
                BotcFab.beginExecution(player.getCommandSource());
                break;
            case 6: //Tp players to chair
                player.sendMessage(Text.literal("Teleporting to chairs"), true);
                PlayerUtils.teleportPlayers("home",player.getCommandSource().getServer());
                break;
            case 7: //Tp players to homes
                player.sendMessage(Text.literal("Teleporting to homes"), true);
                PlayerUtils.teleportPlayers("vote",player.getCommandSource().getServer());
                break;
            case 8: //Tp players to homes
                player.sendMessage(Text.literal("Started discussion time (6min)"), true);
                BotcFab.startDiscussionTime(player.getCommandSource());
                break;

            //TODO Finish adding commands
            default:
                player.sendMessage(Text.literal("Unknown selection."), false);
        }
    }

    public static void handleSelectionCommandPlayerInput(ServerPlayerEntity player, int index) {
        if (selectedPlayer != null) {
            switch (index) {
                case 21: //Instant kill player
                    player.sendMessage(Text.literal("Instant execution: " + selectedPlayer.getNameForScoreboard() + " (" + selectedColour), true);
                    PlayerUtils.executePlayer(selectedPlayer);
                    break;
                case 25: //Demon kill mark for night
                    player.sendMessage(Text.literal("Marked " + selectedPlayer.getNameForScoreboard() + " for demon kill (" + selectedColour+")"), true);
                    PlayerUtils.markPlayerDemonKill(selectedPlayer);
                    break;
                case 26: //Revive player for night
                    player.sendMessage(Text.literal("Added " + selectedPlayer.getNameForScoreboard() + " to revival list (" + selectedColour+")"), true);
                    PlayerUtils.markPlayerRevived(selectedPlayer);
                    break;
            }
        } else {
            player.sendMessage(Text.literal("Please select a valid colour from the row below first"), false);
        }
    }


    public static boolean handleSelectionColour(ServerPlayerEntity player, int index) {
        //TODO check colour select
        selectedColour = POSSIBLE_COLOURS.get(index);
        selectedPlayer = getPlayerFromColour(selectedColour, Objects.requireNonNull(player.getServer()));
        if (selectedPlayer != null) {
            player.sendMessage(Text.literal("Selected " + selectedColour + " for command"), false);
            return true;
        } else {
            player.sendMessage(Text.literal("No player with that colour, failed selection"), false);
            return false;
        }
    }
}