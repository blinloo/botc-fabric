package com.botcfab;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SelectionInventory {

    public static void openMenu(ServerPlayerEntity player) {
        // Create inventory with 9 slots
        Inventory inventory = new SimpleInventory(27);

        // Add selectable items
        inventory.setStack(0, new ItemStack(Items.DIAMOND));
        inventory.setStack(1, new ItemStack(Items.GOLD_INGOT));
        inventory.setStack(2, new ItemStack(Items.EMERALD));

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
            case 0 -> player.sendMessage(Text.literal("You selected Diamond!"), false);
            case 1 -> player.sendMessage(Text.literal("You selected Gold Ingot!"), false);
            case 2 -> player.sendMessage(Text.literal("You selected Emerald!"), false);
            default -> player.sendMessage(Text.literal("Unknown selection."), false);
        }
    }
}