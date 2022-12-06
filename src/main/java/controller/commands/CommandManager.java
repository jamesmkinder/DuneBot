package controller.commands;

import controller.Initializers;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import io.github.cdimascio.dotenv.Dotenv;
import model.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        List<Role> roles = event.getMember().getRoles();
        boolean isGameMaster = false;
        for (Role role : roles) {
            if (role.getName().equals("Game Master") || role.getName().equals("Dungeon Master")) {
                isGameMaster = true;
                break;
            }
        }
        if (!isGameMaster) {
            event.reply("You are not a Game Master!").setEphemeral(true).queue();
            return;
        }

        String name = event.getName();
        event.deferReply(true).queue();

        try {
            if (name.equals("newgame")) newGame(event);
//            else if (name.equals("clean")) clean(event);
            else {
                DiscordGame discordGame = new DiscordGame(event);
                Game gameState = discordGame.getGameState();

                switch (name) {
                    case "addfaction" -> addFaction(event, discordGame, gameState);
                    case "newfactionresource" -> newFactionResource(event, discordGame, gameState);
                    case "resourceaddorsubtract" -> resourceAddOrSubtract(event, discordGame, gameState);
                    case "removeresource" -> removeResource(event, discordGame, gameState);
                    case "draw" -> drawCard(event, discordGame, gameState);
                    case "peek" -> peek(event, discordGame, gameState);
//                    case "discard" -> discard(event, discordGame, gameState);
//                    case "transfercard" -> transferCard(event, discordGame, gameState);
//                    case "putback" -> putBack(event, discordGame, gameState);
//                    case "ixhandselection" -> ixHandSelection(event, discordGame, gameState);
//                    case "selecttraitor" -> selectTraitor(event, discordGame, gameState);
//                    case "placeforces" -> placeForces(event, discordGame, gameState);
//                    case "removeforces" -> removeForces(event, discordGame, gameState);
//                    case "display" -> displayGameState(event, discordGame, gameState);
//                    case "reviveforces" -> revival(event, discordGame, gameState);
//                    case "awardbid" -> awardBid(event, discordGame, gameState);
//                    case "killleader" -> killLeader(event, discordGame, gameState);
//                    case "reviveleader" -> reviveLeader(event, discordGame, gameState);
//                    case "setstorm" -> setStorm(event, discordGame, gameState);
//                    case "bgflip" -> bgFlip(event, discordGame, gameState);
//                    case "bribe" -> bribe(event, discordGame, gameState);
//                    case "mute" -> mute(discordGame, gameState);
//                    case "advancegame" -> advanceGame(event, discordGame, gameState);
                }
            }
            event.getHook().editOriginal("Command Done").queue();
        } catch (ChannelNotFoundException e) {
            e.printStackTrace();
            event.getHook().editOriginal("Channel not found!").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("An error occurred!").queue();
            e.printStackTrace();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        String optionName = event.getFocusedOption().getName();
        String searchValue = event.getFocusedOption().getValue();
        DiscordGame discordGame = new DiscordGame(event);

        try {
            Game gameState = discordGame.getGameState();
            switch (optionName) {
                case "factionname" -> event.replyChoices(CommandOptions.factions(gameState, searchValue)).queue();
            }
        } catch (ChannelNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {

        OptionData gameName = new OptionData(OptionType.STRING, "name", "e.g. 'Dune Discord #5: The Tortoise and the Hajr'", true);
        OptionData gameRole = new OptionData(OptionType.ROLE, "gamerole", "The role you created for the players of this game", true);
        OptionData modRole = new OptionData(OptionType.ROLE, "modrole", "The role you created for the mod(s) of this game", true);
        OptionData user = new OptionData(OptionType.USER, "player", "The player for the faction", true);
        OptionData allFactions = new OptionData(OptionType.STRING, "factionname", "The faction", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich");
        OptionData faction = new OptionData(OptionType.STRING, "factionname", "The faction", true)
                .setAutoComplete(true);
        OptionData resourceName = new OptionData(OptionType.STRING, "resource", "The name of the resource", true);
        OptionData isNumber = new OptionData(OptionType.BOOLEAN, "isanumber", "Set true if it is a numerical value, false otherwise", true);
        OptionData resourceValNumber = new OptionData(OptionType.INTEGER, "numbervalue", "Set the initial value if the resource is a number (leave blank otherwise)");
        OptionData resourceValString = new OptionData(OptionType.STRING, "othervalue", "Set the initial value if the resource is not a number (leave blank otherwise)");
        OptionData amount = new OptionData(OptionType.INTEGER, "amount", "Amount to be added or subtracted (e.g. -3, 4)", true);
        OptionData message = new OptionData(OptionType.STRING, "message", "Message for spice transactions", false);
        OptionData password = new OptionData(OptionType.STRING, "password", "You really aren't allowed to run this command unless Voiceofonecrying lets you", true);
        OptionData deck = new OptionData(OptionType.STRING, "deck", "The deck", true)
                .addChoice("Treachery Deck", "treachery_deck")
                .addChoice("Traitor Deck", "traitor_deck");
        OptionData destination = new OptionData(OptionType.STRING, "destination", "Where the card is being drawn to (name a faction to draw to hand, or 'discard')", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich")
                .addChoice("discard", "discard");
        OptionData numberToPeek = new OptionData(OptionType.STRING, "number", "The number of cards you want to peek (default is 1).");
        OptionData card = new OptionData(OptionType.STRING, "card", "The card.", true);
        OptionData sender = new OptionData(OptionType.STRING, "sender", "The one giving the card", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich")
                .addChoice("Add from discard", "discard");
        OptionData recipient = new OptionData(OptionType.STRING, "recipient", "The recipient", true)
                .addChoice("Atreides", "Atreides")
                .addChoice("Harkonnen", "Harkonnen")
                .addChoice("Emperor", "Emperor")
                .addChoice("Fremen", "Fremen")
                .addChoice("Spacing Guild", "Guild")
                .addChoice("Bene Gesserit", "BG")
                .addChoice("Ixian", "Ix")
                .addChoice("Tleilaxu", "BT")
                .addChoice("CHOAM", "CHOAM")
                .addChoice("Richese", "Rich")
                .addChoice("Place up for bid", "market");
        OptionData bottom = new OptionData(OptionType.BOOLEAN, "bottom", "Place on bottom?", true);
        OptionData traitor = new OptionData(OptionType.STRING, "traitor", "The name of the traitor", true);
        OptionData territory = new OptionData(OptionType.STRING, "mostlikelyterritories", "The name of the territory (more 'important' territories).")
                .addChoice("Cielago North", "Cielago North")
                .addChoice("Cielago South", "Cielago South")
                .addChoice("False Wall South", "False Wall South")
                .addChoice("South Mesa", "South Mesa")
                .addChoice("False Wall East", "False Wall East")
                .addChoice("The Minor Erg", "The Minor Erg")
                .addChoice("Tuek's Sietch", "Tuek's Sietch")
                .addChoice("Red Chasm", "Red Chasm")
                .addChoice("Imperial Basin", "Imperial Basin")
                .addChoice("Old Gap", "Old Gap")
                .addChoice("Sihaya Ridge", "Sihaya Ridge")
                .addChoice("Arrakeen", "Arrakeen")
                .addChoice("Broken Land", "Broken Land")
                .addChoice("Carthag", "Carthag")
                .addChoice("Hagga Basin", "Hagga Basin")
                .addChoice("Rock Outcroppings", "Rock Outcroppings")
                .addChoice("Sietch Tabr", "Sietch Tabr")
                .addChoice("Funeral Plain", "Funeral Plain")
                .addChoice("The Great Flat", "The Great Flat")
                .addChoice("False Wall West", "False Wall West")
                .addChoice("Habbanya Erg", "Habbanya Erg")
                .addChoice("Habbanya Ridge Flat", "Habbanya Ridge Flat")
                .addChoice("Habbanya Sietch", "Habbanya Sietch")
                .addChoice("Wind Pass North", "Wind Pass North")
                .addChoice("Polar Sink", "Polar Sink");
        OptionData otherTerritory = new OptionData(OptionType.STRING, "otherterritories", "Added for completeness, less likely to use.")
                .addChoice("Cielago Depression","Cielago Depression")
                .addChoice("Meridian", "Meridian")
                .addChoice("Cielago East", "Cielago East")
                .addChoice("Harg Pass", "Harg Pass")
                .addChoice("Pasty Mesa", "Pasty Mesa")
                .addChoice("Gara Kulon", "Gara Kulon")
                .addChoice("Basin", "Basin")
                .addChoice("Hole in the Rock", "Hole in the Rock")
                .addChoice("Rim Wall West", "Rim Wall West")
                .addChoice("Arsunt", "Arsunt")
                .addChoice("Tsimpo", "Tsimpo")
                .addChoice("Plastic Basin", "Plastic Basin")
                .addChoice("Bight of the Cliff", "Bight of the Cliff")
                .addChoice("Wind Pass", "Wind Pass")
                .addChoice("The Greater Flat", "The Greater Flat")
                .addChoice("Cielago West", "Cielago West")
                .addChoice("Shield Wall", "Shield Wall");
        OptionData sector = new OptionData(OptionType.INTEGER, "sector", "The storm sector");
        OptionData starred = new OptionData(OptionType.BOOLEAN, "starred", "Are they starred forces?", true);
        OptionData spent = new OptionData(OptionType.INTEGER, "spent", "How much was spent on the card.", true);
        OptionData revived = new OptionData(OptionType.INTEGER, "revived", "How many are being revived.", true);
        OptionData data = new OptionData(OptionType.STRING, "data", "What data to display", true)
                .addChoice("Territories", "territories")
                .addChoice("Decks and Discards", "dnd")
                .addChoice("Phase, Turn, and everything else", "etc")
                .addChoice("Faction Info", "factions");
        OptionData isShipment = new OptionData(OptionType.BOOLEAN, "isshipment", "Is this placement a shipment?", true);
        OptionData toTanks = new OptionData(OptionType.BOOLEAN, "totanks", "Remove these forces to the tanks (true) or to reserves (false)?", true);
        OptionData leader = new OptionData(OptionType.STRING, "leader", "The leader.", true);

        //add new slash command definitions to commandData list
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("clean", "FOR TEST ONLY: DO NOT RUN").addOptions(password));
        commandData.add(Commands.slash("newgame", "Creates a new Dune game instance.").addOptions(gameName, gameRole, modRole));
        commandData.add(Commands.slash("addfaction", "Register a user to a faction in a game").addOptions(allFactions, user));
        commandData.add(Commands.slash("newfactionresource", "Initialize a new resource for a faction").addOptions(faction, resourceName, isNumber, resourceValNumber, resourceValString));
        commandData.add(Commands.slash("resourceaddorsubtract", "Performs basic addition and subtraction of numerical resources for factions").addOptions(faction, resourceName, amount, message));
        commandData.add(Commands.slash("removeresource", "Removes a resource category entirely (Like if you want to remove a Tech Token from a player)").addOptions(faction, resourceName));
        commandData.add(Commands.slash("draw", "Draw a card from the top of a deck.").addOptions(deck, destination));
        commandData.add(Commands.slash("peek", "Peek at the top n number of cards of a deck without moving them.").addOptions(deck, numberToPeek));
        commandData.add(Commands.slash("discard", "Move a card from a faction's hand to the discard pile").addOptions(faction, card));
        commandData.add(Commands.slash("transfercard", "Move a card from one faction's hand to another").addOptions(sender, card, recipient));
        commandData.add(Commands.slash("putback", "Used for the Ixian ability to put a treachery card on the top or bottom of the deck.").addOptions(card, bottom));
        commandData.add(Commands.slash("advancegame", "Send the game to the next phase, turn, or card (in bidding round"));
        commandData.add(Commands.slash("ixhandselection", "Only use this command to select the Ix starting treachery card").addOptions(card));
        commandData.add(Commands.slash("selecttraitor", "Select a starting traitor from hand.").addOptions(faction, traitor));
        commandData.add(Commands.slash("placeforces", "Place forces from reserves onto the surface").addOptions(faction, amount, isShipment, starred, territory, otherTerritory, sector));
        commandData.add(Commands.slash("removeforces", "Remove forces from the board.").addOptions(faction, amount, toTanks, starred, territory, otherTerritory, sector));
        commandData.add(Commands.slash("awardbid", "Designate that a card has been won by a faction during bidding phase.").addOptions(faction, spent));
        commandData.add(Commands.slash("reviveforces", "Revive forces for a faction.").addOptions(faction, revived, starred));
        commandData.add(Commands.slash("display", "Displays some element of the game to the mod.").addOptions(data));
        commandData.add(Commands.slash("setstorm", "Sets the storm to an initial sector.").addOptions(sector));
        commandData.add(Commands.slash("killleader", "Send a leader to the tanks.").addOptions(faction, leader));
        commandData.add(Commands.slash("reviveleader", "Revive a leader from the tanks.").addOptions(faction, leader));
        commandData.add(Commands.slash("bgflip", "Flip BG forces to advisor or fighter.").addOptions(territory, otherTerritory, sector));
        commandData.add(Commands.slash("mute", "Toggle mute for all bot messages."));
        commandData.add(Commands.slash("bribe", "Record a bribe transaction").addOptions(faction, recipient, amount));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    public void newGame(SlashCommandInteractionEvent event) throws ChannelNotFoundException, IOException {
        Role gameRole = event.getOption("gamerole").getAsRole();
        Role modRole = event.getOption("modrole").getAsRole();
        String name = event.getOption("name").getAsString();
        event.getGuild()
                .createCategory(name)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(modRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(gameRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .complete();

        Category category = event.getGuild().getCategoriesByName(name, true).get(0);

        category.createTextChannel("bot-data")
                .addPermissionOverride(gameRole, null, EnumSet.of(Permission.VIEW_CHANNEL)).complete();
        category.createTextChannel("chat")
                        .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), null).complete();
        category.createTextChannel("turn-summary")
                .addPermissionOverride(gameRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND))
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("game-actions")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("bribes")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("bidding-phase")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("rules")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("pre-game-voting")
                .addPermissionOverride(event.getGuild().getRolesByName("Observer", true).get(0), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)).complete();
        category.createTextChannel("mod-info")
                .addPermissionOverride(gameRole, null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).complete();

        DiscordGame discordGame = new DiscordGame(event, category);
        discordGame.getTextChannel("rules").sendMessage("""
            <:DuneRulebook01:991763013814198292>  Dune rulebook: https://www.gf9games.com/dunegame/wp-content/uploads/Dune-Rulebook.pdf
            <:weirding:991763071775297681>  Dune FAQ Nov 20: https://www.gf9games.com/dune/wp-content/uploads/2020/11/Dune-FAQ-Nov-2020.pdf
            <:ix:991763319406997514> <:bt:991763325576810546>  Ixians & Tleilaxu Rules: https://www.gf9games.com/dunegame/wp-content/uploads/2020/09/IxianAndTleilaxuRulebook.pdf
            <:choam:991763324624703538> <:rich:991763318467465337> CHOAM & Richese Rules: https://www.gf9games.com/dune/wp-content/uploads/2021/11/CHOAM-Rulebook-low-res.pdf""").queue();

        Game game = new Game();
        game.addTerritories(Initializers.buildBoard());
        game.addDeck(Initializers.buildTreacheryDeck());
        game.addDeck(new Deck("treachery_discard"));
        game.addDeck(Initializers.buildStormDeck());
        game.addDeck(new Deck("traitor"));
        game.addDeck(Initializers.buildSpiceDeck());
        game.addDeck(new Deck("spice_discardA"));
        game.addDeck(new Deck("spice_discardB"));



        game.addResource(new IntegerResource("turn", 0, 0, 10));
        game.addResource(new IntegerResource("phase", 0, 0, 9));
        game.addResource(new IntegerResource("storm", 18, 1, 18));
        game.addResource(new BooleanResource("shieldwallbroken", false));

//        gameResources.put("market", new JSONArray());
//        gameResources.put("tanks_forces", new JSONObject());
//        gameResources.put("tanks_leaders", new JSONArray());
//        gameResources.put("turn_order", new JSONObject());

        game.setGameRole(gameRole.getName());
        game.setModRole(modRole.getName());
        game.setMute(false);
        discordGame.setGameState(game);
        discordGame.pushGameState();
    }

    public void addFaction(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, IOException {
        TextChannel modInfo = discordGame.getTextChannel("mod-info");
        if (!gameState.getResource("turn").getValue().equals(0)) {
            modInfo.sendMessage("The game has already started, you can't add more factions!").queue();
            return;
        }
        if (gameState.getFactions().size() >= 6) {
            modInfo.sendMessage("This game is already full!").queue();
            return;
        }
        String factionName = event.getOption("factionname").getAsString();
        if (gameState.hasFaction(factionName)) {
            modInfo.sendMessage("This faction has already been taken!").queue();
            return;
        }

        Faction faction = new Faction(factionName, event.getOption("player").getAsUser().getAsMention(), event.getOption("player").getAsMember().getNickname());

        Initializers.newFaction(faction, gameState);

        Category game = discordGame.getGameCategory();
        discordGame.pushGameState();
        game.createTextChannel(factionName.toLowerCase() + "-info").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(event.getGuild().getRolesByName(gameState.getGameRole(), true).get(0), null, EnumSet.of(Permission.VIEW_CHANNEL)).queue();

        game.createTextChannel(factionName.toLowerCase() + "-chat").addPermissionOverride(event.getOption("player").getAsMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(event.getGuild().getRolesByName(gameState.getGameRole(), true).get(0), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null).queue();

    }

    public void newFactionResource(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String factionName = event.getOption("factionname").getAsString();
        if (!gameState.hasFaction(factionName)) {
            event.getChannel().sendMessage("That faction is not in this game!").queue();
            return;
        }

        Faction faction = gameState.getFaction(factionName);

        if (event.getOption("isanumber").getAsBoolean()) {
            Resource<Integer> resource = new Resource<>(
                    event.getOption("resource").getAsString(),
                    event.getOption("numbervalue").getAsInt()
            );

            faction.addResource(resource);
        } else {
            Resource<String> resource = new Resource<>(
                    event.getOption("resource").getAsString(),
                    event.getOption("othervalue").getAsString()
            );

            faction.addResource(resource);
        }

        discordGame.pushGameState();
    }

    public void resourceAddOrSubtract(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException {
        String factionName = event.getOption("factionname").getAsString();
        String resourceName = event.getOption("resource").getAsString();
        int amount = event.getOption("amount").getAsInt();

        Resource resource = gameState.getFaction(factionName).getResource(resourceName);

        if (resource instanceof IntegerResource) {
            ((IntegerResource) resource).addValue(amount);
        } else {
            throw new InvalidGameStateException("Resource is not numeric");
        }

        if (resource.getName().equals("spice")) {
            String message = event.getOption("message") == null ? "custom transaction" : event.getOption("message").getAsString();
            spiceMessage(discordGame, Math.abs(amount), event.getOption("factionname").getAsString(), message, amount >= 0);

        }

        writeFactionInfo(event, gameState, discordGame, event.getOption("factionname").getAsString());
        discordGame.pushGameState();
    }

    public void removeResource(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        Faction faction = gameState.getFaction(event.getOption("factionname").getAsString());
        faction.removeResource(event.getOption("resource").getAsString());
        discordGame.pushGameState();
    }

    public void drawCard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String sourceDeckName = event.getOption("deck").getAsString();
        String sourceFactionName = event.getOption("deck_faction").getAsString();
        String destinationDeckName = event.getOption("destination").getAsString();
        String destinationFactionName = event.getOption("destination_faction").getAsString();

        Deck sourceDeck = sourceFactionName.isEmpty() ? gameState.getDeck(sourceDeckName) :
                gameState.getFaction(sourceFactionName).getDeck(sourceDeckName);

        Deck destinationDeck = destinationFactionName.isEmpty() ? gameState.getDeck(destinationDeckName) :
                gameState.getFaction(destinationFactionName).getDeck(destinationDeckName);

        Card card = sourceDeck.pop();
        destinationDeck.push(card);

        if (!sourceFactionName.isEmpty()) {
            writeFactionInfo(event, gameState, discordGame, sourceFactionName);
        }

        if (!destinationFactionName.isEmpty()) {
            writeFactionInfo(event, gameState, discordGame, destinationFactionName);
        }

        discordGame.pushGameState();
    }

//    public String drawCard(Game gameState, String deckName, String faction) {
//        JSONArray deck = gameState.getDeck(deckName);
//
//        if (deck.length() == 0 && deckName.equals("spice_deck")) {
//            JSONArray discardA = gameState.getResources().getJSONArray("spice_discardA");
//            JSONArray discardB = gameState.getResources().getJSONArray("spice_discardB");
//
//            for (Object o : discardA) {
//                deck.put(o);
//            }
//            for (Object o : discardB) {
//                deck.put(o);
//            }
//            discardA.clear();
//            discardB.clear();
//            shuffle(deck);
//
//        }
//
//        String drawn = deck.getString(deck.length() - 1);
//        if (gameState.getResources().getInt("turn") == 1 && drawn.equals("Shai-Hulud")) {
//            shuffle(deck);
//            return drawCard(gameState, deckName, faction);
//        }
//        if (deckName.equals("spice_deck")) {
//            //In this case, faction is used as a flag to determine if this is the first or second spice blow of the turn.
//            if (faction.equals("A: ")) {
//                gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray("spice_discardA").put(drawn);
//            }
//            else {
//                gameState.getJSONObject("game_state").getJSONObject("game_resources").getJSONArray("spice_discardB").put(drawn);
//            }
//            deck.remove(deck.length() - 1);
//            if (!drawn.equals("Shai-Hulud") && gameState.getResources().getInt("storm") != gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(drawn.split("-")[0].strip()).getInt("sector")) {
//                int spice = gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(drawn.split("-")[0].strip()).getInt("spice");
//                gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(drawn.split("-")[0].strip()).put("spice", spice + Integer.parseInt(drawn.split("-")[1].strip()));
//            }
//            if (drawn.equals("Shai-Hulud")) drawn += ", " + drawCard(gameState, deckName, faction);
//            return drawn.split("\\(")[0];
//        }
//        JSONObject resources = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(faction).getJSONObject("resources");
//        switch (deckName) {
//            case "traitor_deck" -> resources.getJSONArray("traitors").put(drawn);
//            case "treachery_deck" -> resources.getJSONArray("treachery_hand").put(drawn);
//
//        }
//        deck.remove(deck.length() - 1);
//        return drawn;
//    }

    public void peek(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String deckName = event.getOption("deck").getAsString();
        String deckFaction = event.getOption("deck_faction").getAsString();

        Deck deck = deckFaction.isEmpty() ? gameState.getDeck(deckName) :
                gameState.getFaction(deckFaction).getDeck(deckName);

        StringBuilder message = new StringBuilder();
        message.append("Top of deck: \n");
        int number = 1;
        if (event.getOption("number") != null) {
            number = event.getOption("number").getAsInt();
        }

        List<Card> cards = deck.getTopCards(number);

        for (int i = 0; i < number; i++) {
            message.append(cards.get(i).getName());
            message.append("\n");
        }
        event.getUser().openPrivateChannel().queue((privateChannel -> privateChannel.sendMessage(message).queue()));
    }

//    public void discard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONObject faction = gameState.getFaction(event.getOption("factionname").getAsString());
//
//        JSONArray hand = faction.getJSONObject("resources").getJSONArray("treachery_hand");
//        int i = 0;
//
//        for (; i < hand.length(); i++) {
//            String card = hand.getString(i);
//            if (card.toLowerCase().contains(event.getOption("card").getAsString().toLowerCase())) {
//                gameState.getResources().getJSONArray("treachery_discard").put(card);
//                break;
//            }
//        }
//        hand.remove(i);
//        writeFactionInfo(event, gameState, discordGame, faction.getString("name"));
//        discordGame.pushGameState();
//    }

//    public void transferCard(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONArray giverHand;
//        JSONArray receiverHand;
//        JSONObject giver = null;
//        JSONObject receiver = null;
//
//        if (event.getOption("recipient").getAsString().equals("market")) {
//            receiverHand = gameState.getResources().getJSONArray("market");
//        }
//        else {
//            receiver = gameState.getFaction(event.getOption("recipient").getAsString());
//            receiverHand = receiver.getJSONObject("resources").getJSONArray("treachery_hand");
//        }
//
//        if (event.getOption("sender").getAsString().equals("discard")) giverHand = gameState.getResources().getJSONArray("treachery_discard");
//        else {
//            giver = gameState.getFaction(event.getOption("sender").getAsString());
//            giverHand = giver.getJSONObject("resources").getJSONArray("treachery_hand");
//        }
//
//        if ((giver == null && !event.getOption("sender").getAsString().equals("discard")) ||
//                (receiver == null && !event.getOption("recipient").getAsString().equals("market"))) {
//            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("At least one of the selected factions is not playing in this game!").queue());
//            return;
//        }
//
//        if ((receiver != null && receiver.getString("name").equals("Harkonnen") && receiverHand.length() >= 8) || receiverHand.length() >= 4) {
//            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("The recipient's hand is full!").queue());
//            return;
//        }
//        int i = 0;
//
//        boolean cardFound = false;
//        for (; i < giverHand.length(); i++) {
//            String card = giverHand.getString(i);
//            if (card.toLowerCase().contains(event.getOption("card").getAsString())) {
//                cardFound = true;
//                receiverHand.put(card);
//                break;
//            }
//        }
//        if (!cardFound) {
//            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Could not find that card!").queue());
//            return;
//        }
//        giverHand.remove(i);
//        if (giver != null) writeFactionInfo(event, gameState, discordGame, giver.getString("name"));
//        if (receiver != null) writeFactionInfo(event, gameState, discordGame, receiver.getString("name"));
//        discordGame.pushGameState();
//    }

//    public void putBack(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONArray market = gameState.getResources().getJSONArray("market");
//        int i = 0;
//        boolean found = false;
//        for (; i < market.length(); i++) {
//            if (market.getString(i).contains(event.getOption("card").getAsString())) {
//                if (!event.getOption("bottom").getAsBoolean()) gameState.getResources().getJSONArray("treachery_deck")
//                        .put(market.getString(i));
//                else gameState.getResources().getJSONArray("treachery_deck").put(0, market.getString(i));
//                found = true;
//                break;
//            }
//        }
//        if (!found) {
//            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Card not found, are you sure it's there?").queue());
//            return;
//        }
//        market.remove(i);
//        shuffle(market);
//        discordGame.pushGameState();
//    }

//    public void ixHandSelection(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONArray hand = gameState.getFaction("Ix").getJSONObject("resources").getJSONArray("treachery_hand");
//        shuffle(hand);
//        for (int i = 0; i < hand.length(); i++) {
//            if (hand.getString(i).toLowerCase().contains(event.getOption("card").getAsString())) continue;
//            gameState.getResources().getJSONArray("treachery_deck").put(hand.getString(i));
//        }
//        int shift = 0;
//        int length = hand.length() - 1;
//        for (int i = 0; i < length; i++) {
//            if (hand.getString(0).toLowerCase().contains(event.getOption("card").getAsString())) {
//                shift = 1;
//                continue;
//            }
//            hand.remove(shift);
//        }
//        writeFactionInfo(event, gameState, discordGame, "Ix");
//        discordGame.pushGameState();
//    }

//    public void awardBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        if (event.getOption("factionname").getAsString().equals("harkonnen")
//                && gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getJSONArray("treachery_hand").length() > 7
//                || gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getJSONArray("treachery_hand").length() > 3) {
//            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Player's hand is full, they cannot bid on this card!").queue());
//            return;
//        }
//        try {
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getJSONArray("treachery_hand").put(gameState.getResources().getJSONArray("market").getString(0));
//            discordGame.sendMessage("turn-summary", gameState.getFaction(event.getOption("factionname").getAsString()).getString("emoji") + " wins card up for bid for " + event.getOption("spent").getAsInt() + " <:spice4:991763531798167573>");
//        } catch (JSONException e) {
//            discordGame.sendMessage("mod-info", "No more cards up for bid.  Please advance the game.");
//            return;
//        }
//        gameState.getResources().getJSONArray("market").remove(0);
//
//        if (gameState.getFaction("Atreides") != null && gameState.getResources().getJSONArray("market").length() > 0) {
//            discordGame.sendMessage("atreides-info", "The next card up for bid is <:treachery:991763073281040518> "
//                            + gameState.getResources()
//                            .getJSONArray("market")
//                            .getString(0).split("\\|")[0] + " <:treachery:991763073281040518>");
//        }
//
//        if (gameState.getResources().getJSONArray("market").length() > 0) {
//                StringBuilder message = new StringBuilder();
//                int cardNumber = gameState.getResources().getInt("market_size") - gameState.getResources().getJSONArray("market").length();
//                message.append("R").append(gameState.getResources().getInt("turn")).append(":C").append(cardNumber + 1).append("\n");
//                int firstBid = Math.ceilDiv(gameState.getResources().getInt("storm"), 3) + 1 + cardNumber;
//                for (int i = 0; i < gameState.getJSONObject("game_state").getJSONObject("factions").length(); i++) {
//                    int playerPosition = (firstBid + i) % 6;
//                    if (playerPosition == 0) playerPosition = 6;
//                    String faction = gameState.getResources().getJSONObject("turn_order").getString(String.valueOf(playerPosition));
//                    int length = gameState.getFaction(faction).getJSONObject("resources").getJSONArray("treachery_hand").length();
//                    if (faction.equals("Harkonnen") && length < 8 || faction.equals("CHOAM") && length < 5 ||
//                            !(faction.equals("Harkonnen") || faction.equals("CHOAM")) && length < 4) {
//                        message.append(gameState.getFaction(faction).getString("emoji")).append(":");
//                        if (i == 0) message.append(" ").append(gameState.getFaction(faction).getString("player"));
//                        message.append("\n");
//                    }
//                }
//                discordGame.sendMessage("bidding-phase", message.toString());
//        }
//        if (event.getOption("factionname").getAsString().equals("Harkonnen") && gameState.getFaction("Harkonnen").getJSONObject("resources").getJSONArray("treachery_hand").length() < 8) {
//            drawCard(gameState, "treachery_deck", "Harkonnen");
//            discordGame.sendMessage("turn-summary", gameState.getFaction(event.getOption("factionname").getAsString()).getString("emoji") + " draws another card from the <:treachery:991763073281040518> deck.");
//        }
//        int spice = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("spice");
//        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("spice");
//        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("spice", spice - event.getOption("spent").getAsInt());
//        spiceMessage(discordGame, event.getOption("spent").getAsInt(), event.getOption("factionname").getAsString(), "R" +
//                gameState.getResources().getInt("turn") + ":C" + (gameState.getResources().getInt("market_size") - gameState.getResources().getJSONArray("market").length()), false);
//        writeFactionInfo(event, gameState, discordGame, event.getOption("factionname").getAsString());
//        if (gameState.getJSONObject("game_state").getJSONObject("factions").keySet().contains("Emperor") && !event.getOption("factionname").getAsString().equals("Emperor")) {
//            int spiceEmp = gameState.getFaction("Emperor").getJSONObject("resources").getInt("spice");
//            gameState.getFaction("Emperor").getJSONObject("resources").remove("spice");
//            gameState.getFaction("Emperor").getJSONObject("resources").put("spice", spiceEmp + event.getOption("spent").getAsInt());
//            discordGame.sendMessage("turn-summary", gameState.getFaction("Emperor").getString("emoji") + " is paid " + event.getOption("spent").getAsInt() + " <:spice4:991763531798167573>");
//            writeFactionInfo(event, gameState, discordGame, "Emperor");
//        }
//        spiceMessage(discordGame, event.getOption("spent").getAsInt(), "emperor", "R" +
//                gameState.getResources().getInt("turn") + ":C" + (gameState.getResources().getInt("market_size") - gameState.getResources().getJSONArray("market").length()), true);
//        discordGame.pushGameState();
//    }
//
    public void spiceMessage(DiscordGame discordGame, int amount, String faction, String message, boolean plus) throws ChannelNotFoundException {
        String plusSign = plus ? "+" : "-";
        for (TextChannel channel : discordGame.getTextChannels()) {
            if (channel.getName().equals(faction.toLowerCase() + "-info")) {
                discordGame.sendMessage(channel.getName(), plusSign + amount + "<:spice4:991763531798167573> for " + message);
            }
        }
    }

//    public void killLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONObject faction = gameState.getFaction(event.getOption("factionname").getAsString());
//        JSONArray tanks = gameState.getResources().getJSONArray("tanks_leaders");
//        int remove = 0;
//        boolean found = false;
//        String leader = "";
//        for (int i = 0; i < faction.getJSONObject("resources").getJSONArray("leaders").length(); i++) {
//            String leaderName = faction.getJSONObject("resources").getJSONArray("leaders").getString(i);
//            if (leaderName.toLowerCase().contains(event.getOption("leader").getAsString().toLowerCase())) {
//                remove = i;
//                found = true;
//                leader = leaderName;
//                break;
//            }
//        }
//        if (found) faction.getJSONObject("resources").getJSONArray("leaders").remove(remove);
//        else {
//            discordGame.getTextChannel("mod-info").sendMessage("Leader not found.").queue();
//            return;
//        }
//        tanks.put(leader);
//        discordGame.pushGameState();
//    }
//
//    public void reviveLeader(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONObject faction = gameState.getFaction(event.getOption("factionname").getAsString());
//        JSONArray tanks = gameState.getResources().getJSONArray("tanks_leaders");
//        int revive = 0;
//        boolean found = false;
//        String leader = "";
//        for (int i = 0; i < tanks.length(); i++) {
//            String leaderName = tanks.getString(i);
//            if (leaderName.toLowerCase().contains(event.getOption("leader").getAsString().toLowerCase())) {
//                revive = i;
//                found = true;
//                leader = leaderName;
//                break;
//            }
//        }
//        if (found) tanks.remove(revive);
//        else {
//            discordGame.getTextChannel("mod-info").sendMessage("Leader not found in the tanks.").queue();
//            return;
//        }
//        faction.getJSONObject("resources").getJSONArray("leaders").put(leader);
//        discordGame.pushGameState();
//    }
//
//    public void revival(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        String star = event.getOption("starred").getAsBoolean() ? "*" : "";
//        String faction = event.getOption("factionname").getAsString();
//        int tanksForces = gameState.getResources().getJSONObject("tanks_forces").getInt(faction + star);
//        gameState.getResources().getJSONObject("tanks_forces").remove(faction + star);
//        gameState.getResources().getJSONObject("tanks_forces").put(faction + star, tanksForces - event.getOption("revived").getAsInt());
//        int reserves = gameState.getFaction(faction).getJSONObject("resources").getInt("reserves" + star);
//        gameState.getFaction(faction).getJSONObject("resources").remove("reserves" + star);
//        gameState.getFaction(faction).getJSONObject("resources").put("reserves" + star, reserves + event.getOption("revived").getAsInt());
//        gameState.getFaction(faction).getJSONObject("resources").put("spice", gameState.getFaction(faction).getJSONObject("resources").getInt("spice") - 2 * event.getOption("revived").getAsInt());
//        spiceMessage(discordGame, 2 * event.getOption("revived").getAsInt(), faction, "Revivals", false);
//        if (!gameState.getJSONObject("game_state").getJSONObject("factions").isNull("bt")) {
//            gameState.getFaction("bt").getJSONObject("resources").put("spice", gameState.getFaction("bt").getJSONObject("resources").getInt("spice") + 2 * event.getOption("revived").getAsInt());
//            spiceMessage(discordGame, 2 * event.getOption("revived").getAsInt(), "bt", gameState.getFaction(faction).getString("emoji") + " revivals", true);
//            writeFactionInfo(event, gameState, discordGame, "bt");
//        }
//        writeFactionInfo(event, gameState, discordGame, faction);
//        discordGame.pushGameState();
//    }
//
//    public void advanceGame(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        Category game = discordGame.getGameCategory();
//
//        //Turn 0 is for the set-up for play section from the rules page 6.
//        if (gameState.getTurn() == 0) {
//            switch (gameState.getPhase()) {
//                //1. Positions
//                case 0 -> {
//                    shuffle(gameState.getDeck("spice_deck"));
//                    shuffle(gameState.getDeck("treachery_deck"));
//                    JSONObject turnOrder = gameState.getResources().getJSONObject("turn_order");
//                    int i = 1;
//                    JSONArray shuffled = new JSONArray();
//                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
//                        shuffled.put(faction);
//                    }
//                    shuffle(shuffled);
//                    for (Object faction: shuffled) {
//                        turnOrder.put(String.valueOf(i), faction);
//                        i++;
//                    }
//                    gameState.advancePhase();
//                    //If Bene Gesserit are present, time to make a prediction
//                    if (gameState.hasFaction("BG")) {
//                        discordGame.sendMessage("bg-chat", "Please make your secret prediction.");
//                    }
//                }
//                //2. Traitors
//                case 1 -> {
//                    shuffle(gameState.getDeck("traitor_deck"));
//                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
//                        if (!faction.equals("BT")) {
//                            for (int j = 0; j < 4; j++) {
//                                drawCard(gameState, "traitor_deck", faction);
//                            }
//                            writeFactionInfo(event, gameState, discordGame, faction);
//                        }
//                    }
//                    for (TextChannel channel : discordGame.getTextChannels()) {
//                            if (channel.getName().contains("-chat") && !channel.getName().contains("game") &&
//                                    !channel.getName().contains("harkonnen") && !channel.getName().contains("bt")) discordGame.sendMessage(channel.getName(), "Please select your traitor.");
//                        }
//
//
//                        gameState.advancePhase();
//                    //If Bene Tleilax are not present, advance past the Face Dancers draw
//                    if (!gameState.hasFaction("BT")) {
//                        gameState.advancePhase();
//                    }
//                    discordGame.sendMessage("turn-summary", "2. Traitors are being selected.");
//                }
//                //Bene Tleilax to draw Face Dancers
//                case 2 -> {
//                    shuffle(gameState.getDeck("traitor_deck"));
//                    drawCard(gameState, "traitor_deck", "BT");
//                    drawCard(gameState, "traitor_deck", "BT");
//                    drawCard(gameState, "traitor_deck", "BT");
//                    writeFactionInfo(event, gameState, discordGame, "BT");
//                    gameState.advancePhase();
//                    discordGame.sendMessage("turn-summary", "2b. Bene Tleilax have drawn their Face Dancers.");
//                }
//                //3. Spice, 4. Forces
//                case 3 -> {
//                    if (gameState.hasFaction("Fremen")) {
//                        discordGame.sendMessage("fremen-chat", "Please distribute 10 forces between Sietch Tabr, False Wall South, and False Wall West");
//                    }
//                    gameState.advancePhase();
//                    //If BG is not present, advance past the next step
//                    if (gameState.getJSONObject("game_state").getJSONObject("factions").isNull("BG")) {
//                        gameState.advancePhase();
//                    }
//                    discordGame.sendMessage("turn-summary", "3. Spice has been allocated.\n4. Forces are being placed on the board.");
//                }
//                case 4 -> {
//                    if (gameState.hasFaction("BG")) {
//                        discordGame.sendMessage("bg-chat", "Please choose where to place your advisor.");
//                    }
//
//                    gameState.advancePhase();
//                    //If Ix is not present, advance past the next step
//                    if (!gameState.hasFaction("Ix")) {
//                        gameState.advancePhase();
//                    }
//                }
//                //Ix to select from starting treachery cards
//                case 5 -> {
//                    if (gameState.hasFaction("Ix")) {
//                        int toDraw = gameState.getJSONObject("game_state").getJSONObject("factions").length();
//                        if (gameState.hasFaction("Harkonnen")) toDraw++;
//                        for (int i = 0; i < toDraw; i++) {
//                            drawCard(gameState, "treachery_deck", "Ix");
//                        }
//                        writeFactionInfo(event, gameState, discordGame, "Ix");
//                        discordGame.sendMessage("ix-chat", "Please select one treachery card to keep in your hand.");
//
//                        discordGame.sendMessage("turn-summary", "Ix is selecting their starting treachery card.");
//                    }
//                    gameState.advancePhase();
//                }
//                //5. Treachery
//                case 6 -> {
//                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
//                        if (!faction.equals("Ix")) drawCard(gameState, "treachery_deck", faction);
//                        if (faction.equals("Harkonnen")) drawCard(gameState, "treachery_deck", faction);
//                        writeFactionInfo(event, gameState, discordGame, faction);
//                    }
//                    gameState.advancePhase();
//                    discordGame.sendMessage("turn-summary", "5. Treachery cards are being dealt.");
//                }
//                //6. Turn Marker (prompt for dial for First Storm)
//                case 7 -> {
//                    JSONObject turnOrder = gameState.getResources().getJSONObject("turn_order");
//                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
//                        if (turnOrder.getString(String.valueOf(1)).equals(faction) || turnOrder.getString(String.valueOf(6)).equals(faction)) {
//                            discordGame.sendMessage(faction.toLowerCase() + "-chat", "Please submit your dial for initial storm position.");
//                        }
//                    }
//                    shuffle(gameState.getDeck("storm_deck"));
//                    gameState.advanceTurn();
//                    discordGame.sendMessage("turn-summary", "6. Turn Marker is set to turn 1.  The game is beginning!  Initial storm is being calculated...");
//                }
//            }
//            discordGame.pushGameState();
//        }
//        else {
//            switch (gameState.getPhase()) {
//                //1. Storm Phase
//                case 1 -> {
//                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Storm Phase:");
//                    JSONObject territories = gameState.getJSONObject("game_state").getJSONObject("game_board");
//                   if (gameState.getTurn() != 1) {
//                       int stormMovement = gameState.getDeck("storm_deck").getInt(0);
//                       shuffle(gameState.getDeck("storm_deck"));
//                       discordGame.sendMessage("turn-summary", "The storm moves " + stormMovement + " sectors this turn.");
//                       for (int i = 0; i < stormMovement; i++) {
//                           gameState.getResources().put("storm", (gameState.getResources().getInt("storm") + 1));
//                           if (gameState.getResources().getInt("storm") == 19) gameState.getResources().put("storm", 1);
//                           for (String territory : territories.keySet()) {
//                               if (!territories.getJSONObject(territory).getBoolean("is_rock") && territories.getJSONObject(territory).getInt("sector") == gameState.getResources().getInt("storm")) {
//                                   Set<String> forces = territories.getJSONObject(territory).getJSONObject("forces").keySet();
//                                   boolean fremenSpecialCase = false;
//                                   //Defaults to play "optimally", destorying Fremen regular forces over Fedaykin
//                                   if (forces.contains("Fremen") && forces.contains("Fremen*")) {
//                                       fremenSpecialCase = true;
//                                       int fremenForces = territories.getJSONObject(territory).getJSONObject("forces").getInt("Fremen");
//                                       int fremenFedaykin = territories.getJSONObject(territory).getJSONObject("forces").getInt("Fremen*");
//                                       int lost = (fremenForces + fremenFedaykin) / 2;
//                                       territories.getJSONObject(territory).getJSONObject("forces").remove("Fremen");
//                                       if (lost < fremenForces) {
//                                           territories.getJSONObject(territory).getJSONObject("forces").put("Fremen", fremenForces - lost);
//                                       } else if (lost > fremenForces) {
//                                           territories.getJSONObject(territory).getJSONObject("forces").remove("Fremen*");
//                                           territories.getJSONObject(territory).getJSONObject("forces").put("Fremen*", lost - fremenForces);
//                                       }
//                                       discordGame.sendMessage("turn-summary",gameState.getFaction("Fremen").getString("emoji") + " lost " + lost +
//                                                       " forces to the storm in " + territory);
//                                   }
//                                   for (String force : forces) {
//                                       if (force.contains("Fremen") && fremenSpecialCase) continue;
//                                       int lost = territories.getJSONObject(territory).getJSONObject("forces").getInt(force);
//                                       territories.getJSONObject(territory).getJSONObject("forces").remove(force);
//                                       if (force.contains("Fremen") && lost > 1) {
//                                           lost /= 2;
//                                           territories.getJSONObject(territory).getJSONObject("forces").put(force, lost);
//                                       }
//                                       if (gameState.getResources().getJSONObject("tanks_forces").isNull(force)) {
//                                           gameState.getResources().getJSONObject("tanks_forces").put(force, lost);
//                                       } else {
//                                           gameState.getResources().getJSONObject("tanks_forces").put(force,
//                                                   gameState.getResources().getJSONObject("tanks_forces").getInt(force) + lost);
//                                       }
//                                       discordGame.sendMessage("turn-summary",
//                                               gameState.getFaction(force.replace("*", "")).getString("emoji") + " lost " +
//                                                       lost + " forces to the storm in " + territory);
//                                   }
//
//                               }
//                               territories.getJSONObject(territory).remove("spice");
//                               territories.getJSONObject(territory).put("spice", 0);
//                           }
//                       }
//                   }
//                   if (gameState.hasFaction("Fremen")) {
//                       discordGame.sendMessage("fremen-chat", "The storm will move " + gameState.getDeck("storm_deck").getInt(0) + " sectors next turn.");
//
//                   }
//                   gameState.advancePhase();
//                }
//                //2. Spice Blow and Nexus
//                case 2 -> {
//                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Blow Phase:");
//                    discordGame.sendMessage("turn-summary", "A: " + drawCard(gameState, "spice_deck", "A"));
//                    discordGame.sendMessage("turn-summary", "B: " + drawCard(gameState, "spice_deck", "B"));
//                    gameState.advancePhase();
//                }
//                //3. Choam Charity
//                case 3 -> {
//                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " CHOAM Charity Phase:");
//                    int multiplier = 1;
//                    if (!gameState.getResources().isNull("inflation token")) {
//                        if (gameState.getResources().getString("inflation token").equals("cancel")) {
//                            discordGame.sendMessage("turn-summary","CHOAM Charity is cancelled!");
//                            gameState.advancePhase();
//                            break;
//                        } else {
//                            multiplier = 2;
//                        }
//                    }
//
//                    int choamGiven = 0;
//                    Set<String> factions = gameState.getJSONObject("game_state").getJSONObject("factions").keySet();
//                    if (factions.contains("CHOAM")) discordGame.sendMessage("turn-summary",
//                            gameState.getFaction("CHOAM").getString("emoji") + " receives " +
//                            gameState.getJSONObject("game_state").getJSONObject("factions").length() * 2 * multiplier + " <:spice4:991763531798167573> in dividends from their many investments."
//                    );
//                    for (String faction : factions) {
//                        if (faction.equals("CHOAM")) continue;
//                        int spice = gameState.getFaction(faction).getJSONObject("resources").getInt("spice");
//                        if (faction.equals("BG")) {
//                            gameState.getFaction(faction).getJSONObject("resources").remove("spice");
//                            choamGiven += 2 * multiplier;
//                            gameState.getFaction(faction).getJSONObject("resources").put("spice", spice + (2 * multiplier));
//                            discordGame.sendMessage("turn-summary", gameState.getFaction(faction).getString("emoji") + " have received " + 2 * multiplier + " <:spice4:991763531798167573> in CHOAM Charity.");
//                            spiceMessage(discordGame, 2 * multiplier, faction, "CHOAM Charity", true);
//                        }
//                        else if (spice < 2) {
//                            int charity = (2 * multiplier) - (spice * multiplier);
//                            choamGiven += charity;
//                            gameState.getFaction(faction).getJSONObject("resources").remove("spice");
//                            gameState.getFaction(faction).getJSONObject("resources").put("spice", spice + charity);
//                            discordGame.sendMessage("turn-summary",
//                                    gameState.getFaction(faction).getString("emoji") + " have received " + charity + " <:spice4:991763531798167573> in CHOAM Charity."
//                            );
//                            spiceMessage(discordGame, charity, faction, "CHOAM Charity", true);
//                        }
//                        else continue;
//                        writeFactionInfo(event, gameState, discordGame, faction);
//                    }
//                    if (!gameState.getJSONObject("game_state").getJSONObject("factions").isNull("CHOAM")) {
//                        int spice = gameState.getFaction("CHOAM").getJSONObject("resources").getInt("spice");
//                        gameState.getFaction("CHOAM").getJSONObject("resources").remove("spice");
//                        gameState.getFaction("CHOAM").getJSONObject("resources").put("spice", (10 * multiplier) + spice - choamGiven);
//                        spiceMessage(discordGame, gameState.getJSONObject("game_state").getJSONObject("factions").length() * 2 * multiplier, "choam", "CHOAM Charity", true);
//                        discordGame.sendMessage("turn-summary",
//                                gameState.getFaction("CHOAM").getString("emoji") + " has paid " + choamGiven + " <:spice4:991763531798167573> to factions in need."
//                        );
//                        spiceMessage(discordGame, choamGiven, "choam", "CHOAM Charity given", false);
//                    }
//                    gameState.advancePhase();
//                }
//                //4. Bidding
//                case 4 -> {
//                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Bidding Phase:");
//                    int cardsUpForBid = 0;
//                    Set<String> factions = gameState.getJSONObject("game_state").getJSONObject("factions").keySet();
//                    StringBuilder countMessage = new StringBuilder();
//                    countMessage.append("<:treachery:991763073281040518>Number of Treachery Cards<:treachery:991763073281040518>\n");
//                    for (String faction : factions) {
//                        int length = gameState.getFaction(faction).getJSONObject("resources").getJSONArray("treachery_hand").length();
//                        countMessage.append(gameState.getFaction(faction).getString("emoji")).append(": ").append(length).append("\n");
//                        if (faction.equals("Harkonnen") && length < 8 || faction.equals("CHOAM") && length < 5 ||
//                                !(faction.equals("Harkonnen") || faction.equals("CHOAM")) && length < 4) cardsUpForBid++;
//                        if (faction.equals("Ix")) cardsUpForBid++;
//                        if (faction.equals("Rich")) cardsUpForBid--;
//                    }
//                    JSONArray deck = gameState.getDeck("treachery_deck");
//                    if (factions.contains("Ix")) {
//                        discordGame.sendMessage("ix-chat", "Please select a card to put back to top or bottom.");
//                    }
//                    countMessage.append("There will be ").append(cardsUpForBid).append(" <:treachery:991763073281040518> cards up for bid this round.");
//                    discordGame.sendMessage("turn-summary", countMessage.toString());
//                    gameState.getResources().put("market_size", cardsUpForBid);
//                    for (int i = 0; i < cardsUpForBid; i++) {
//                        gameState.getResources().getJSONArray("market").put(deck.getString(deck.length() - 1));
//                        deck.remove(deck.length() - 1);
//                        if (factions.contains("Ix")) {
//                            discordGame.sendMessage("ix-chat", "<:treachery:991763073281040518> " +
//                                    deck.getString(deck.length() - i - 1) + " <:treachery:991763073281040518>");
//                        }
//                    }
//                    if (factions.contains("Atreides")) {
//                        discordGame.sendMessage("atreides-chat","The first card up for bid is <:treachery:991763073281040518> " + gameState.getResources().getJSONArray("market").getString(0).split("\\|")[0] + " <:treachery:991763073281040518>");
//                    }
//                    StringBuilder message = new StringBuilder();
//                    message.append("R").append(gameState.getResources().getInt("turn")).append(":C1\n");
//                    int firstBid = Math.ceilDiv(gameState.getResources().getInt("storm"), 3) + 1;
//                    for (int i = 0; i < factions.size(); i++) {
//                        int playerPosition = firstBid + i > 6 ? firstBid + i - 6 : firstBid + i;
//                        String faction = gameState.getResources().getJSONObject("turn_order").getString(String.valueOf(playerPosition));
//                        int length = gameState.getFaction(faction).getJSONObject("resources").getJSONArray("treachery_hand").length();
//                        if (faction.equals("Harkonnen") && length < 8 || faction.equals("CHOAM") && length < 5 ||
//                                !(faction.equals("Harkonnen") || faction.equals("CHOAM")) && length < 4) message.append(gameState.getFaction(faction).getString("emoji")).append(":");
//                                if (i == 0) message.append(" ").append(gameState.getFaction(faction).getString("player"));
//                                message.append("\n");
//                    }
//                    discordGame.sendMessage("bidding-phase", message.toString());
//                    gameState.advancePhase();
//                }
//                //5. Revival
//                case 5 -> {
//                    if (gameState.getResources().getJSONArray("market").length() > 0) {
//                        discordGame
//                                .sendMessage("turn-summary", "There were " + gameState.getResources().getJSONArray("market").length() + " cards not bid on this round that are placed back on top of the <:treachery:991763073281040518> deck.");
//                        int marketLength = gameState.getResources().getJSONArray("market").length();
//                        for (int i = 0; i < marketLength; i++) {
//                            gameState.getDeck("treachery_deck").put(gameState.getResources().getJSONArray("market").getString(gameState.getResources().getJSONArray("market").length() - 1));
//                            gameState.getResources().getJSONArray("market").remove(gameState.getResources().getJSONArray("market").length() - 1);
//                        }
//                    }
//                    gameState.getResources().remove("market_size");
//                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Revival Phase:");
//                    Set<String> factions = gameState.getJSONObject("game_state").getJSONObject("factions").keySet();
//                    StringBuilder message = new StringBuilder();
//                    message.append("Free Revivals:\n");
//                    for (String faction : factions) {
//                        int free = gameState.getFaction(faction).getInt("free_revival");
//                        int revived = 0;
//                        boolean revivedStar = false;
//                        for (int i = free; i > 0; i--) {
//                            if (gameState.getResources().getJSONObject("tanks_forces").getInt(faction) == 0
//                                    && (gameState.getResources().getJSONObject("tanks_forces").isNull(faction + "*") || gameState.getResources().getJSONObject("tanks_forces").getInt(faction + "*") == 0)) continue;
//                            revived++;
//                            if (!gameState.getResources().getJSONObject("tanks_forces").isNull(faction + "*") && gameState.getResources().getJSONObject("tanks_forces").getInt(faction + "*") != 0 && !revivedStar) {
//                                int starred = gameState.getResources().getJSONObject("tanks_forces").getInt(faction + "*");
//                                gameState.getResources().getJSONObject("tanks_forces").remove(faction + "*");
//                                if (starred > 1) gameState.getResources().getJSONObject("tanks_forces").put(faction + "*", starred - 1);
//                                revivedStar = true;
//                                int reserves = gameState.getFaction(faction).getJSONObject("resources").getInt("reserves*");
//                                gameState.getFaction(faction).getJSONObject("resources").remove("reserves*");
//                                gameState.getFaction(faction).getJSONObject("resources").put("reserves*", reserves + 1);
//                            } else if (gameState.getResources().getJSONObject("tanks_forces").getInt(faction) != 0) {
//                                int forces = gameState.getResources().getJSONObject("tanks_forces").getInt(faction);
//                                gameState.getResources().getJSONObject("tanks_forces").remove(faction);
//                                gameState.getResources().getJSONObject("tanks_forces").put(faction, forces - 1);
//                                int reserves = gameState.getFaction(faction).getJSONObject("resources").getInt("reserves");
//                                gameState.getFaction(faction).getJSONObject("resources").remove("reserves");
//                                gameState.getFaction(faction).getJSONObject("resources").put("reserves", reserves + 1);
//                            }
//                        }
//                        if (revived > 0) {
//                            message.append(gameState.getFaction(faction).getString("emoji")).append(": ").append(revived).append("\n");
//                        }
//                    }
//                    discordGame.sendMessage("turn-summary", message.toString());
//                    gameState.advancePhase();
//                }
//                //6. Shipment and Movement
//                case 6 -> {
//                    discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Shipment and Movement Phase:");
//                    if (gameState.hasFaction("Atreides")) {
//                        discordGame.sendMessage("atreides-info", "You see visions of " + gameState.getDeck("spice_deck").getString(gameState.getDeck("spice_deck").length() - 1) + " in your future.");
//                    }
//                    if(!gameState.getJSONObject("game_state").getJSONObject("factions").isNull("BG")) {
//                       for (String territoryName : gameState.getGameBoard().keySet()) {
//                           JSONObject territory = gameState.getTerritory(territoryName);
//                           if (!territory.getJSONObject("forces").keySet().contains("Advisor")) continue;
//                           discordGame.sendMessage("turn-summary",gameState.getFaction("BG").getString("emoji") + " to decide whether to flip their advisors in " + territory.getString("territory_name"));
//                       }
//                    }
//                    gameState.advancePhase();
//                }
//                //TODO: 7. Battle
//                case 7 -> {
//                    discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Battle Phase:");
//                    gameState.advancePhase();
//
//                }
//                //TODO: 8. Spice Harvest
//                case 8 -> {
//                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Harvest Phase:");
//                   JSONObject territories = gameState.getJSONObject("game_state").getJSONObject("game_board");
//                   //This is hacky, but I add spice to Arrakeen, Carthag, and Tuek's, then if it is not collected by the following algorithm, it is removed.
//                    territories.getJSONObject("Arrakeen").remove("spice");
//                    territories.getJSONObject("Carthag").remove("spice");
//                    territories.getJSONObject("Tuek's Sietch").remove("spice");
//                    territories.getJSONObject("Arrakeen").put("spice", 2);
//                    territories.getJSONObject("Carthag").put("spice", 2);
//                    territories.getJSONObject("Tuek's Sietch").put("spice", 1);
//                    for (String territoryName : territories.keySet()) {
//                        JSONObject territory = territories.getJSONObject(territoryName);
//                        if (territory.getInt("spice") == 0 || territory.getJSONObject("forces").length() == 0) continue;
//                        int spice = territory.getInt("spice");
//                        territory.remove("spice");
//                        Set<String> factions = territory.getJSONObject("forces").keySet();
//                        for (String faction : factions) {
//                            int forces = territory.getJSONObject("forces").getInt(faction);
//                            forces += territory.getJSONObject("forces").isNull(faction + "*") ? 0 : territory.getJSONObject("forces").getInt(faction + "*");
//                            int toCollect = 0;
//                            if (faction.equals("BG") && factions.size() > 1) continue;
//                            //If the faction has mining equipment, collect 3 spice per force.
//                            if ((!territories.getJSONObject("Arrakeen").getJSONObject("forces").isNull(faction) || !territories.getJSONObject("Carthag").getJSONObject("forces").isNull(faction) && !faction.equals("BG")) ||
//                                    (faction.equals("BG") && (territories.getJSONObject("Arrakeen").getJSONObject("forces").length() < 2 && !territories.getJSONObject("Arrakeen").getJSONObject("forces").isNull("BG")) ||
//                                            (territories.getJSONObject("Carthag").getJSONObject("forces").length() < 2 && !territories.getJSONObject("Carthag").getJSONObject("forces").isNull("BG")))) {
//                                toCollect += forces * 3;
//                            } else toCollect += forces * 2;
//                            if (spice < toCollect) {
//                                toCollect = spice;
//                                spice = 0;
//                            } else spice -= toCollect;
//                            territory.put("spice", spice);
//                            int factionSpice = gameState.getFaction(faction).getJSONObject("resources").getInt("spice");
//                            gameState.getFaction(faction).getJSONObject("resources").remove("spice");
//                            gameState.getFaction(faction).getJSONObject("resources").put("spice", factionSpice + toCollect);
//                            discordGame.sendMessage("turn-summary", gameState.getFaction(faction).getString("emoji") + " collects " + toCollect + " <:spice4:991763531798167573> from " + territoryName);
//                        }
//
//                    }
//                    territories.getJSONObject("Arrakeen").remove("spice");
//                    territories.getJSONObject("Carthag").remove("spice");
//                    territories.getJSONObject("Tuek's Sietch").remove("spice");
//                    territories.getJSONObject("Arrakeen").put("spice", 0);
//                    territories.getJSONObject("Carthag").put("spice", 0);
//                    territories.getJSONObject("Tuek's Sietch").put("spice", 0);
//                    gameState.advancePhase();
//                }
//                //TODO: 9. Mentat Pause
//                case 9 -> {
//                    discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Mentat Pause Phase:");
//                    for (String faction : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
//                        if (!gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").isNull("spice")) {
//                            discordGame.sendMessage("turn-summary", gameState.getFaction(faction).getString("emoji") + " collects " +
//                                    gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").getInt("spice") + " <:spice4:991763531798167573> from front of shield.");
//                            spiceMessage(discordGame,  gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").getInt("spice"), faction, "front of shield", true);
//                            gameState.getFaction(faction).getJSONObject("resources").put("spice", gameState.getFaction(faction).getJSONObject("resources").getInt("spice") +
//                                    gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").getInt("spice"));
//                            gameState.getFaction(faction).getJSONObject("resources").getJSONObject("front_of_shield").remove("spice");
//                        }
//                        writeFactionInfo(event, gameState, discordGame, faction);
//                    }
//                    gameState.advanceTurn();
//                }
//            }
//            discordGame.pushGameState();
//            drawGameBoard(discordGame, gameState);
//        }
//    }
//
//    public void selectTraitor(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONObject faction = gameState.getFaction(event.getOption("factionname").getAsString());
//        for (int i = 0; i < 4; i++) {
//            if (!faction.getJSONObject("resources").getJSONArray("traitors").getString(i).toLowerCase().contains(event.getOption("traitor").getAsString().toLowerCase())) {
//                gameState.getDeck("traitor_deck").put(faction.getJSONObject("resources").getJSONArray("traitors").get(i));
//                String traitor = faction.getJSONObject("resources").getJSONArray("traitors").getString(i);
//                faction.getJSONObject("resources").getJSONArray("traitors").put(i, "~~" + traitor + "~~");
//            }
//        }
//        writeFactionInfo(event, gameState, discordGame, event.getOption("factionname").getAsString());
//        discordGame.pushGameState();
//    }
//
//    public String getTerritoryString(SlashCommandInteractionEvent event, Game gameState) {
//        String sector = event.getOption("sector") == null ? "" : "(" + event.getOption("sector").getAsString() + ")";
//        String territory = "";
//        if (event.getOption("mostlikelyterritories") == null && event.getOption("otherterritories") == null) {
//            event.getChannel().sendMessage("You have to select a territory.").queue();
//            return null;
//        } else if (event.getOption("mostlikelyterritories") == null) {
//            territory = event.getOption("otherterritories").getAsString();
//        } else {
//            territory = event.getOption("mostlikelyterritories").getAsString();
//        }
//        if (gameState.getJSONObject("game_state").getJSONObject("game_board").isNull(territory + sector)) {
//            event.getChannel().sendMessage("Territory does not exist in that sector. Check your sector number and try again.").queue();
//            return null;
//        }
//        return territory + sector;
//    }
//
//    public void placeForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        String star = "";
//        String territory = getTerritoryString(event, gameState);
//
//        if (event.getOption("starred") != null && event.getOption("starred").getAsBoolean()) {
//            star = "*";
//        }
//        int reserves = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("reserves" + star);
//        if (reserves < event.getOption("amount").getAsInt()) {
//            discordGame.sendMessage("mod-info", "This faction does not have enough forces in reserves!");
//            return;
//        }
//        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("reserves" + star);
//        gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("reserves" + star, reserves - event.getOption("amount").getAsInt());
//        int previous = 0;
//
//        if (!gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(territory)
//                .getJSONObject("forces").isNull(event.getOption("factionname").getAsString() + star)) {
//            previous = gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(territory).getJSONObject("forces").getInt(event.getOption("factionname").getAsString() + star);
//        }
//
//        if (event.getOption("isshipment").getAsBoolean()) {
//            int cost = gameState.getTerritory(territory).getBoolean("is_stronghold") ? 1 : 2;
//            cost *= event.getOption("factionname").getAsString().equals("Guild") ? Math.ceilDiv(event.getOption("amount").getAsInt(), 2) : event.getOption("amount").getAsInt();
//            int spice = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("spice");
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("spice");
//            if (spice < cost) {
//                discordGame.sendMessage("mod-info","This faction doesn't have the resources to make this shipment!");
//                return;
//            }
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("spice", spice - cost);
//            spiceMessage(discordGame, cost, event.getOption("factionname").getAsString(), "shipment to " + territory, false);
//            if (gameState.getFaction("Guild") != null && !(event.getOption("factionname").getAsString().equals("Guild") || event.getOption("factionname").getAsString().equals("Fremen"))) {
//                spice = gameState.getFaction("Guild").getJSONObject("resources").getInt("spice");
//                gameState.getFaction("Guild").getJSONObject("resources").remove("spice");
//                gameState.getFaction("Guild").getJSONObject("resources").put("spice", spice + event.getOption("amount").getAsInt());
//                spiceMessage(discordGame, cost, "guild", gameState.getFaction(event.getOption("factionname").getAsString()).getString("emoji") + " shipment", true);
//                writeFactionInfo(event, gameState, discordGame, "Guild");
//            }
//            writeFactionInfo(event, gameState, discordGame, event.getOption("factionname").getAsString());
//        }
//        if (gameState.getTerritory(territory).getJSONObject("forces").keySet().contains("BG")) {
//            discordGame.sendMessage("turn-summary", gameState.getFaction("BG").getString("emoji") + " to decide whether to flip their forces in " + territory);
//        }
//        gameState.getJSONObject("game_state").getJSONObject("game_board").getJSONObject(territory).getJSONObject("forces").put(event.getOption("factionname").getAsString() + star, event.getOption("amount").getAsInt() + previous);
//        discordGame.pushGameState();
//        drawGameBoard(discordGame, gameState);
//    }
//
//    public void removeForces(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        String sector = event.getOption("sector") == null ? "" : "(" + event.getOption("sector").getAsString() + ")";
//        String territoryName = "";
//
//        if (event.getOption("mostlikelyterritories") == null && event.getOption("otherterritories") == null) {
//            discordGame.sendMessage("mod-info", "You have to select a territory.");
//            return;
//        } else if (event.getOption("mostlikelyterritories") == null) {
//            territoryName = event.getOption("otherterritories").getAsString();
//        } else {
//            territoryName = event.getOption("mostlikelyterritories").getAsString();
//        }
//        if (gameState.getJSONObject("game_state").getJSONObject("game_board").isNull(territoryName + sector)) {
//            discordGame.sendMessage("mod-info","Territory does not exist in that sector. Check your sector number and try again.");
//            return;
//        }
//        JSONObject territory = gameState.getTerritory(territoryName + sector);
//        String starred = event.getOption("starred").getAsBoolean() ? "*" : "";
//        int forces = territory.getJSONObject("forces").getInt(event.getOption("factionname").getAsString() + starred);
//        territory.getJSONObject("forces").remove(event.getOption("factionname").getAsString() + starred);
//        if (forces > event.getOption("amount").getAsInt()) {
//            territory.getJSONObject("forces").put(event.getOption("factionname").getAsString() + starred, forces - event.getOption("amount").getAsInt());
//        } else if (forces < event.getOption("amount").getAsInt()) {
//            discordGame.sendMessage("mod-info","You are trying to remove more forces than this faction has in this territory! Please check your info and try again.");
//            return;
//        }
//
//        if (event.getOption("totanks").getAsBoolean()) {
//            int tanks = gameState.getResources().getJSONObject("tanks_forces").getInt(event.getOption("factionname").getAsString() + starred);
//            gameState.getResources().getJSONObject("tanks_forces").remove(event.getOption("factionname").getAsString() + starred);
//            gameState.getResources().getJSONObject("tanks_forces").put(event.getOption("factionname").getAsString() + starred, tanks + event.getOption("amount").getAsInt());
//        } else {
//            int reserves = gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").getInt("reserves" + starred);
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").remove("reserves" + starred);
//            gameState.getFaction(event.getOption("factionname").getAsString()).getJSONObject("resources").put("reserves" + starred, reserves + event.getOption("amount").getAsInt());
//        }
//        discordGame.pushGameState();
//        if (event.getOption("totanks").getAsBoolean()) drawGameBoard(discordGame, gameState);
//    }
//
//    public void setStorm(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        gameState.getResources().remove("storm");
//        try {
//            gameState.getResources().put("storm", event.getOption("sector").getAsInt());
//        } catch (NullPointerException e) {
//            discordGame.sendMessage("mod-info", "No storm sector was selected.");
//            return;
//        }
//        discordGame.sendMessage("turn-summary","The storm has been initialized to sector " + event.getOption("sector").getAsInt());
//        discordGame.pushGameState();
//        drawGameBoard(discordGame, gameState);
//    }
//
    public void writeFactionInfo(SlashCommandInteractionEvent event, Game gameState, DiscordGame discordGame, String factionName) throws ChannelNotFoundException {
        if (gameState.getFaction(factionName) == null) return;
        Faction faction = gameState.getFaction(factionName);

        String emoji = faction.getEmoji();
        Deck traitors = faction.getDeck("traitors");
        StringBuilder traitorString = new StringBuilder();
        if (faction.equals("BT")) traitorString.append("\n__Face Dancers:__\n");
        else traitorString.append("\n__Traitors:__\n");
        for (Card traitor : traitors.getAllCards()) {
            traitorString.append(traitor.getName());
            traitorString.append("\n");
        }
        for (TextChannel channel : discordGame.getTextChannels()) {
            if (channel.getName().equals(faction.getName().toLowerCase() + "-info")) {
                discordGame.sendMessage(channel.getName(), emoji + "**Faction Info**" + emoji + "\n__Spice:__ " +
                        faction.getResource("spice").getValue() +
                        traitorString);

                for (Card treachery : faction.getDeck("treachery").getAllCards()) {
                    discordGame.sendMessage(channel.getName(), "<:treachery:991763073281040518> " + treachery.getName().split("\\|")[0].strip() + " <:treachery:991763073281040518>");
                }
            }
        }

    }
//
//    public void bgFlip(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONObject territory = gameState.getTerritory(getTerritoryString(event, gameState));
//        if (territory.getJSONObject("forces").keySet().contains("BG")) {
//            int bg = territory.getJSONObject("forces").getInt("BG");
//            territory.getJSONObject("forces").remove("BG");
//            territory.getJSONObject("forces").put("Advisor", bg);
//        } else if (territory.getJSONObject("forces").keySet().contains("Advisor")) {
//            int advisor = territory.getJSONObject("forces").getInt("Advisor");
//            territory.getJSONObject("forces").remove("Advisor");
//            territory.getJSONObject("forces").put("BG", advisor);
//        } else {
//            discordGame.sendMessage("mod-info","No Bene Gesserit were found in that territory.");
//            return;
//        }
//        discordGame.pushGameState();
//        drawGameBoard(discordGame, gameState);
//    }
//
//    public void drawGameBoard(DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        if (gameState.getBoolean("mute")) return;
//        //Load png resources into a hashmap.
//        HashMap<String, File> boardComponents = new HashMap<>();
//        URL dir = getClass().getClassLoader().getResource("Board Components");
//        try {
//            for (File file : new File(dir.toURI()).listFiles()) {
//                boardComponents.put(file.getName().replace(".png", ""), file);
//            }
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//
//
//        try {
//            BufferedImage board = ImageIO.read(boardComponents.get("Board"));
//
//            //Place sigils
//            for (int i = 1; i <= gameState.getResources().getJSONObject("turn_order").length(); i++) {
//                BufferedImage sigil = ImageIO.read(boardComponents.get(gameState.getResources().getJSONObject("turn_order").getString(String.valueOf(i)) + " Sigil"));
//                Point coordinates = Initializers.getDrawCoordinates("sigil " + i);
//                sigil = resize(sigil, 50, 50);
//                board = overlay(board, sigil, coordinates, 1);
//            }
//
//            //Place turn, phase, and storm markers
//            BufferedImage turnMarker = ImageIO.read(boardComponents.get("Turn Marker"));
//            turnMarker = resize(turnMarker, 55, 55);
//            int turn = gameState.getResources().getInt("turn") == 0 ? 1 : gameState.getResources().getInt("turn");
//            float angle = (turn * 36) + 74f;
//            turnMarker = rotateImageByDegrees(turnMarker, angle);
//            Point coordinates = Initializers.getDrawCoordinates("turn " + gameState.getResources().getInt("turn"));
//            board = overlay(board, turnMarker, coordinates, 1);
//            BufferedImage phaseMarker = ImageIO.read(boardComponents.get("Phase Marker"));
//            phaseMarker = resize(phaseMarker, 50, 50);
//            coordinates = Initializers.getDrawCoordinates("phase " + (gameState.getResources().getInt("phase") - 1));
//            board = overlay(board, phaseMarker, coordinates, 1);
//            BufferedImage stormMarker = ImageIO.read(boardComponents.get("storm"));
//            stormMarker = resize(stormMarker, 172, 96);
//            stormMarker = rotateImageByDegrees(stormMarker, -(gameState.getResources().getInt("storm") * 20));
//            board = overlay(board, stormMarker, Initializers.getDrawCoordinates("storm " + gameState.getResources().getInt("storm")), 1);
//
//
//            //Place forces
//            for (String territoryName : gameState.getGameBoard().keySet()) {
//                JSONObject territory = gameState.getTerritory(territoryName);
//                if (territory.getJSONObject("forces").length() == 0 && territory.getInt("spice") == 0) continue;
//                int offset = 0;
//                int i = 0;
//
//                if (territory.getInt("spice") != 0) {
//                    i = 1;
//                    int spice = territory.getInt("spice");
//                    while (spice != 0) {
//                        if (spice >= 10) {
//                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("10 Spice"));
//                            spiceImage = resize(spiceImage, 25,25);
//                            Point spicePlacement = Initializers.getPoints(territoryName).get(0);
//                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
//                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
//                            spice -= 10;
//                        } else if (spice >= 5) {
//                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("5 Spice"));
//                            spiceImage = resize(spiceImage, 25,25);
//                            Point spicePlacement = Initializers.getPoints(territoryName).get(0);
//                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
//                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
//                            spice -= 5;
//                        } else if (spice >= 2) {
//                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("2 Spice"));
//                            spiceImage = resize(spiceImage, 25,25);
//                            Point spicePlacement = Initializers.getPoints(territoryName).get(0);
//                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
//                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
//                            spice -= 2;
//                        } else {
//                            BufferedImage spiceImage = ImageIO.read(boardComponents.get("1 Spice"));
//                            spiceImage = resize(spiceImage, 25,25);
//                            Point spicePlacement = Initializers.getPoints(territoryName).get(0);
//                            Point spicePlacementOffset = new Point(spicePlacement.x + offset, spicePlacement.y - offset);
//                            board = overlay(board, spiceImage, spicePlacementOffset, 1);
//                            spice -= 1;
//                        }
//                        offset += 15;
//                    }
//                }
//                offset = 0;
//                for (String force : territory.getJSONObject("forces").keySet()) {
//                    int strength = territory.getJSONObject("forces").getInt(force);
//                    BufferedImage forceImage = buildForceImage(boardComponents, force, strength);
//                    Point forcePlacement = Initializers.getPoints(territoryName).get(i);
//                    Point forcePlacementOffset = new Point(forcePlacement.x, forcePlacement.y + offset);
//                    board = overlay(board, forceImage, forcePlacementOffset, 1);
//                    i++;
//                    if (i == Initializers.getPoints(territoryName).size()) {
//                        offset += 20;
//                        i = 0;
//                    }
//                }
//            }
//
//            //Place tanks forces
//            int i = 0;
//            int offset = 0;
//            for (String force : gameState.getResources().getJSONObject("tanks_forces").keySet()) {
//                JSONObject tanks = gameState.getResources().getJSONObject("tanks_forces");
//                if (tanks.getInt(force) == 0) continue;
//                int strength = tanks.getInt(force);
//                BufferedImage forceImage = buildForceImage(boardComponents, force, strength);
//
//                Point tanksCoordinates = Initializers.getPoints("Forces Tanks").get(i);
//                Point tanksOffset = new Point(tanksCoordinates.x, tanksCoordinates.y - offset);
//
//                board = overlay(board, forceImage, tanksOffset, 1);
//                i++;
//                if (i > 1) {
//                    offset += 30;
//                    i = 0;
//                }
//            }
//
//            //Place tanks leaders
//            i = 0;
//            offset = 0;
//            for (Object leader : gameState.getResources().getJSONArray("tanks_leaders")) {
//                String leaderString = (String) leader;
//                BufferedImage leaderImage = ImageIO.read(boardComponents.get(leaderString.split("-")[0].strip()));
//                leaderImage = resize(leaderImage, 70,70);
//                Point tanksCoordinates = Initializers.getPoints("Leaders Tanks").get(i);
//                Point tanksOffset = new Point(tanksCoordinates.x, tanksCoordinates.y - offset);
//                board = overlay(board, leaderImage, tanksOffset, 1);
//                i++;
//                if (i > Initializers.getPoints("Leaders Tanks").size() - 1) {
//                    offset += 70;
//                    i = 0;
//                }
//            }
//
//            ByteArrayOutputStream boardOutputStream = new ByteArrayOutputStream();
//            ImageIO.write(board, "png", boardOutputStream);
//
//            FileUpload boardFileUpload = FileUpload.fromData(boardOutputStream.toByteArray(), "board.png");
//            discordGame.getTextChannel("turn-summary")
//                    .sendFiles(boardFileUpload)
//                    .queue();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public BufferedImage buildForceImage(HashMap<String, File> boardComponents, String force, int strength) throws IOException {
//        BufferedImage forceImage = !force.equals("Advisor") ? ImageIO.read(boardComponents.get(force.replace("*", "") + " Troop")) : ImageIO.read(boardComponents.get("BG Advisor"));
//        forceImage = resize(forceImage, 47, 29);
//        if (force.contains("*")) {
//            BufferedImage star = ImageIO.read(boardComponents.get("star"));
//            star = resize(star, 8, 8);
//            forceImage = overlay(forceImage, star, new Point(20, 7), 1);
//        }
//        if (strength > 9) {
//            BufferedImage oneImage = ImageIO.read(boardComponents.get("1"));
//            BufferedImage digitImage = ImageIO.read(boardComponents.get(String.valueOf(strength - 10)));
//            oneImage = resize(oneImage, 12, 12);
//            digitImage = resize(digitImage, 12,12);
//            forceImage = overlay(forceImage, oneImage, new Point(28, 14), 1);
//            forceImage = overlay(forceImage, digitImage, new Point(36, 14), 1);
//        } else {
//            BufferedImage numberImage = ImageIO.read(boardComponents.get(String.valueOf(strength)));
//            numberImage = resize(numberImage, 12, 12);
//            forceImage = overlay(forceImage, numberImage, new Point(30,14), 1);
//
//        }
//        return forceImage;
//    }
//
//    public BufferedImage overlay(BufferedImage board, BufferedImage piece, Point coordinates, float alpha) throws IOException {
//
//        int compositeRule = AlphaComposite.SRC_OVER;
//        AlphaComposite ac;
//        ac = AlphaComposite.getInstance(compositeRule, alpha);
//        BufferedImage overlay = new BufferedImage(board.getWidth(), board.getHeight(), BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g = overlay.createGraphics();
//        g.drawImage(board, 0, 0, null);
//        g.setComposite(ac);
//        g.drawImage(piece, coordinates.x - (piece.getWidth()/2), coordinates.y - (piece.getHeight()/2), null);
//        g.setComposite(ac);
//        g.dispose();
//
//        return overlay;
//    }
//
//    public BufferedImage rotateImageByDegrees(BufferedImage img, double angle) {
//        double rads = Math.toRadians(angle);
//        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
//        int w = img.getWidth();
//        int h = img.getHeight();
//        int newWidth = (int) Math.floor(w * cos + h * sin);
//        int newHeight = (int) Math.floor(h * cos + w * sin);
//
//        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g2d = rotated.createGraphics();
//        AffineTransform at = new AffineTransform();
//        at.translate((newWidth - w) / 2, (newHeight - h) / 2);
//
//        int x = w / 2;
//        int y = h / 2;
//
//        at.rotate(rads, x, y);
//        g2d.setTransform(at);
//        g2d.drawImage(img, 0, 0, null);
//        g2d.setColor(Color.RED);
//        g2d.drawRect(0, 0, newWidth - 1, newHeight - 1);
//        g2d.dispose();
//
//        return rotated;
//    }
//
//    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
//        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
//        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
//
//        Graphics2D g2d = dimg.createGraphics();
//        g2d.drawImage(tmp, 0, 0, null);
//        g2d.dispose();
//
//        return dimg;
//    }
//
//    public void bribe(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        JSONObject faction = gameState.getFaction(event.getOption("factionname").getAsString());
//        JSONObject recipient = gameState.getFaction(event.getOption("recipient").getAsString());
//        int amount = event.getOption("amount").getAsInt();
//
//        if (faction.getJSONObject("resources").getInt("spice") < amount) {
//            discordGame.getTextChannel("mod-info").sendMessage("Faction does not have enough spice to pay the bribe!").queue();
//        }
//        faction.getJSONObject("resources").put("spice", faction.getJSONObject("resources").getInt("spice") - amount);
//        spiceMessage(discordGame, amount, faction.getString("name"), "bribe to " + recipient.getString("emoji"), false);
//        if (recipient.getJSONObject("resources").getJSONObject("front_of_shield").isNull("spice")) recipient.getJSONObject("resources").getJSONObject("front_of_shield").put("spice", amount);
//        else recipient.getJSONObject("resources").getJSONObject("front_of_shield").put("spice", recipient.getJSONObject("resources").getJSONObject("front_of_shield").getInt("spice") + amount);
//        discordGame.pushGameState();
//    }
//
//
//    public void mute(DiscordGame discordGame, Game gameState) {
//        gameState.put("mute", !gameState.getBoolean("mute"));
//        discordGame.pushGameState();
//    }
//
//    public void displayGameState(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
//        TextChannel channel = discordGame.getTextChannel("mod-info");
//        switch (event.getOption("data").getAsString()) {
//            case "territories" -> {
//               JSONObject territories = gameState.getJSONObject("game_state").getJSONObject("game_board");
//               for (String territoryName : territories.keySet()) {
//                   JSONObject territory = territories.getJSONObject(territoryName);
//                   if (territory.getInt("spice") == 0 && !territory.getBoolean("is_stronghold") && territory.getJSONObject("forces").length() == 0) continue;
//                   discordGame.sendMessage(channel.getName(), "**" + territory.getString("territory_name") + "(" + territory.getInt("sector") + "):** \n" +
//                           "Spice: " + territory.getInt("spice") + "\nForces: " + territory.getJSONObject("forces").toString(4));
//               }
//            }
//            case "dnd" -> {
//                for (String key : gameState.getResources().keySet()) {
//                    if (key.contains("deck") || key.contains("discard")) {
//                        discordGame.sendMessage(channel.getName(), "**" + key + ":** " + gameState.getResources().getJSONArray(key).toString(4));
//                    }
//                }
//
//            }
//            case "etc" -> {
//                for (String key : gameState.getResources().keySet()) {
//                    if (!key.contains("deck") && !key.contains("discard")) {
//                        discordGame.sendMessage(channel.getName(), "**" + key + ":** " + gameState.getResources().get(key).toString());
//                    }
//                }
//            }
//            case "factions" -> {
//                for (String factionName : gameState.getJSONObject("game_state").getJSONObject("factions").keySet()) {
//                    JSONObject faction = gameState.getJSONObject("game_state").getJSONObject("factions").getJSONObject(factionName);
//                    StringBuilder message = new StringBuilder();
//                    message.append("**" + faction.getString("name")).append(":**\nPlayer: ").append(faction.getString("username")).append("\n");
//                    for (String resourceName : faction.getJSONObject("resources").keySet()) {
//                        Object resource = faction.getJSONObject("resources").get(resourceName);
//                        message.append(resourceName).append(": ").append(resource.toString()).append("\n");
//                    }
//                    discordGame.sendMessage(channel.getName(), message.toString());
//                }
//            }
//        }
//        drawGameBoard(discordGame, gameState);
//    }
//
//    public void clean(SlashCommandInteractionEvent event) {
//        if (!event.getOption("password").getAsString().equals(Dotenv.configure().load().get("PASSWORD"))) {
//            event.getChannel().sendMessage("You have attempted the forbidden command.\n\n...Or you're Voiceofonecrying " +
//                    "and you fat-fingered the password").queue();
//            return;
//        }
//        List<Category> categories = event.getGuild().getCategories();
//        for (Category category : categories) {
//            //if (!category.getName().startsWith("test")) continue;
//            category.delete().complete();
//        }
//        List<TextChannel> channels = event.getGuild().getTextChannels();
//        for (TextChannel channel : channels) {
//            if (//!channel.getName().startsWith("test") ||
//            channel.getName().equals("general")) continue;
//            channel.delete().complete();
//        }
//    }
}
