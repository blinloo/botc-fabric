package com.botcfab.classes;

import com.botcfab.FormattingHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.botcfab.FormattingHelper.formatGameInfo;

public class BotcGame {
    ArrayList<BotcPlayer> players = new ArrayList<>();
    BotcPlayer storyteller;
    List<String> possibleColours;
    List<String> coloursInUse;
    int totalPlayers;
    int colourStartIndex;
    boolean showVoteResult;
    boolean rolesVisible = false;
    boolean playersInvis;

    //Only need a new one when storyteller changes?
    public BotcGame(BotcPlayer storyteller, int total, List<String> colours) {
        this.storyteller = storyteller;
        this.totalPlayers = total;
        this.possibleColours = colours;
        this.showVoteResult = true;
        this.playersInvis = true;

        for (int i = 0; i < totalPlayers; i++) {
            //Add players classes for number of players
            players.add(new BotcPlayer(i, null, null));
        }
    }

    public Text getGameInfo(){
        return formatGameInfo(this);
    }

    public boolean assignPlayer(int pos, ServerPlayerEntity player){
        if (pos > totalPlayers){
            return false;
        }
        players.get(pos).setPlayer(player);

        return true;
    }

    public boolean assignRandomPlayers(ArrayList<ServerPlayerEntity> playerList){
        //Assigns players to each position in random order
        Collections.shuffle(playerList);

        if (playerList.size() == totalPlayers) { //if number of players in list matches total players.
            for (int index = 0; index < totalPlayers; index++) {
                ServerPlayerEntity p = playerList.get(index);
                assignPlayer(index,p); //Assign player to BotcPlayer class at position
                index++;
            }
            return true;
        }
        return false;
    }

    public boolean assignColours(){
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

    public boolean setupRandomGame(ArrayList<ServerPlayerEntity> playerList){
        assignColours();
        assignRandomPlayers(playerList);
        return true;
    }

    public int getTotalPlayers(){
        return this.totalPlayers;
    }

    public BotcPlayer getPlayerAtColour(String colour){
        for (BotcPlayer p:players){
            if (p.getColour().equals(colour))
            {
                return p;
            }
        }
        return null;
    }

    public ArrayList<BotcPlayer> getPlayers(){
        return this.players;
    }

    public BotcPlayer getStoryteller(){
        return this.storyteller;
    }

    public ArrayList<BotcPlayer> getKillFlaggedPlayers(){
        ArrayList<BotcPlayer> flaggedPlayers = new ArrayList<>();
        for (BotcPlayer p:players){
            if (p.getKillFlag())
                flaggedPlayers.add(p);
        }
        return flaggedPlayers;
    }

    public ArrayList<BotcPlayer> getReviveFlaggedPlayers(){
        ArrayList<BotcPlayer> flaggedPlayers = new ArrayList<>();
        for (BotcPlayer p:players){
            if (p.getReviveFlag())
                flaggedPlayers.add(p);
        }
        return flaggedPlayers;
    }

    public BotcPlayer getAccusedPlayer(){
        for (BotcPlayer p:players){
            if (p.isAccused) {
                return p;
            }
        }
        if (storyteller.isAccused){
            return storyteller;
        }
        return null;
    }

    public BotcPlayer getMarkedPlayer(){
        for (BotcPlayer p:players){
            if (p.isMarked) {
                return p;
            }
        }
        if (storyteller.isMarked){
            return storyteller;
        }
        return null;
    }

    public BotcPlayer getPlayerBeingExecuted(){
        for (BotcPlayer p:players){
            if (p.getExecutionStatus()) {
                return p;
            }
        }
        if (storyteller.getExecutionStatus()){
            return storyteller;
        }
        return null;
    }

    public BotcPlayer findPlayer(ServerPlayerEntity playerEntity){
        for (BotcPlayer p:players){
            if (p.player == playerEntity) {
                return p;
            }
        }
        if (storyteller.player == playerEntity){
            return storyteller;
        }
        return null;
    }

    public boolean getRoleVisible(){
        return this.rolesVisible;
    }
    public boolean hideRoles(){
        this.rolesVisible = false;
        return true;
    }
    public boolean showRoles(){
        this.rolesVisible = true;
        return true;
    }
    public boolean toggleRoleVisible(){
        this.rolesVisible = !this.rolesVisible;
        return true;
    }
    public boolean showVoteResult(){
        return showVoteResult;
    }

    public void changeShowVoteResult(boolean newValue){
        this.showVoteResult = newValue;
    }

    public boolean invisPlayers(){
        return this.playersInvis;
    }

    public void changePlayerInvis(boolean newValue){
        this.playersInvis = newValue;
    }
}
