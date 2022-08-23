package Controller;

import Model.Game;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class Commands {

    public static void newGame(MessageReceivedEvent event) {
        if (event.getMember() == null) {
            event.getChannel().sendMessage("You are not a Game Master").queue();
            return;
        }
        List<Role> roles = event.getMember().getRoles();
        for (Role role : roles) {
            if (role.getName().equals("Game Master")) {
                Commands.newGame(event);
                event.getChannel().sendMessage("You are not a Game Master").queue();
                return;
            }
        }
        Game newGame = new Game();
        newGame.setName(event.getMessage().getContentRaw().replace("$new game$", "").strip());
        newGame.setTurn(1);
        newGame.setShieldWallBroken(false);

        event.getGuild().createCategory(newGame.getName()).queue();
        try {
            buildChannels(event, newGame);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void buildChannels(MessageReceivedEvent event, Game newGame) throws InterruptedException {
        try {
            Category category = event.getGuild().getCategoriesByName(newGame.getName(), true).get(0);
            category.createTextChannel("out-of-game-chat").queue();
            category.createTextChannel("in-game-chat").queue();
            category.createTextChannel("turn-summary").queue();
            category.createTextChannel("game-actions").queue();
            category.createTextChannel("bribes").queue();
            category.createTextChannel("bidding-phase").queue();
            category.createTextChannel("rules").queue();
            category.createTextChannel("pre-game-voting").queue();
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Trying again, " + newGame.getName());
            Thread.sleep(1000);
            buildChannels(event, newGame);
        }
    }


}
