package com.botcfab;

// Role assignment stuff
// 1. Keeper runs /botc_initGame
// 1. Import JSON with the available roles for script
// 2. Get number of players, and based on that, associate number of players (no. townsfolk, outsider, minions, and demons)
// 3. Assign the players the roles from the thingy
// 4. Query for each role if they have a special setup

import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

@SuppressWarnings("unchecked")
public class Grimoire {
    // Players	5	6	7	8	9	10	11	12	13	14	15+
    // Townsf   3	3	5	5	5	7	7	7	9	9	9
    // Outsid   0	1	0	1	2	0	1	2	0	1	2
    // Minions	1	1	1	1	1	2	2	2	3	3	3
    // Demons	1	1	1	1	1	1	1	1	1	1	1
    private final HashMap<Integer, ArrayList<Integer>> roleNumbers = new HashMap<>();
    public static final Logger LOGGER = LoggerFactory.getLogger("botc-fab");
    private ArrayList<ServerPlayerEntity> aCopyOfPlayers = new ArrayList<>();
    public Grimoire(ArrayList<ServerPlayerEntity> players) {
        this.roleNumbers.put(5, new ArrayList<Integer>(Arrays.asList(3, 0, 1, 1)));
        this.roleNumbers.put(6, new ArrayList<Integer>(Arrays.asList(3, 1, 1, 1)));
        this.roleNumbers.put(7, new ArrayList<Integer>(Arrays.asList(5, 0, 1, 1)));
        this.roleNumbers.put(8, new ArrayList<Integer>(Arrays.asList(5, 1, 1, 1)));
        this.roleNumbers.put(9, new ArrayList<Integer>(Arrays.asList(5, 2, 1, 1)));
        this.roleNumbers.put(10, new ArrayList<Integer>(Arrays.asList(7, 0, 2, 1)));
        this.roleNumbers.put(11, new ArrayList<Integer>(Arrays.asList(7, 1, 2, 1)));
        this.roleNumbers.put(12, new ArrayList<Integer>(Arrays.asList(7, 2, 2, 1)));
        this.roleNumbers.put(13, new ArrayList<Integer>(Arrays.asList(9, 0, 3, 1)));
        this.roleNumbers.put(14, new ArrayList<Integer>(Arrays.asList(9, 1, 3, 1)));
        this.roleNumbers.put(15, new ArrayList<Integer>(Arrays.asList(9, 2, 3, 1)));
    }

    private ArrayList<BotcRole> getListOfRoles() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ArrayList<Object> items = mapper.readValue(
                new File("roles.json"),
                ArrayList.class
        );
        ArrayList<Map<String, Object>> allPossibleRoles = mapper.readValue(new File("roles.json"), ArrayList.class);
        ArrayList<BotcRole> roles = new ArrayList<BotcRole>();
        for (Object item : items) {
            if (item instanceof String) {
                System.out.println("Role: " + item);
//                stringedRoles.add(item.toString());
//                Role role = new Role();
//                roles.add(role.CreateRoleFromCompendium(""));
            }
        }
        return roles;
    }

    private void randomRolesAssign(ArrayList<ServerPlayerEntity> players, String pack) {
        aCopyOfPlayers = players;
        int numberOfPlayers = aCopyOfPlayers.size();
        if (numberOfPlayers < 5 || numberOfPlayers > 15) {
            LOGGER.info("Not enough users or too many users. Will not randomly assign roles");
            return;
        };
        ArrayList<Integer> roleAllocations = roleNumbers.get(numberOfPlayers);
//        BotcPack botcPack = pack;
        
    }
}
