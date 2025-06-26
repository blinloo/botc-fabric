package com.botcfab;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

public class SelectionInventoryScreenHandler extends ScreenHandler {
    private final PlayerEntity player;
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);

    public SelectionInventoryScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(BotcFab.MY_SCREEN_HANDLER, syncId);
        this.player = playerInventory.player;

        // Fill with example items
        inventory.set(0, new ItemStack(Items.DIAMOND));
        inventory.set(1, new ItemStack(Items.GOLD_INGOT));
        inventory.set(2, new ItemStack(Items.EMERALD));

        for (int i = 0; i < inventory.size(); i++) {
            final int slotIndex = i;
            addSlot(new Slot(null, i, 0, 0) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return false;
                }

                @Override
                public ItemStack getStack() {
                    return inventory.getStack(slotIndex);
                }

                @Override
                public void setStack(ItemStack stack) {
                    inventory.setStack(slotIndex, stack);
                }

                @Override
                public void onTakeItem(PlayerEntity player, ItemStack stack) {
                    handleClick(slotIndex);
                }
            });
        }
    }

    private void handleClick(int index) {
        switch (index) {
            case 0 -> player.sendMessage(Text.literal("You clicked Diamond!"), false);
            case 1 -> player.sendMessage(Text.literal("You clicked Gold!"), false);
            case 2 -> player.sendMessage(Text.literal("You clicked Emerald!"), false);
            default -> player.sendMessage(Text.literal("Unknown item."), false);
        }//maybe closes when supplied null?
        player.openHandledScreen(null); //.closeHandledScreen(); // Close after selection
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}