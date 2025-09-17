package com.botcfab.classes;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class BotcGame {
    ArrayList<BotcPlayer> players = new ArrayList<>();
    BotcPlayer storyteller;
    ArrayList<String> possibleColours;
    ArrayList<String> coloursInUse;
    int totalPlayers;
    int colourStartIndex;

    //Only need a new one when storyteller changes?
    public BotcGame(BotcPlayer storyteller, int total, ArrayList<String> colours) {
        this.storyteller = storyteller;
        this.totalPlayers = total;
        this.possibleColours = colours;

        for (int i = 0; i < totalPlayers; i++) {
            //Add players classes for number of players
            players.add(new BotcPlayer(false, "", null, null, ""));
        }
    }

    private boolean assignPlayers(ArrayList<ServerPlayerEntity> playerList){
        int index = 0;
        for (ServerPlayerEntity p:playerList){
            players.get(index);
        }

        return true;
    }

    private boolean assignColours(){
        int startPoint = ThreadLocalRandom.current().nextInt(0, (11+1)); //Determines start point for colour selection
        int index = startPoint;
        String currentColour;

        for (BotcPlayer p:players){
            currentColour = possibleColours.get(index);
            coloursInUse.add(currentColour);

            index++;
            if (!(index < possibleColours.size())){
                //if index out of bounds
                index = 0;
            }
        }

        return true;
    }
}
