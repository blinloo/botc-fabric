package com.botcfab;

// Role assignment stuff
// 1. Import JSON with the available roles for script
// 2. Get number of players, and based on that, associate number of players (no. townsfolk, outsider, minions, and demons)
// 3. Assign the players the roles from the thingy
// 4. Query for each role if they have a special setup

import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Grimoire {
    // Players	5	6	7	8	9	10	11	12	13	14	15+
    // Townsf   3	3	5	5	5	7	7	7	9	9	9
    // Outsid   0	1	0	1	2	0	1	2	0	1	2
    // Minions	1	1	1	1	1	2	2	2	3	3	3
    // Demons	1	1	1	1	1	1	1	1	1	1	1
    private final Map<Integer, int[]> roleNumbers = Map.ofEntries(
            Map.entry(5, new int[]{3, 0, 1, 1}),
            Map.entry(6, new int[]{3, 1, 1, 1}),
            Map.entry(7, new int[]{5, 0, 1, 1}),
            Map.entry(8, new int[]{5, 1, 1, 1}),
            Map.entry(9, new int[]{5, 2, 1, 1}),
            Map.entry(10, new int[]{7, 0, 2, 1}),
            Map.entry(11, new int[]{7, 1, 2, 1}),
            Map.entry(12, new int[]{7, 2, 2, 1}),
            Map.entry(13, new int[]{9, 0, 3, 1}),
            Map.entry(14, new int[]{9, 1, 3, 1}),
            Map.entry(15, new int[]{9, 2, 3, 1})
    );

    private List<Role> getListOfRoles() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        List<Object> items = mapper.readValue(
                new File("roles.json"),
                List.class
        );
        List<Role> roles = new ArrayList<Role>();
        for (Object item : items) {
            if (item instanceof String) {
                System.out.println("Role: " + item);
                stringedRoles.add(item.toString());
                Role role = new Role();
                roles.add(role.CreateRoleFromCompendium(""));
            }
        }


        return roles;
    }

    public void assignRoles(List<ServerPlayerEntity> players) {
        // Get the number of users
        final int numberOfPlayers = players.size();
        int[] actualRoleNumber = roleNumbers.get(numberOfPlayers);
    }
}
