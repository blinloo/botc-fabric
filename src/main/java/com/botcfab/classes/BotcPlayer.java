package com.botcfab.classes;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BotcPlayer {
    boolean isStoryteller;
    BotcRole role;
    ServerPlayerEntity player;
    boolean hasGhostVote;
    boolean registerAsDead;
    boolean isInvisible;
    boolean isAccused;
    boolean isMarked;
    boolean killFlag; //To be killed on morning
    boolean reviveFlag; //To be revived on morning
    boolean loseGhostVote;
    boolean willSurviveExecution;
    boolean currentlyBeingExecuted;
    int position;
    String colour; //Colour assigned to player
    Text playerName;


    //TODO
    // Add constructor
    // Add change and get colour function
    // Add change and get role function
    // Add change ghost and dead options
    // Add method to fake register as dead/alive
    // Add method to set assigned player
    // method to return whether has ghost vote
    // method to return whether alive/dead
    public BotcPlayer(int pos, ServerPlayerEntity p, BotcRole r){

        if (r != null) {
            this.role = r;
        }
        this.isStoryteller = false;
        this.hasGhostVote = true;
        this.registerAsDead = false;
        this.position = pos;
        this.isInvisible = false;
        this.isAccused = false;
        this.isMarked = false;
        this.killFlag = false;
        this.reviveFlag = false;
        this.loseGhostVote = true; //Change to false for banshee on ability activation
        this.willSurviveExecution = false;
        this.currentlyBeingExecuted = false;
        if (p != null) {
            this.player = p;
            this.playerName = p.getStyledDisplayName();
        }
    }

    public void killPlayer(){
        if (!registerAsDead){
            this.isInvisible = true;
            addGhostVote();
            this.registerAsDead = true;
        }

    }

    public void revivePlayer(){
        if (registerAsDead){
            this.isInvisible = false;
            addGhostVote();
            this.registerAsDead = false;
        }
    }

    public void removeGhostVote(){
        this.hasGhostVote = false;
    }

    public void addGhostVote(){
        this.hasGhostVote = true;
    }

    public void addKillFlag(){
        this.killFlag = true;
    }

    public void addReviveFlag(){
        this.reviveFlag = true;
    }

    public void removeFlags(){
        this.killFlag = false;
        this.reviveFlag = false;
    }

    public boolean getKillFlag(){
        return killFlag;
    }

    public boolean getReviveFlag(){
        return reviveFlag;
    }

    public boolean getInvisStatus(){
        return isInvisible;
    }

    public void setPlayer(ServerPlayerEntity p){
        if (p != null){
            this.player = p;
            this.playerName = p.getStyledDisplayName();
        }
    }

    public ServerPlayerEntity getPlayer(){
        return player;
    }

    public void setPosition(int pos){
        this.position = pos;
    }

    public int getPosition(){
        return position;
    }

    public void setColour(String c){
        this.colour = c;
    }

    public String getColour(){
        return colour;
    }

    public Text getName(){
        if (playerName != null) {
            return playerName;
        } else {
            return Text.literal("NULL");
        }
    }

    public String getNameString(){
        if (player != null) {
            return player.getNameForScoreboard();
        } else
            return "no name";

    }

    public void setRole(BotcRole r){
        this.role = r;
    }

    public BotcRole getRole(){
        return role;
    }

    public boolean isDead(){
        return registerAsDead;
    }
    public boolean hasGhostVote(){
        return hasGhostVote;
    }

    public boolean canLoseGhostVote(){
        return loseGhostVote;
    }

    public void markAccused(){
        this.isAccused = true;
    }

    public void removedAccused(){
        this.isAccused = false;
    }

    public boolean isAccused(){
        return isAccused;
    }

    public void markMarked(){
        this.isMarked = true;
    }

    public void removedMarked(){
        this.isMarked = false;
    }

    public boolean isMarked(){
        return isMarked;
    }
    public boolean surviveExecution(){
        return willSurviveExecution;
    }
    public void changeSurviveExecution(boolean newValue){
        this.willSurviveExecution = newValue;
    }

    public void changeExecutionStatus(boolean newValue){
        this.currentlyBeingExecuted = newValue;
    }

    public boolean getExecutionStatus(){
        return this.currentlyBeingExecuted;
    }

    public void changeLoseGhostVote(boolean newValue){
        this.loseGhostVote = newValue;
    }

    public void setStoryteller(){
        this.isStoryteller = true;
    }
    public void removeStoryteller(){
        this.isStoryteller = false;
    }
    public boolean isStoryteller(){
        return  isStoryteller;
    }
}
