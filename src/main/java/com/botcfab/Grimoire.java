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
import java.util.*;

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
    private BotcPack selectedPack;
    private HashMap<String, BotcRole> playerRoles = new HashMap<>();
    private BotcPack troubleBrewing = new BotcPack(new ArrayList<BotcRolesEnum>(
            Arrays.asList(
                BotcRolesEnum.WASHERWOMAN,
                BotcRolesEnum.LIBRARIAN,
                BotcRolesEnum.INVESTIGATOR,
                BotcRolesEnum.CHEF,
                BotcRolesEnum.EMPATH,
                BotcRolesEnum.FORTUNETELLER,
                BotcRolesEnum.UNDERTAKER,
                BotcRolesEnum.MONK,
                BotcRolesEnum.RAVENKEEPER,
                BotcRolesEnum.VIRGIN,
                BotcRolesEnum.SLAYER,
                BotcRolesEnum.SOLDIER,
                BotcRolesEnum.MAYOR,
                BotcRolesEnum.BUTLER,
                BotcRolesEnum.DRUNK,
                BotcRolesEnum.RECLUSE,
                BotcRolesEnum.SAINT,
                BotcRolesEnum.POISONER,
                BotcRolesEnum.SPY,
                BotcRolesEnum.SCARLETWOMAN,
                BotcRolesEnum.BARON,
                BotcRolesEnum.IMP
            )));

    public Grimoire(ArrayList<ServerPlayerEntity> players) {
        // roleNumbers are mappings of number of players to amount of townsfolks, outsiders, minions, and demons
        // in that order
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

    private BotcPack selectPack(String pack) {
        switch (pack) {
            default -> {
                return troubleBrewing;
            }
        }
    }

//    private void randomlyAssignTheRoles(ArrayList<ServerPlayerEntity> players, String team, ArrayList<Integer> roleAssigners) {
//        ArrayList<ServerPlayerEntity> copyOfPlayers = players;
//        Random rand = new Random();
//        int randomNumber = rand.nextInt(aCopyOfPlayers.size());
//        int roleRandomNumber = -1;
//        // townsfolk = 0
//        // outsiders = 1
//        // minions = 2
//        // demons = 3
//        int teamSelector = 0;
//        switch (team) {
//            case "minion":
//                roleRandomNumber = rand.nextInt(selectedPack.getAllMinions().size());
//                teamSelector = 2;
//                break;
//            case "outsider":
//                roleRandomNumber = rand.nextInt(selectedPack.getAllOutsiders().size());
//                teamSelector = 1;
//                break;
//            case "townsfolk":
//                roleRandomNumber = rand.nextInt(selectedPack.getAllTownsfolk().size());
//                teamSelector = 0;
//                break;
//            default:
//                LOGGER.info("Check the randomlyAssignTheRoles function in Grimoire");
//                teamSelector = -1;
//                break;
//        }
//        if (roleRandomNumber < 0) return;
//
//        for (int i = 0; i < roleAssigners.get(teamSelector); i++) {
//            ServerPlayerEntity player = aCopyOfPlayers.get(randomNumber);
//
//            playerRoles.put(player.getName().toString(), selectedPack.);
//            aCopyOfPlayers.remove(player);
//        }
//    }

    public void randomRolesAssign(ArrayList<ServerPlayerEntity> players, String pack) {
        aCopyOfPlayers = players;
        Random rand = new Random();
        int numberOfPlayers = aCopyOfPlayers.size();
        if (numberOfPlayers < 5 || numberOfPlayers > 15) {
            LOGGER.info("Not enough users or too many users. Will not randomly assign roles");
            return;
        };
        ArrayList<Integer> roleAllocations = roleNumbers.get(numberOfPlayers);
        selectedPack = troubleBrewing;

        // Assign teams based on the following order: demon, minion, townsfolk, outsiders

        // Assign the demon (Always only one demon initially)
        ArrayList<BotcRole> potentialDemons = selectedPack.getAllDemons();
        ServerPlayerEntity selectedPlayer = aCopyOfPlayers.get(rand.nextInt(aCopyOfPlayers.size()));
        BotcRole selectedDemonRole = potentialDemons.get(rand.nextInt(potentialDemons.size()));
        playerRoles.put(selectedPlayer.getName().toString(), selectedDemonRole);
        // Ensure that the selected player has not been selected before
        aCopyOfPlayers.remove(selectedPlayer);

        if (selectedDemonRole.getSetup()) {
            LOGGER.info("Demon selected has special setups");
        }

        // Assign the minions
        int numberOfMinions = roleAllocations.get(2);
        ArrayList<BotcRole> potentialMinions = selectedPack.getAllMinions();

        for (int i = 0; i < numberOfMinions; i++) {
            int randomNumber = rand.nextInt(aCopyOfPlayers.size());
            int minionRandom = rand.nextInt(potentialMinions.size());
            ServerPlayerEntity localSelectedPlayer = aCopyOfPlayers.get(randomNumber);
            BotcRole localSelectedMinion = potentialMinions.get(minionRandom);

            playerRoles.put(localSelectedPlayer.getName().toString(), localSelectedMinion);

            // Remove the role and the player so it doesn't get duplicated
            potentialMinions.remove(localSelectedMinion);
            aCopyOfPlayers.remove(localSelectedPlayer);
        }
        LOGGER.info("Minions and demons have been assigned");
        LOGGER.debug("Currently: {}", playerRoles);

        // Assign the outsiders
        int numberOfOutsiders = roleAllocations.get(1);
        ArrayList<BotcRole> potentialOutsiders = selectedPack.getAllOutsiders();

        for (int i = 0; i < numberOfOutsiders; i++) {
            int randomNumber = rand.nextInt(aCopyOfPlayers.size());
            int teamRandom = rand.nextInt(potentialOutsiders.size());
            ServerPlayerEntity localPlayer = aCopyOfPlayers.get(randomNumber);
            BotcRole localRole = potentialOutsiders.get(teamRandom);

            playerRoles.put(localPlayer.getName().toString(), localRole);

            // Remove the role and the player so it doesn't get duplicated
            selectedPack.removeRole(localRole);
            aCopyOfPlayers.remove(localPlayer);
        }
        LOGGER.info("Outsiders have been assigned");
        LOGGER.debug("Currently: {}", playerRoles);

        // Assign the townsfolk
        int numberOfTownsfolk = roleAllocations.get(0);
        ArrayList<BotcRole> potentialTownsfolk = selectedPack.getAllTownsfolk();

        for (int i = 0; i < numberOfOutsiders; i++) {
            int randomNumber = rand.nextInt(aCopyOfPlayers.size());
            int teamRandom = rand.nextInt(potentialMinions.size());
            ServerPlayerEntity localPlayer = aCopyOfPlayers.get(randomNumber);
            BotcRole localRole = potentialMinions.get(teamRandom);

            playerRoles.put(localPlayer.getName().toString(), localRole);

            // Remove the role and the player so it doesn't get duplicated
            selectedPack.removeRole(localRole);
            aCopyOfPlayers.remove(localPlayer);
        }
        LOGGER.info("Outsiders have been assigned");
        LOGGER.debug("Currently: {}", playerRoles);
    }
}
