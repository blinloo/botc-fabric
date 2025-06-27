package com.botcfab;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.item.ItemStack;

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
            if (!clicked.isEmpty() && slotIndex < 4) { //checks command doesn't need colour input
                // Run your procedure
                SelectionInventory.handleSelection((ServerPlayerEntity) clicker, slotIndex);

                // Optional: close screen (casts to ServerPlayerEntity)
                if (clicker instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.closeHandledScreen();
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