package com.botcfab;

import com.botcfab.classes.BotcGame;
import com.botcfab.classes.BotcPlayer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

import static com.botcfab.BotcFab.ALIVE;
import static com.botcfab.BotcFab.DEAD;
import static com.botcfab.ItemUtils.getColourHex;

public class FormattingHelper {
    static MutableText getPlayerOrder(BotcGame game) {
        MutableText PlayerOrderMessage = Text.literal("Player Order: \n");
        for (BotcPlayer p : game.getPlayers()) {
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

    public static MutableText formatGameInfo(BotcGame game) {
        MutableText gameInfoText = Text.literal("Game Info: \n");
        ArrayList<BotcPlayer> players = game.getPlayers();
        //TODO add info for script selected

        gameInfoText //storyteller
                .append(Text.literal("Storyteller: "))
                .append(game.getStoryteller().getName().copy())
                .append(Text.literal("\n"));

        gameInfoText //total players
                .append(Text.literal("Number of players: "))
                .append(Text.literal(game.getTotalPlayers()+""))
                .append(Text.literal("\n"));

        for (BotcPlayer p : players) { //Show all players
            String c = p.getColour();
            Text name = p.getName();
            //Name
            gameInfoText
                    .append(Text.literal("⬛ ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))  // Square with colour
                    .append(name.copy()) //Player name
                    .append(Text.literal("\n")); //New line
            //Role
            gameInfoText
                    .append(Text.literal("Role: "))
                    .append(Text.literal(p.getRole().getName()))  // Role name
                    .append(Text.literal("\n")); //New line
            //Status
            gameInfoText
                    .append(Text.literal("Status: "));
            if (p.isDead()) { //Checks dead or alive status
                gameInfoText.append(Text.literal(DEAD));
            } else {
                gameInfoText.append(Text.literal(ALIVE));
            }
            gameInfoText.append(Text.literal("\n")); //New line

            gameInfoText.append(Text.literal("\n")); //New line
        }
        return gameInfoText;
    }
}