package model;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Faction {
    private final String name;
    private String emoji;
    private final String player;
    private final String userName;
    private final int handLimit;
    private int spice;
    private final List<TechToken> techTokens;
    private Force reserves;
    private Force specialReserves;
    private int frontOfShieldSpice;
    private int freeRevival;
    private boolean hasMiningEquipment;
    private final List<TreacheryCard> treacheryHand;
    private final List<TraitorCard> traitorHand;
    private final List<Leader> leaders;
    private final List<Resource> resources;

public Faction(String name, String player, String userName, Game gameState) {

        if (name.equals("Harkonnen")) this.handLimit = 8;
        else if (name.equals("CHOAM")) this.handLimit = 5;
        else this.handLimit = 4;
        this.name = name;
        this.player = player;
        this.userName = userName;
        this.treacheryHand = new LinkedList<>();
        this.frontOfShieldSpice = 0;
        this.hasMiningEquipment = false;

        this.traitorHand = new LinkedList<>();
        this.leaders = new LinkedList<>();
        this.resources = new LinkedList<>();
        this.techTokens = new LinkedList<>();
        this.spice = 0;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Faction.class.getClassLoader().getResourceAsStream("Leaders.csv"))
        ));
        CSVParser csvParser = null;
        try {
            csvParser = CSVParser.parse(bufferedReader, CSVFormat.EXCEL);
        } catch (IOException e) {
            e.printStackTrace();
        }

    LinkedList<TraitorCard> traitorDeck = gameState.getTraitorDeck();

        for (CSVRecord csvRecord : csvParser) {
            if (csvRecord.get(0).equals(this.getName())) {
                TraitorCard traitorCard = new TraitorCard(
                        csvRecord.get(1),
                        csvRecord.get(0),
                        Integer.parseInt(csvRecord.get(2))
                );
                this.leaders.add(new Leader(csvRecord.get(1), Integer.parseInt(csvRecord.get(2))));
                traitorDeck.add(traitorCard);
            }
        }

       switch (name.toUpperCase()) {
           case "ATREIDES" -> {
               this.spice = 10;
               this.freeRevival = 2;
               this.hasMiningEquipment = true;
               this.reserves = new Force("Atreides", 10);
               this.emoji = "<:atreides:991763327996923997>";
               this.resources.add(new IntegerResource("forces_lost", 0, 0, 7));
               gameState.getTerritories().get("Arrakeen").getForces().add(new Force("Atreides", 10));
           }
           case "HARKONNEN" -> {
               this.spice = 10;
               this.freeRevival = 2;
               this.hasMiningEquipment = true;
               this.reserves = new Force("Harkonnen", 10);
               this.emoji = "<:harkonnen:991763320333926551>";
               gameState.getTerritories().get("Carthag").getForces().add(new Force("Harkonnen", 10));
           }
           case "EMPEROR" -> {
               this.spice = 10;
               this.freeRevival = 1;
               this.reserves = new Force("Emperor", 15);
               this.specialReserves = new Force("Emperor*", 5);
               this.emoji = "<:emperor:991763323454500914>";
           }
           case "GUILD" -> {
               this.spice = 5;
               this.freeRevival = 1;
               this.reserves = new Force("Guild", 15);
               this.emoji = "<:guild:991763321290244096>";
               gameState.getTerritories().get("Tuek's Sietch").getForces().add(new Force("Guild", 5));
           }
           case "FREMEN" -> {
               this.spice = 3;
               this.freeRevival = 3;
               this.reserves = new Force("Fremen", 17);
               this.specialReserves = new Force("Fremen*", 3);
               this.emoji = "<:fremen:991763322225577984>";
           }
           case "BG" -> {
               this.spice = 5;
               this.freeRevival = 1;
               this.reserves = new Force("BG", 20);
               this.emoji = "<:bg:991763326830911519>";
           }
           case "BT" -> {
               this.spice = 5;
               this.freeRevival = 2;
               this.reserves = new Force("BT", 20);
               this.emoji = "<:bt:991763325576810546>";
           }
           case "IX" -> {
               this.spice = 10;
               this.freeRevival = 1;
               this.reserves = new Force("Ix", 10);
               this.specialReserves = new Force("Ix*", 4);
               this.emoji = "<:ix:991763319406997514>";
               gameState.getTerritories().get("Hidden Mobile Stronghold").getForces().add(new Force("Ix", 3));
               gameState.getTerritories().get("Hidden Mobile Stronghold").getForces().add(new Force("Ix*", 3));
           }
           case "CHOAM" -> {
               this.spice = 2;
               this.freeRevival = 0;
               this.reserves = new Force("CHOAM", 20);
               this.emoji = "<:choam:991763324624703538>";
           }
           case "RICH" -> {
               this.spice = 5;
               this.freeRevival = 2;
               this.reserves = new Force("Rich", 20);
               this.emoji = "<:rich:991763318467465337>";
               this.resources.add( new IntegerResource("no field", 0, 0, 0));
               this.resources.add(new IntegerResource("no field", 3, 3, 3));
               this.resources.add(new IntegerResource("no field", 5, 5, 5));
               this.resources.add(new Resource<List<TreacheryCard>>("cache", new ArrayList<>()));
               List<TreacheryCard> cache = (List<TreacheryCard>) this.getResource("cache");
               cache.add(new TreacheryCard("Ornithoper", "Special - Movement"));
               cache.add(new TreacheryCard("Residual Poison", "Special"));
               cache.add(new TreacheryCard("Semuta Drug", "Special"));
               cache.add(new TreacheryCard("Stone Burner", "Weapon - Special"));
               cache.add(new TreacheryCard("Mirror Weapon", "Weapon - Special"));
               cache.add(new TreacheryCard("Portable Snooper", "Defense - Poison"));
               cache.add(new TreacheryCard("Distrans", "Special"));
               cache.add(new TreacheryCard("Juice of Sapho", "Special"));
               cache.add(new TreacheryCard("Karama", "Special"));
               cache.add(new TreacheryCard("Nullentropy", "Special"));
           }
        }
    }

    public Resource getResource(String name) {
        return resources.stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .get();
    }

    public String getName() {
        return this.name;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getPlayer() {
        return player;
    }

    public String getUserName() {
        return userName;
    }

    public int getHandLimit() {
        return handLimit;
    }

    public List<TreacheryCard> getTreacheryHand() {
        return treacheryHand;
    }

    public void addTreacheryCard(TreacheryCard card) {
        treacheryHand.add(card);
    }

    public void removeTreacheryCard(String cardName) {
        TreacheryCard remove = null;
        for (TreacheryCard card : treacheryHand) {
            if (card.name().equals(cardName)) {
               remove = card;
            }
        }
        if (remove != null) treacheryHand.remove(remove);
        else throw new IllegalArgumentException("Card not found");
    }

    public List<TraitorCard> getTraitorHand() {
        return traitorHand;
    }

    public int getSpice() {
        return spice;
    }

    public void setSpice(int spice) {
        this.spice = spice;
    }

    public void addSpice(int spice) {
        if (spice < 0) throw new IllegalArgumentException("You cannot add a negative number.");
        this.spice += spice;
    }

    public void subtractSpice(int spice) {
        this.spice -= Math.abs(spice);
        if (this.spice < 0) throw new IllegalStateException("Faction cannot spend more spice than they have.");
    }

    public void addResource(Resource resource) {
        resources.add(resource);
    }

    public void removeResource(String resourceName) {
        this.resources.remove(getResource(resourceName));
    }

    public Force getReserves() {
        return reserves;
    }

    public Force getSpecialReserves() {
        return specialReserves;
    }

    public int getFrontOfShieldSpice() {
        return frontOfShieldSpice;
    }

    public int getFreeRevival() {
        return freeRevival;
    }

    public List<Leader> getLeaders() {
        return leaders;
    }

    public Leader removeLeader(String name) {
        Leader remove = null;
        for (Leader leader : leaders) {
            if (leader.name().equals(name)) {
                remove = leader;
            }
        }
        if (remove == null) throw new IllegalArgumentException("Leader not found.");
        leaders.remove(remove);
        return remove;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setFrontOfShieldSpice(int frontOfShieldSpice) {
        this.frontOfShieldSpice = frontOfShieldSpice;
    }

    public void addFrontOfShieldSpice(int amount) {
        this.frontOfShieldSpice += amount;
    }

    public boolean hasMiningEquipment() {
        return hasMiningEquipment;
    }

    public void setHasMiningEquipment(boolean hasMiningEquipment) {
        this.hasMiningEquipment = hasMiningEquipment;
    }

    public List<TechToken> getTechTokens() {
        return techTokens;
    }

}