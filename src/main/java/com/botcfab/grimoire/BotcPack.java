package com.botcfab.grimoire;

import com.botcfab.BotcFab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;

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

    public BotcPack() {}
    /// ArrayList<BotcRolesEnum> roles
    /// @return the roles variables, which stores a list of roles in an enum
    public ArrayList<BotcRolesEnum> getAllRolesAsArray() {
        return roles;
    }

    // Submits the roleID
    public String getRoleDescription(String role) {
        for (String key: mappedRoles.keySet()) {
            for (int i = 0; i < mappedRoles.get(key).size(); i++) {
                if (mappedRoles.get(key).get(i).getId().equalsIgnoreCase(role))  {
                    return mappedRoles.get(key).get(i).getAbility();
                }
            }
        }
        BotcFab.LOGGER.info("mappedRoles is the following when trying to getRoleDescription {}\n",role);
        BotcFab.LOGGER.info(logPrettyMappedRoles());
        return "";
    }

    public String logPrettyMappedRoles() {
        StringBuilder builder = new StringBuilder();
        builder.append("====\ntownsfolk\n====\n");
        for (int i = 0; i < mappedRoles.get("townsfolk").size(); i++) {
            builder.append(mappedRoles.get("townsfolk").get(i).getName()).append("\n");
        }
        builder.append("====\noutsider\n====\n");
        for (int i = 0; i < mappedRoles.get("outsider").size(); i++) {
            builder.append(mappedRoles.get("outsider").get(i).getName()).append("\n");
        }
        builder.append("====\nminion\n====\n");
        for (int i = 0; i < mappedRoles.get("minion").size(); i++) {
            builder.append(mappedRoles.get("minion").get(i).getName()).append("\n");
        }
        builder.append("====\ndemon\n====\n");
        for (int i = 0; i < mappedRoles.get("demon").size(); i++) {
            builder.append(mappedRoles.get("demon").get(i).getName()).append("\n");
        }
        return builder.toString();
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
        return temp.toString();
    }
}
