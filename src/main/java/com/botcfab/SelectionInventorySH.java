package com.botcfab;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.item.ItemStack;

import static com.botcfab.BotcFab.POSSIBLE_COLOURS;
import static com.botcfab.ItemUtils.*;
import static com.botcfab.SelectionInventory.handleSelectionCommandPlayerInput;

public class SelectionInventorySH extends GenericContainerScreenHandler {
    private final Inventory inventory;
    private final PlayerEntity player;

    public SelectionInventorySH(int syncId, PlayerInventory playerInventory, Inventory inventory, PlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.player = player;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType,PlayerEntity clicker) {
        if (slotIndex >= 0 && slotIndex < inventory.size()) {
            ItemStack clicked = inventory.getStack(slotIndex);
            if (!clicked.isEmpty()) {
                //Handle selection
                if (slotIndex >= 9 && slotIndex <= 20){ //If selection is within colour area
                    //TODO set selected colour to glass
                    // -also reset old selection to wool
                    if (SelectionInventory.handleSelectionColour((ServerPlayerEntity) clicker, slotIndex-9)) { //-9 to index to match with colour indexes
                        String c = POSSIBLE_COLOURS.get(slotIndex-9);
                        this.setPreviousTrackedSlot(slotIndex, clicked.copy());
                        this.inventory.setStack(slotIndex,getGlassFromColour(c));
                    }
                } else {
                    if (slotIndex >= 21) { //For commands that need player selection
                        handleSelectionCommandPlayerInput((ServerPlayerEntity) clicker, slotIndex);
                    } else {
                        SelectionInventory.handleSelectionCommand((ServerPlayerEntity) clicker, slotIndex);
                    }
                    // close screen of player so they can't select again
                    if (clicker instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.closeHandledScreen();
                    }
                    // Prevent item from being taken
                    // Prevent item from being taken (clear the slot manually)
                    this.setPreviousTrackedSlot(slotIndex, clicked.copy()); // optional tracking
                    this.inventory.setStack(slotIndex, clicked); // reset original stack
                }
                //TODO add separate handle procedure for commands with player input to avoid duplicate checks.
            }
        }

        super.onSlotClick(slotIndex, button, actionType, clicker);
    }
}