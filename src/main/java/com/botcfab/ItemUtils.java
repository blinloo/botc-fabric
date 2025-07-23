package com.botcfab;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.botcfab.BotcFab.POSSIBLE_COLOURS;

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

        // Rename using components
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
        // Create the desired lever block state
        var state = lever.getDefaultState()
                //.with(Properties.HORIZONTAL_FACING, facing)
                .with(LeverBlock.FACING, facing)
                .with(LeverBlock.FACE, wallSide)
                .with(Properties.POWERED, false); // default off

        // Place it in the world
        world.setBlockState(pos, state, Block.NOTIFY_ALL);
    }

    static private void placeSign(ServerWorld world, BlockPos pos, Text text, String colour) {
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

    static void placeWallSign(ServerWorld world, BlockPos pos, Direction facing, Text text, String colour) {
        if (world == null) return;

        // Place a spruce wall sign facing arg direction
        world.setBlockState(pos, Blocks.SPRUCE_WALL_SIGN.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing));

        placeSign(world,pos,text,colour);
    }

    static void placeStandingSign(ServerWorld world, BlockPos pos, Direction facing, Text text, String colour) {
        if (world == null) return;

        // Place a spruce wall sign facing arg direction
        world.setBlockState(pos, Blocks.DARK_OAK_SIGN.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing));

        placeSign(world,pos,text,colour);
    }

    static void setColourBoots(ServerPlayerEntity p, String c){ //Give player boots with assigned colour
        //String colourRGB = Color.decode(Integer.toString(colourHex)).toString();
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        boots.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true)); //Should set to unbreakable
        //boots.addEnchantment((RegistryEntry<Enchantment>) Enchantments.BINDING_CURSE,1); //Might work?
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

    static ItemStack getWoolFromColour(String colour){
        return switch (colour) {
            case "black" -> new ItemStack(Items.BLACK_WOOL);
            case "yellow" -> new ItemStack(Items.YELLOW_WOOL);
            case "orange" -> new ItemStack(Items.ORANGE_WOOL);
            case "pink" -> new ItemStack(Items.PINK_WOOL);
            case "red" -> new ItemStack(Items.RED_WOOL);
            case "purple" -> new ItemStack(Items.PURPLE_WOOL);
            case "brown" -> new ItemStack(Items.BROWN_WOOL);
            case "green" -> new ItemStack(Items.GREEN_WOOL);
            case "white" -> new ItemStack(Items.WHITE_WOOL);
            case "blue" -> new ItemStack(Items.BLUE_WOOL);
            case "cyan" -> new ItemStack(Items.CYAN_WOOL);
            case "gray" -> new ItemStack(Items.GRAY_WOOL);
            default -> null;
        };
    }
    static ItemStack getGlassFromColour(String colour){
        return switch (colour) {
            case "black" -> new ItemStack(Items.BLACK_STAINED_GLASS);
            case "yellow" -> new ItemStack(Items.YELLOW_STAINED_GLASS);
            case "orange" -> new ItemStack(Items.ORANGE_STAINED_GLASS);
            case "pink" -> new ItemStack(Items.PINK_STAINED_GLASS);
            case "red" -> new ItemStack(Items.RED_STAINED_GLASS);
            case "purple" -> new ItemStack(Items.PURPLE_STAINED_GLASS);
            case "brown" -> new ItemStack(Items.BROWN_STAINED_GLASS);
            case "green" -> new ItemStack(Items.GREEN_STAINED_GLASS);
            case "white" -> new ItemStack(Items.WHITE_STAINED_GLASS);
            case "blue" -> new ItemStack(Items.BLUE_STAINED_GLASS);
            case "cyan" -> new ItemStack(Items.CYAN_STAINED_GLASS);
            case "gray" -> new ItemStack(Items.GRAY_STAINED_GLASS);
            default -> null;
        };
    }

    static void givePlayerStorytellerItems(ServerPlayerEntity player){
        ArrayList<ItemStack> items = new ArrayList<>();
        ItemStack grimoire = setCustomName(new ItemStack(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE),Text.literal("Grimoire"));
        grimoire.set(DataComponentTypes.RARITY, Rarity.EPIC);
        grimoire.remove(DataComponentTypes.HIDE_TOOLTIP); //Might not do anything
        items.add(grimoire);

        ItemStack accuseStick = setCustomName(new ItemStack(Items.BREEZE_ROD),Text.literal("Accusation Stick"));
        accuseStick.set(DataComponentTypes.RARITY, Rarity.RARE);
        items.add(accuseStick);

        ItemStack playerOrder = setCustomName(new ItemStack(Items.PAPER),Text.literal("Get Player Order"));
        playerOrder.set(DataComponentTypes.RARITY, Rarity.UNCOMMON);
        items.add(playerOrder);

        for (ItemStack i:items){
            if (!player.getInventory().insertStack(i))
            {
                player.sendMessage(Text.literal("Not enough space in inventory for items! Please make space and run the command again"));
                return;
            }
        }
    }
}