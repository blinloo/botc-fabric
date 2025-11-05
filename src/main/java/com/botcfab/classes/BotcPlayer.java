package com.botcfab.classes;

import net.minecraft.server.network.ServerPlayerEntity;

public class BotcPlayer {
    BotcRole role;
    ServerPlayerEntity player;
    boolean hasGhostVote;
    boolean registerAsDead;
    boolean isInvisible;
    boolean isStoryteller;
    int position;
    String colour; //Colour assigned to player
    String status; //Player status, dead, alive, ghost


    //TODO
    // Add constructor
    // Add change and get colour function
    // Add change and get role function
    // Add change ghost and dead options
    // Add method to fake register as dead/alive
    // Add method to set assigned player
    // method to return whether has ghost vote
    // method to return whether alive/dead
    public BotcPlayer(boolean isStoryteller, int pos, ServerPlayerEntity p, BotcRole r, String s){
        this.isStoryteller = isStoryteller;
        if (!isStoryteller){
            if (r != null) {
                this.role = r;
            }
            this.hasGhostVote = true;
            this.registerAsDead = false;
            this.position = pos;
            this.status = s;
        }
        this.isInvisible = false;
        if (p != null) {
            this.player = p;
        }
    }

    public boolean getStoryteller(){
        return isStoryteller;
    }

    public boolean getInvisStatus(){
        return isInvisible;
    }

    public void setPlayer(ServerPlayerEntity p){
        if (p != null){
            this.player = p;
        }
    }

    public ServerPlayerEntity getPlayer(ServerPlayerEntity p){
        return player;
    }

    public void setColour(String c){
        this.colour = c;
    }

    public String getColour(){
        return colour;
    }

    public void setRole(BotcRole r){
        this.role = r;
    }

    public BotcRole getRole(){
        return role;
    }
}
