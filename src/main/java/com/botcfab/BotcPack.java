package com.botcfab;

import net.minecraft.block.Block;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class BotcPack{
    private ArrayList<BotcRolesEnum> roles;
    /// mappedRoles provide a list of roles categorised by the team names
    /// We are using Hashmap to provide faster lookup times
    private HashMap<String, ArrayList<BotcRole>> mappedRoles = new HashMap<>();

    public BotcPack(ArrayList<BotcRolesEnum> listOfRoles) {
        this.roles = listOfRoles;
        for (BotcRolesEnum roleEnum: listOfRoles) {
            BotcRole role = BotcRoleCacher.getRole(roleEnum.getId());
            if (role != null) {
                ArrayList<BotcRole> tempRoles = this.mappedRoles.getOrDefault(role.getTeam(), new ArrayList<>());
                tempRoles.add(role);
                this.mappedRoles.put(role.getTeam(), tempRoles);
            }
        }
    }

    public ArrayList<BotcRole> getAllEvilPlayers() {
        ArrayList<BotcRole> tempRole = new ArrayList<>();
        tempRole.addAll(mappedRoles.get("minion"));
        tempRole.addAll(mappedRoles.get("demon"));
        return tempRole;
    }
    public ArrayList<BotcRole> getAllDemons() {
        return mappedRoles.get("demon");
    }

    public ArrayList<BotcRole> getAllMinions() {
        return mappedRoles.get("minion");
    }
    public ArrayList<BotcRole> getAllTownsfolk() {
        return mappedRoles.get("townsfolk");
    }
    public ArrayList<BotcRole> getAllOutsiders() {
        return mappedRoles.get("outsider");
    }

    public ArrayList<BotcRole> getAllGoodRoles() {
        ArrayList<BotcRole> tempRole = new ArrayList<>();
        tempRole.addAll(mappedRoles.get("townsfolk"));
        tempRole.addAll(mappedRoles.get("outsider"));
        return tempRole;
    }

    public void removeRole(BotcRole selectedRole) {
        for (String key: mappedRoles.keySet()) {
            mappedRoles.get(key).remove(selectedRole);
            BotcFab.LOGGER.info("Removed mappedRole {}", selectedRole.getName());
            BotcFab.LOGGER.info("mappedRoles are now {}", mappedRoles.toString());
        }
    }

    public HashMap<String, ArrayList<BotcRole>> getAllPossibleRoles() {
        return mappedRoles;
    }

    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder();
        for (String key: mappedRoles.keySet()) {
            temp.append("====").append(key).append("====\n");
            for (int i = 0; i < mappedRoles.get(key).size(); i++) {
                temp.append(mappedRoles.get(key).get(i).getName()).append(", ");
            }
            temp.append("\n");
        }
        BotcFab.LOGGER.info("toString override test is {}", temp);
        return temp.toString();
    }
}
