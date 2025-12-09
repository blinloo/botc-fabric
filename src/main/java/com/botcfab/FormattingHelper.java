package com.botcfab;

import com.botcfab.classes.BotcGame;
import com.botcfab.classes.BotcPlayer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import static com.botcfab.ItemUtils.getColourHex;

public class FormattingHelper {
    static MutableText getPlayerOrder(BotcGame game){
        MutableText PlayerOrderMessage = Text.literal("Player Order: \n");
        for (BotcPlayer p:game.getPlayers()){
            String c = p.getColour();
            Text name = p.getName();
            if (p.isDead()) {
                PlayerOrderMessage
                        .append(Text.literal("⬛ ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))  // Square with colour
                        .append(Text.literal("💀 "))
                        .append(name.copy().setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GRAY)))) //Player name grey for dead
                        .append(Text.literal(" ⬛").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))
                        .append(Text.literal("\n")); //New line
            } else {
                PlayerOrderMessage
                        .append(Text.literal("⬛ ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))  // Square with colour
                        .append(name.copy()) //Player name
                        .append(Text.literal(" ⬛").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))
                        .append(Text.literal("\n")); //New line
            }
        }
        return PlayerOrderMessage;
    }

}
