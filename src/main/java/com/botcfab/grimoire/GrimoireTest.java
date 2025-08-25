package com.botcfab.grimoire;

// Role assignment stuff
// 1. Keeper runs /botc_initGame
// 1. Import JSON with the available roles for script
// 2. Get number of players, and based on that, associate number of players (no. townsfolk, outsider, minions, and demons)
// 3. Assign the players the roles from the thingy
// 4. Query for each role if they have a special setup

import com.botcfab.BotcFab;

import java.util.*;

public class GrimoireTest {
    // Players	5	6	7	8	9	10	11	12	13	14	15+
    // Townsf   3	3	5	5	5	7	7	7	9	9	9
    // Outsid   0	1	0	1	2	0	1	2	0	1	2
    // Minions	1	1	1	1	1	2	2	2	3	3	3
    // Demons	1	1	1	1	1	1	1	1	1	1	1
    @SuppressWarnings("SpellCheckingInspection")
    private final HashMap<Integer, ArrayList<Integer>> roleNumbers = new HashMap<>();
    private final ArrayList<String> aCopyOfPlayers = new ArrayList<>(
            Arrays.asList(
                    "Player 1",
                    "Player 2",
                    "Player 3",
                    "Player 4",
                    "Player 5",
                    "Player 6",
                    "Player 7",
                    "Player 8"
            )
    );
    private BotcPack selectedPack;
    private HashMap<String, BotcRole> playerRoles = new HashMap<>();
    private final BotcPack troubleBrewing = new BotcPack(new ArrayList<>(
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
    private final int TOWNSFOLK_INDEX = 0;
    private final int OUTSIDER_INDEX = 1;
    private final int MINION_INDEX = 2;
    private final int DEMON_INDEX = 3;
    public GrimoireTest() {
        // roleNumbers are mappings of number of players to amount of townsfolks, outsiders, minions, and demons
        // in that order

        this.roleNumbers.put(5, new ArrayList<>(Arrays.asList(3, 0, 1, 1)));
        this.roleNumbers.put(6, new ArrayList<>(Arrays.asList(3, 1, 1, 1)));
        this.roleNumbers.put(7, new ArrayList<>(Arrays.asList(5, 0, 1, 1)));
        this.roleNumbers.put(8, new ArrayList<>(Arrays.asList(5, 1, 1, 1)));
        this.roleNumbers.put(9, new ArrayList<>(Arrays.asList(5, 2, 1, 1)));
        this.roleNumbers.put(10, new ArrayList<>(Arrays.asList(7, 0, 2, 1)));
        this.roleNumbers.put(11, new ArrayList<>(Arrays.asList(7, 1, 2, 1)));
        this.roleNumbers.put(12, new ArrayList<>(Arrays.asList(7, 2, 2, 1)));
        this.roleNumbers.put(13, new ArrayList<>(Arrays.asList(9, 0, 3, 1)));
        this.roleNumbers.put(14, new ArrayList<>(Arrays.asList(9, 1, 3, 1)));
        this.roleNumbers.put(15, new ArrayList<>(Arrays.asList(9, 2, 3, 1)));
        BotcFab.LOGGER.info("Finished assigning numbers");
    }

    private BotcPack selectPack(String pack) {
        switch (pack) {
            default -> {
                return troubleBrewing;
            }
        }
    }

    private void assignRandomRolesTeam(int numberOfTeams, ArrayList<BotcRole> potentialRoles) {
        Random rand = new Random();
        for (int i = 0; i < numberOfTeams; i++) {
            int randomNumber = rand.nextInt(aCopyOfPlayers.size());
            int minionRandom = rand.nextInt(potentialRoles.size());
            String localSelectedPlayer = aCopyOfPlayers.get(randomNumber);
            BotcRole localSelectedMinion = potentialRoles.get(minionRandom);

            playerRoles.put(localSelectedPlayer, localSelectedMinion);

            // Remove the role and the player so it doesn't get duplicated
            potentialRoles.remove(localSelectedMinion);
            aCopyOfPlayers.remove(localSelectedPlayer);
        }
    }

    private void logPlayerRoles() {
        BotcFab.LOGGER.info("=======================");
        for (String player: playerRoles.keySet()) {
            String name = playerRoles.get(player).getName();
            boolean special = playerRoles.get(player).getSetup();
            BotcFab.LOGGER.info("{}: {} (Special: {})", player,  name, special);
        }
        BotcFab.LOGGER.info("=======================");
    }

    public BotcPack getAllRoles() {
        return selectedPack;
    }

    public String getAssignedRoles() {
        StringBuilder temp = new StringBuilder();
        ArrayList<String> townsfolk = new ArrayList<>();
        ArrayList<String> outsider = new ArrayList<>();
        ArrayList<String> minion = new ArrayList<>();
        ArrayList<String> demon = new ArrayList<>();
        for (String player: playerRoles.keySet()) {
            String role = playerRoles.get(player).getName();
            String team = playerRoles.get(player).getTeam();
            switch (team) {
                case "townsfolk":
                    townsfolk.add(player + " " + role + "\n");
                    break;
                case "outsider":
                    outsider.add(player + " " + role  + "\n");
                    break;
                case "minion":
                    minion.add(player + " " + role  + "\n");
                    break;
                case "demon":
                    demon.add(player + " " + role  + "\n");
                    break;
                default:
                    BotcFab.LOGGER.info("Team not one of the four possible cases. It is {}", team);
                    break;
            }
        }
        temp.append("=======Townsfolk=======\n");
        temp.append(townsfolk);
        temp.append("=======Outsider=======\n");
        temp.append(outsider);
        temp.append("=======Minions=======\n");
        temp.append(minion);
        temp.append("=======Demons=======\n");
        temp.append(demon);
        return temp.toString();
    }

    public void randomRolesAssign(String pack) {
        Random rand = new Random();
        BotcFab.LOGGER.info("Random Roles Assign here");
        int numberOfPlayers = aCopyOfPlayers.size();
        BotcFab.LOGGER.info("Number of players {}", numberOfPlayers);
        if (numberOfPlayers < 5 || numberOfPlayers > 15) {
            BotcFab.LOGGER.info("Not enough users or too many users. Will not randomly assign roles");
            return;
        };
        ArrayList<Integer> roleAllocations = roleNumbers.get(numberOfPlayers);
        selectedPack = troubleBrewing;
        BotcFab.LOGGER.info(selectedPack.toString());

        BotcFab.LOGGER.info("Selected pack was {} trouble brewing", selectedPack.getAllDemons().size());
        // Assign teams based on the following order: demon, minion, townsfolk, outsiders

        // Assign the demon (Always only one demon initially)
        ArrayList<BotcRole> potentialDemons = selectedPack.getAllDemons();
        String selectedPlayer = aCopyOfPlayers.get(rand.nextInt(aCopyOfPlayers.size()));
        BotcRole selectedDemonRole = potentialDemons.get(rand.nextInt(potentialDemons.size()));
        playerRoles.put(selectedPlayer, selectedDemonRole);
        // Ensure that the selected player has not been selected before
        aCopyOfPlayers.remove(selectedPlayer);
        logPlayerRoles();


        if (selectedDemonRole.getSetup()) {
            BotcFab.LOGGER.info("Demon selected has special setups");
        }

        // Assign the minions
        int numberOfMinions = roleAllocations.get(MINION_INDEX);
        ArrayList<BotcRole> potentialMinions = selectedPack.getAllMinions();
        assignRandomRolesTeam(numberOfMinions, potentialMinions);
        BotcFab.LOGGER.info("Minions have been assigned");
        logPlayerRoles();

        // Assign the outsiders
        int numberOfOutsiders = roleAllocations.get(OUTSIDER_INDEX);
        ArrayList<BotcRole> potentialOutsiders = selectedPack.getAllOutsiders();
        assignRandomRolesTeam(numberOfOutsiders, potentialOutsiders);
        BotcFab.LOGGER.info("Outsiders have been assigned");
        logPlayerRoles();

        // Assign the townsfolk
        int numberOfTownsfolk = roleAllocations.get(TOWNSFOLK_INDEX);
        ArrayList<BotcRole> potentialTownsfolk = selectedPack.getAllTownsfolk();

        assignRandomRolesTeam(numberOfTownsfolk, potentialTownsfolk);
        BotcFab.LOGGER.info("Townsfolk have been assigned");
        logPlayerRoles();

        // time to fuck around and find out with the specials

    }
}
