package com.botcfab.classes;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class BotcGame {
    ArrayList<BotcPlayer> players = new ArrayList<>();
    BotcPlayer storyteller;
    ArrayList<String> possibleColours;
    ArrayList<String> coloursInUse;
    int totalPlayers;
    int colourStartIndex;
    boolean rolesVisible = false;

    //Only need a new one when storyteller changes?
    public BotcGame(BotcPlayer storyteller, int total, ArrayList<String> colours) {
        this.storyteller = storyteller;
        this.totalPlayers = total;
        this.possibleColours = colours;

        for (int i = 0; i < totalPlayers; i++) {
            //Add players classes for number of players
            players.add(new BotcPlayer(false, i, null, null, ""));
        }
    }

    private boolean assignPlayer(int pos, ServerPlayerEntity player){
        if (pos > totalPlayers){
            return false;
        }
        players.get(pos).setPlayer(player);

        return true;
    }

    private boolean assignRandomPlayers(ArrayList<ServerPlayerEntity> playerList){
        Collections.shuffle(playerList);

        if (playerList.size() == totalPlayers) { //if number of players in list matches total players.
            for (int index = 0; index < totalPlayers; index++) {
                players.get(index).setPlayer(playerList.get(index));
                index++;
            }
            return true;
        }
        return false;
    }

    private boolean assignColours(){
        boolean reverseOrder = ThreadLocalRandom.current().nextBoolean(); //Decides if colours are in order or reversed.
        int index = 0;
        String currentColour;

        if (reverseOrder) Collections.reverse(possibleColours);

        for (BotcPlayer p:players){
            currentColour = possibleColours.get(index);
            coloursInUse.add(currentColour);
            p.setColour(currentColour);

            index++;
            if (!(index < possibleColours.size())){
                //if index out of bounds
                return false;
            }
        }

        return true;
    }

    private boolean getRoleVisible(){
        return this.rolesVisible;
    }
    private boolean hideRoles(){
        this.rolesVisible = false;
        return true;
    }
    private boolean showRoles(){
        this.rolesVisible = true;
        return true;
    }
    private boolean toggleRoleVisible(){
        this.rolesVisible = !this.rolesVisible;
        return true;
    }
}
