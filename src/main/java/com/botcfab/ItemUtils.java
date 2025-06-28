package com.botcfab;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.List;

import static com.botcfab.BotcFab.POSSIBLE_COLOURS;
import static com.mojang.text2speech.Narrator.LOGGER;

public class ItemUtils {
    static final List<Integer> COLOUR_HEX = Arrays.asList(0x000000, 0xFFEE00, 0xFF8D00, 0xFFAFC7, 0xE50000,
            0x760088, 0x613915, 0x028121, 0xFFFFFF, 0x004CFF, 0x73D7EE, 0x888888); //Colour picked from progress pride flag
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

    static void placeLever(ServerWorld world, BlockPos pos, Direction facing, BlockFace wallSide) {
        if (world == null || world.isClient) return;

        LeverBlock lever = (LeverBlock) Blocks.LEVER;
        LOGGER.info("Setting lever state...");
        // Create the desired lever block state
        var state = lever.getDefaultState()
                //.with(Properties.HORIZONTAL_FACING, facing)
                .with(LeverBlock.FACING, facing)
                .with(LeverBlock.FACE, wallSide)
                .with(Properties.POWERED, false); // default off

        // Place it in the world
        if (world.setBlockState(pos, state, Block.NOTIFY_ALL)) {
            LOGGER.info("lever placed success");
        } else LOGGER.info("lever failed");
    }

    static void placeSign(ServerWorld world, BlockPos pos, Direction facing, Text text, String colour) {
        if (world == null || world.isClient) return;

        //WallSignBlock sign = (WallSignBlock) Blocks.SPRUCE_WALL_SIGN;

        // Place a spruce wall sign facing arg direction
        world.setBlockState(pos, Blocks.SPRUCE_WALL_SIGN.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing));

        SignBlockEntity sign = (SignBlockEntity) world.getBlockEntity(pos);
        if (sign != null) {
            Text[] signPlayerName = new Text[]{
                    Text.literal(""),
                    text, // Replace line 2 with player name, leave other lines empty
                    Text.literal(""),
                    Text.literal("")
            };

            //Format the sign text player name with colour
            SignText formatText = new SignText(signPlayerName, signPlayerName, DyeColor.byName(colour, DyeColor.BLACK), true);
            sign.setText(formatText, true);
            sign.setWaxed(true);

            // Mark updated
            sign.markDirty();
            world.updateListeners(pos, sign.getCachedState(), sign.getCachedState(), 3);
        }
    }

    static void setColourBoots(ServerPlayerEntity p, String c){ //Give player boots with assigned colour
        //String colourRGB = Color.decode(Integer.toString(colourHex)).toString();
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        List<DyeItem> dyes = List.of(DyeItem.byColor(DyeColor.byName(c,DyeColor.LIME))); //Defaults to lime if colour not got from String
        ItemStack dyedBoots = DyedColorComponent.setColor(boots, dyes);
        p.getInventory().setStack(36,dyedBoots); //36 is slot for boots, IDK why help
    }

    static int getColourHex(String colour){
        if (POSSIBLE_COLOURS.contains(colour)) {
            return COLOUR_HEX.get(POSSIBLE_COLOURS.indexOf(colour));
        } else {
            return 0;
        }
    }
}