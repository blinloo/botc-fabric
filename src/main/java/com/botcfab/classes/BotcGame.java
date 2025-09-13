package com.botcfab.classes;

import java.util.ArrayList;

public class BotcGame {
    ArrayList<BotcPlayer> players = new ArrayList<>();
    BotcPlayer storyteller;
    int totalPlayers;

    //Only need a new one when storyteller changes?
    public BotcGame(BotcPlayer storyteller, int total){
        this.storyteller = storyteller;
        this.totalPlayers = total;

        for (int i = 0; i < totalPlayers; i++){
            players.add(new BotcPlayer(false, "",null,null,""));
        }
    }
}
