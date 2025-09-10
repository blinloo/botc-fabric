package com.botcfab.classes;

import net.minecraft.server.network.ServerPlayerEntity;

public class BotcPlayer {
    BotcRole role;
    ServerPlayerEntity player;
    boolean hasGhostVote;
    boolean registerAsDead;
    boolean isInvisible;
    String colour;
    String status;


    //TODO
    // Add constructor
    // Add change and get colour function
    // Add change and get role function
    // Add change ghost and dead options
    // Add method to fake register as dead/alive
    // Add method to set assigned player
    // method to return whether has ghost vote
    // method to return whether alive/dead
    public BotcPlayer(String c, ServerPlayerEntity p, BotcRole r, String s){
        this.hasGhostVote = true;
        this.registerAsDead = false;
        this.isInvisible = false;
        this.colour = c;
        this.status = s;
        if (p != null) {
            this.player = p;
        }
        if (r != null) {
            this.role = r;
        }
    }

    public boolean getInvisStatus(){
        return isInvisible;
    }
}
