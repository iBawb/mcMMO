package com.gmail.nossr50.commands.admin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gmail.nossr50.datatypes.McMMOPlayer;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.util.Users;

public class McrefreshCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PlayerProfile profile;

        switch (args.length) {
        case 0:
            if (!sender.hasPermission("mcmmo.commands.mcrefresh")) {
                sender.sendMessage(command.getPermissionMessage());
                return true;
            }

            if (!(sender instanceof Player)) {
                return false;
            }

            profile = Users.getPlayer(sender.getName()).getProfile();

            profile.setRecentlyHurt(0);
            profile.resetCooldowns();
            profile.resetToolPrepMode();
            profile.resetAbilityMode();

            sender.sendMessage(LocaleLoader.getString("Ability.Generic.Refresh"));
            return true;

        case 1:
            if (!sender.hasPermission("mcmmo.commands.mcrefresh.others")) {
                sender.sendMessage(command.getPermissionMessage());
                return true;
            }

            McMMOPlayer mcMMOPlayer = Users.getPlayer(args[0]);

            if (mcMMOPlayer == null) {
                profile = new PlayerProfile(args[0], false);

                if (!profile.isLoaded()) {
                    sender.sendMessage(LocaleLoader.getString("Commands.DoesNotExist"));
                    return true;
                }

                sender.sendMessage(LocaleLoader.getString("Commands.Offline"));
                return true;
            }
            profile = mcMMOPlayer.getProfile();
            Player player = mcMMOPlayer.getPlayer();

            if (!player.isOnline()) {
                sender.sendMessage(LocaleLoader.getString("Commands.Offline"));
                return true;
            }

            profile.setRecentlyHurt(0);
            profile.resetCooldowns();
            profile.resetToolPrepMode();
            profile.resetAbilityMode();

            player.sendMessage(LocaleLoader.getString("Ability.Generic.Refresh"));
            sender.sendMessage(LocaleLoader.getString("Commands.mcrefresh.Success", args[0]));
            return true;

        default:
            return false;
        }
    }
}
