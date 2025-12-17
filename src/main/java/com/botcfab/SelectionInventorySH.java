package com.botcfab;

import com.botcfab.classes.BotcPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Objects;

import static com.botcfab.BotcFab.POSSIBLE_COLOURS;
import static com.botcfab.ItemUtils.*;
import static com.botcfab.SelectionInventory.handleSelectionCommandPlayerInput;
import static com.botcfab.SelectionInventory.openPlayerSelectMenu;

public class SelectionInventorySH extends GenericContainerScreenHandler {
    private final Inventory inventory;
    private final PlayerEntity player;
    private final String type;
    private final List<ItemStack> selectOptions;
    private final int prevIndex;

    public SelectionInventorySH(int syncId, PlayerInventory playerInventory, Inventory inventory, PlayerEntity player, String menuType, List<ItemStack> options, int prevCommand) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.player = player;
        this.type = menuType;
        this.selectOptions = options;
        this.prevIndex = prevCommand;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType,PlayerEntity clicker) {
        if (slotIndex >= 0 && slotIndex < inventory.size()) {
            ItemStack clicked = inventory.getStack(slotIndex);
            if (!clicked.isEmpty()) {
                if (Objects.equals(type, "command")){
                    if (selectOptions.contains(clicked)) { //For basic commands
                        SelectionInventory.handleSelectionCommand((ServerPlayerEntity) clicker, slotIndex);
                        if (clicker instanceof ServerPlayerEntity serverPlayer) {
                            serverPlayer.closeHandledScreen();
                            // close screen of player so they can't select again
                        }
                    } else {
                        this.setPreviousTrackedSlot(slotIndex, clicked.copy()); //Tracking previous command
                        openPlayerSelectMenu((ServerPlayerEntity) clicker, slotIndex); //Creates new window for colour selection, saving command selected.
                    }
                }
                //Handle player selection
                if (Objects.equals(type, "player")){ //If selection for players
                    BotcPlayer selected = SelectionInventory.handleSelectionColour(slotIndex); //Get player selected
                    if (selected != null) {
                        handleSelectionCommandPlayerInput((ServerPlayerEntity) clicker, prevIndex, selected);

                    }
                    if (clicker instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.closeHandledScreen();
                        // close screen of player so they can't select again
                    }

                    // Prevent item from being taken (clear the slot manually)
                    this.setPreviousTrackedSlot(slotIndex, clicked.copy()); // optional tracking
                    this.inventory.setStack(slotIndex, clicked); // reset original stack
                }
            }
        }

        super.onSlotClick(slotIndex, button, actionType, clicker);
    }
}