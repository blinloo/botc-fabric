package com.botcfab.classes;

import com.botcfab.BotcFab;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BotcRoleCacher {
    private static HashMap<String, BotcRole> cachedRoles = new HashMap<>();
    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Path configPath = BotcFab.getFolderPath();
            BotcFab.LOGGER.info(configPath.toString());
            File fullFile = configPath.resolve("all-possible-roles.json").toFile();
            // Read JSON file into a list of roles
            ArrayList<BotcRole> data = mapper.readValue(fullFile, mapper.getTypeFactory().constructCollectionType(List.class, BotcRole.class));
            BotcFab.LOGGER.info("all-possible-roles successfully cached (number of roles cached: {})", data.size());

            // Populate the Hashmap for faster lookups
            for (BotcRole role : data) {
                cachedRoles.put(role.getId(), role);
            }
        } catch (IOException e) {
            BotcFab.LOGGER.info("Failed to cache all roles. Please check your configuration");
            BotcFab.LOGGER.info("Due to {}", e.toString());
        }
    }

    /**
     * Retrieve a role by its id value.
     * BotcRolesEnum contains a mapping of enums to ID.
     * @param roleId The unique string ID of the role.
     * @return The BotcRole object or null if not found.
     */
    public static BotcRole getRole(String roleId) {
        return cachedRoles.get(roleId);
    }
}
