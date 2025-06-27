package com.botcfab;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.botcfab.ItemUtils.setCustomName;

public class SelectionInventory {

    public static void openMenu(ServerPlayerEntity player, String menuType) {
        // Create inventory with 9 slots
        Inventory inventory = new SimpleInventory(27);

        // Add selectable items
        switch (menuType) {
            case "commands":
                inventory.setStack(0, setCustomName(new ItemStack(Items.DIRT),Text.literal("Setup Game")));
                inventory.setStack(1, setCustomName(new ItemStack(Items.GRASS_BLOCK),Text.literal("Begin game and teleport players")));
                inventory.setStack(2, setCustomName(new ItemStack(Items.SUNFLOWER),Text.literal("Start day")));
                inventory.setStack(3, setCustomName(new ItemStack(Items.SNOWBALL),Text.literal("Night falls")));
                break;
            case "colours":
                List<ServerPlayerEntity> playerList = Objects.requireNonNull(player.getServer()).getPlayerManager().getPlayerList();
                int slot = 9;
                for (ServerPlayerEntity p : playerList){
                    inventory.setStack(slot, new ItemStack(Items.PLAYER_HEAD).set());
                    slot++;
                }
                inventory.setStack(9, new ItemStack(Items.BLACK_WOOL));
                inventory.setStack(10, new ItemStack(Items.YELLOW_WOOL));
                inventory.setStack(11, new ItemStack(Items.ORANGE_WOOL));
                inventory.setStack(12, new ItemStack(Items.PINK_WOOL));
                inventory.setStack(13, new ItemStack(Items.RED_WOOL));
                inventory.setStack(14, new ItemStack(Items.PURPLE_WOOL));
                inventory.setStack(15, new ItemStack(Items.BROWN_WOOL));
                inventory.setStack(16, new ItemStack(Items.GREEN_WOOL));
                inventory.setStack(17, new ItemStack(Items.WHITE_WOOL));
                inventory.setStack(18, new ItemStack(Items.BLUE_WOOL));
                inventory.setStack(19, new ItemStack(Items.CYAN_WOOL));
                inventory.setStack(20, new ItemStack(Items.GRAY_WOOL));
                break;
            default:
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

    public static void handleSelection(ServerPlayerEntity player, int index) {
        switch (index) {
            //TODO put BOTC commands here
            case 0: //setup
                player.sendMessage(Text.literal("Setting up game"), false);
                BotcFab.setupGame(player.getCommandSource());
                break;
            case 1: //begin game
                player.sendMessage(Text.literal("You selected Gold Ingot!"), false);
                break;
            case 2: //start day
                player.sendMessage(Text.literal("You selected Emerald!"), false);
                break;
            case 3: //night falls
                player.sendMessage(Text.literal("You selected Emerald!"), false);
                break;
            default: player.sendMessage(Text.literal("Unknown selection."), false);
        }
    }
}