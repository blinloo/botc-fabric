package com.botcfab.classes;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;

public class BotcScript {
    ArrayList<BotcRole> townsfolk;
    ArrayList<BotcRole> outsiders;
    ArrayList<BotcRole> minions;
    ArrayList<BotcRole> demons;
    ArrayList<BotcRole> jinxes;

    //TODO
    // Add constructor
    public BotcScript(ArrayList<BotcRole> t, ArrayList<BotcRole> o, ArrayList<BotcRole> m, ArrayList<BotcRole> d, ArrayList<BotcRole> j){
        this.townsfolk = t;
        this.outsiders = o;
        this.minions = m;
        this.demons = d;
        if (j != null) {
            this.jinxes = j;
        }
    }

    public ArrayList<BotcRole> getTown(){
        return this.townsfolk;
    }
}
