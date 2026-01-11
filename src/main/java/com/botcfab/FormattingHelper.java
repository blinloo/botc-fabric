package com.botcfab;

import com.botcfab.classes.BotcGame;
import com.botcfab.classes.BotcPlayer;
import com.botcfab.classes.BotcRole;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

import static com.botcfab.BotcFab.ALIVE;
import static com.botcfab.BotcFab.DEAD;

public class FormattingHelper {
    static int getColourHex(String colour){
        return switch (colour) {
            case "white" -> 0xF0F0F0;
            case "orange" -> 0xF9801D;
            case "magenta" -> 0xC74EBD;
            case "light_blue" -> 0x3AB3DA;
            case "yellow" -> 0xFED83D;
            case "lime" -> 0x80C71F;
            case "pink" -> 0xF38BAA;
            case "gray" -> 0x474F52;
            case "light_gray" -> 0x9D9D97;
            case "cyan" -> 0x169C9C;
            case "purple" -> 0x8932B8;
            case "blue" -> 0x3C44AA;
            case "brown" -> 0x835432;
            case "green" -> 0x5E7C16;
            case "red" -> 0xB02E26;
            case "black" -> 0x1D1D21;
            default -> 0xF2B233;
        };
    }

    static MutableText getPlayerOrder(BotcGame game) {
        MutableText PlayerOrderMessage = Text.literal("Player Order: \n");
        for (BotcPlayer p : game.getPlayers()) {
            String c = p.getColour();
            if (c == null){
                c = "lime";
            }
            Text name = p.getName();
            if (name == null){
                name = Text.literal("NULL");
            }
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

    public static MutableText formatRoleName(BotcRole role){
        if (role != null){
            MutableText name = Text.literal(role.getName());
            switch (role.getTeam()) {
                case "townsfolk":
                    name.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex("blue"))));
                    break;
                case "outsider":
                    name.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex("light_blue"))));
                    break;
                case "minion":
                    name.setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED)));
                    break;
                case "demon":
                    name.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex("red"))));
                    break;
                default:
                    //If role does not have team, green makes error obvious.
                    name.setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GREEN)));
                    break;
            }
            return name;
        }
        return Text.literal("NULL ROLE");
    }

    public static MutableText formatRoleDesc(BotcRole role){
        if (role != null){
            return Text.literal(role.getAbility());
        }
        return Text.literal("NULL ROLE DESC");
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
            if (c == null){
                c = "lime";
            }
            if (name == null){
                name = Text.literal("NULL");
            }
            //Name
            gameInfoText
                    .append(Text.literal("⬛ ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColourHex(c)))))  // Square with colour
                    .append(name.copy()) //Player name
                    .append(Text.literal("\n")); //New line
            //Role
            if (p.getRole() != null) {
                gameInfoText
                        .append(Text.literal("Role: "))
                        .append(Text.literal(p.getRole().getName()))  // Role name
                        .append(Text.literal("\n")); //New line
            } else {
                gameInfoText
                        .append(Text.literal("Role: N/A"))
                        .append(Text.literal("\n")); //New line
            }
            //Status
            gameInfoText
                    .append(Text.literal("Status: "));
            if (p.isDead()) { //Checks dead or alive status
                gameInfoText.append(Text.literal(DEAD));
            } else {
                gameInfoText.append(Text.literal(ALIVE));
            }
            if (p.getKillFlag()){
                gameInfoText.append(Text.literal("Marked for kill on day\n"));
            }
            if (p.getReviveFlag()){
                gameInfoText.append(Text.literal("Marked for revive on day\n"));
            }

            gameInfoText.append(Text.literal("\n")); //New line - 2 between players
            gameInfoText.append(Text.literal("\n")); //New line
        }
        return gameInfoText;
    }
}