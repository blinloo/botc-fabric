package com.botcfab;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.item.ItemStack;

import java.util.Objects;

import static com.botcfab.BotcFab.POSSIBLE_COLOURS;
import static com.botcfab.PlayerUtils.getPlayerFromColour;
import static com.botcfab.ItemUtils.*;

public class SelectionInventorySH extends GenericContainerScreenHandler {
    private final Inventory inventory;
    private final PlayerEntity player;
    private String colourSelected;

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
                if (slotIndex >= 9){ //If selection is within colour area
                    SelectionInventory.handleSelection((ServerPlayerEntity) clicker, slotIndex-9, "colour"); //-9 to index to match with colour indexes
                    //TODO set selected colour to glass
                    // -also reset old selection to wool
                    if (SelectionInventory.handleSelectionColour((ServerPlayerEntity) clicker, slotIndex-9, "colour")) {
                        String c = POSSIBLE_COLOURS.get(slotIndex-9);
                        this.inventory.setStack(slotIndex,getGlassFromColour(c));
                    }
                } else {
                    SelectionInventory.handleSelection((ServerPlayerEntity) clicker, slotIndex, "command");

                    // close screen of player so they can't select again
                    if (clicker instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.closeHandledScreen();
                    }
                }

                // Prevent item from being taken
                // Prevent item from being taken (clear the slot manually)
                this.setPreviousTrackedSlot(slotIndex, clicked.copy()); // optional tracking
                this.inventory.setStack(slotIndex, clicked); // reset original stack
            }
        }

        super.onSlotClick(slotIndex, button, actionType, clicker);
    }
}