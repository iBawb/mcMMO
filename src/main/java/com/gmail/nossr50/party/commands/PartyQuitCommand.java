package com.gmail.nossr50.party.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gmail.nossr50.events.party.McMMOPartyChangeEvent.EventReason;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.party.Party;
import com.gmail.nossr50.party.PartyManager;
import com.gmail.nossr50.util.Users;

public class PartyQuitCommand implements CommandExecutor {
    private Player player;
    private Party playerParty;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mcmmo.commands.party.quit")) {
            sender.sendMessage(command.getPermissionMessage());
            return true;
        }

        switch (args.length) {
        case 1:
            player = (Player) sender;
            playerParty = Users.getPlayer(player).getParty();

            if (!PartyManager.handlePartyChangeEvent(player, playerParty.getName(), null, EventReason.LEFT_PARTY)) {
                return true;
            }

            PartyManager.removeFromParty(player, playerParty);
            sender.sendMessage(LocaleLoader.getString("Commands.Party.Leave"));
            return true;

        default:
            sender.sendMessage(LocaleLoader.getString("Commands.Usage.1", "party", "[quit|q|leave]"));
            return true;
        }
    }
}
