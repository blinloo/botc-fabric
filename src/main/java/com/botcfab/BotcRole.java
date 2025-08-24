package com.botcfab;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class BotcRole {

    private String id;
    private String name;
    private String edition;
    private String team;
    private String firstNightReminder;
    private String otherNightReminder;
    private ArrayList<String> reminders;
    private boolean setup;
    private String ability;
    private ArrayList<Special> special;
    private String flavor;
    private int firstNight;
    private int otherNight;
    private ArrayList<Jinx> jinxes;
    private ArrayList<String> remindersGlobal;

    @JsonCreator
    public BotcRole(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("edition") String edition,
            @JsonProperty("team") String team,
            @JsonProperty("firstNightReminder") String firstNightReminder,
            @JsonProperty("otherNightReminder") String otherNightReminder,
            @JsonProperty("reminders") ArrayList<String> reminders,
            @JsonProperty("setup") boolean setup,
            @JsonProperty("ability") String ability,
            @JsonProperty("flavor") String flavor,
            @JsonProperty("firstNight") int firstNight,
            @JsonProperty("otherNight") int otherNight,
            @JsonProperty("jinxes") ArrayList<Jinx> jinxes,
            @JsonProperty("special") ArrayList<Special> special,
            @JsonProperty("remindersGlobal") ArrayList<String> remindersGlobal
    ) {
        this.id = id;
        this.name = name;
        this.edition = edition;
        this.team = team;
        this.firstNightReminder = firstNightReminder;
        this.otherNightReminder = otherNightReminder;
        this.reminders = reminders;
        this.setup = setup;
        this.ability = ability;
        this.flavor = flavor;
        this.firstNight = firstNight;
        this.otherNight = otherNight;
        this.jinxes = jinxes;
        this.special = special;
        this.remindersGlobal = remindersGlobal;
    }

    public static class Special {
        private String type;
        private String name;
        private String time;
        private String global;
        private String value;

        public Special(@JsonProperty("type") String type,
                       @JsonProperty("name") String name,
                       @JsonProperty("time") String time,
                       @JsonProperty("global") String global,
                       @JsonProperty("value") String value) {
            this.type = type;
            this.name = name;
            this.time = time;
            this.global = global;
            this.value = value;
        }
    }

    public static class Jinx {
        private String id;
        private String reason;

        public Jinx(@JsonProperty("id") String id,
                    @JsonProperty("reason") String reason) {
            this.id = id;
            this.reason = reason;
        }
    }

    public String getId() {
        return id;
    }

    public boolean getSetup() {
        return setup;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getFirstNightReminder() {
        return firstNightReminder;
    }

    public void setFirstNightReminder(String firstNightReminder) {
        this.firstNightReminder = firstNightReminder;
    }

    public String getOtherNightReminder() {
        return otherNightReminder;
    }

    public void setOtherNightReminder(String otherNightReminder) {
        this.otherNightReminder = otherNightReminder;
    }

    public List<String> getReminders() {
        return reminders;
    }

    public void setReminders(ArrayList<String> reminders) {
        this.reminders = reminders;
    }

    public boolean isSetup() {
        return setup;
    }

    public void setSetup(boolean setup) {
        this.setup = setup;
    }

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public int getFirstNight() {
        return firstNight;
    }

    public void setFirstNight(int firstNight) {
        this.firstNight = firstNight;
    }

    public int getOtherNight() {
        return otherNight;
    }

    public void setOtherNight(int otherNight) {
        this.otherNight = otherNight;
    }
}

