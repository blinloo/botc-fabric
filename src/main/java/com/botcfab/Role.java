package com.botcfab;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

public class Role {

    private String id;
    private String name;
    private String edition;
    private String team;
    private String firstNightReminder;
    private String otherNightReminder;
    private List<String> reminders;
    private boolean setup;
    private String ability;
    private List<Special> special;
    private String flavor;
    private int firstNight;
    private int otherNight;
    private List<Jinx> jinxes;

    @JsonCreator
    public Role(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("edition") String edition,
            @JsonProperty("team") String team,
            @JsonProperty("firstNightReminder") String firstNightReminder,
            @JsonProperty("otherNightReminder") String otherNightReminder,
            @JsonProperty("reminders") List<String> reminders,
            @JsonProperty("setup") boolean setup,
            @JsonProperty("ability") String ability,
            @JsonProperty("flavor") String flavor
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
    }
    ///  Compendium: the file path to the JSON file with all roles in blood on the clocktower
    public Role CreateRoleFromCompendium(String pathToCompendium, String chosenRole) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Read JSON file into a list of roles
            List<Role> data = mapper.readValue(new File("compendium.json"), mapper.getTypeFactory().constructCollectionType(List.class, Role.class));

            // Identify which object in the list is the role we are looking for
            for (Role role: data) {
                if (role.getName().equalsIgnoreCase(chosenRole)) {
                    return role;
                }
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Special {
        private String type;
        private String name;

        public Special(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    public static class Jinx {
        private String id;
        private String reason;

        public Jinx(String id, String reason) {
            this.id = id;
            this.reason = reason;
        }
    }

    public String getId() {
        return id;
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

    public void setReminders(List<String> reminders) {
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
}

