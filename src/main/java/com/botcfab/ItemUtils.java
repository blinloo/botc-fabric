package com.botcfab;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ItemUtils {
    /**
     * Sets a custom display name on an ItemStack.
     *
     * @param item The ItemStack to rename.
     * @param name The Text component to set as the display name.
     * @return The same ItemStack with the name applied.
     */
    public static ItemStack setCustomName(ItemStack item, Text name) {
        if (item == null || name == null) {
            throw new IllegalArgumentException("ItemStack and name must not be null");
        }

        // Apply the CustomNameComponent via components
        item.set(DataComponentTypes.CUSTOM_NAME, name);

        return item;
    }

    public static ItemStack setPlayerHead(ItemStack item, ServerPlayerEntity owner) {
        if (item == null || owner == null || item.getItem() != Items.PLAYER_HEAD) {
            throw new IllegalArgumentException("ItemStack and name must not be null");
        }

        ProfileComponent profile = new ProfileComponent(owner.getGameProfile());
        // Apply the CustomNameComponent via components
        item.set(DataComponentTypes.CUSTOM_NAME, owner.getStyledDisplayName());
        item.set(DataComponentTypes.PROFILE, profile);

        return item;
    }
}