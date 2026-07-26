package dev.aurelium.auraskills.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

/** Management commands for optional per-player skill caps. */
@CommandAlias("%skills_alias")
public class PlayerCapCommand extends BaseCommand {

    private final AuraSkills plugin;

    public PlayerCapCommand(AuraSkills plugin) {
        this.plugin = plugin;
    }

    @Subcommand("addlevellimit")
    @CommandCompletion("@skills @players")
    @CommandPermission("auraskills.command.cap.add")
    public void addLevel(CommandSender sender, Skill skill, String playerName, int amount) {
        OfflinePlayer player = getPlayer(sender, playerName);
        if (player == null) return;
        try {
            int oldCap = plugin.getPlayerSkillCapManager().getBaseCap(player.getUniqueId(), skill);
            int newCap = plugin.getPlayerSkillCapManager().add(player.getUniqueId(), skill, amount);
            sender.sendMessage("§a" + player.getName() + " " + skill.getId() + " base cap: " + oldCap + " -> " + newCap);
        } catch (IOException e) {
            sender.sendMessage("§cUnable to save player skill cap: " + e.getMessage());
        }
    }

    @Subcommand("setlevellimit")
    @CommandCompletion("@skills @players")
    @CommandPermission("auraskills.command.cap.set")
    public void setLevelCap(CommandSender sender, Skill skill, String playerName, int cap) {
        OfflinePlayer player = getPlayer(sender, playerName);
        if (player == null) return;
        if (cap < 0) {
            sender.sendMessage("§cCap must be zero or greater.");
            return;
        }
        try {
            plugin.getPlayerSkillCapManager().set(player.getUniqueId(), skill, cap);
            sender.sendMessage("§a" + player.getName() + " " + skill.getId() + " cap set to " + cap);
        } catch (IOException e) {
            sender.sendMessage("§cUnable to save player skill cap: " + e.getMessage());
        }
    }

    @Subcommand("getlevellimit")
    @CommandCompletion("@skills @players")
    @CommandPermission("auraskills.command.cap.get")
    public void getLevelCap(CommandSender sender, Skill skill, String playerName) {
        OfflinePlayer player = getPlayer(sender, playerName);
        if (player == null) return;
        int base = plugin.getPlayerSkillCapManager().getBaseCap(player.getUniqueId(), skill);
        int allocated = plugin.getPlayerSkillCapManager().getAllocatedPoints(player.getUniqueId(), skill);
        int effective = plugin.getPlayerSkillCapManager().getEffectiveCap(player.getUniqueId(), skill);
        sender.sendMessage("§a" + player.getName() + " " + skill.getId() + " cap: base=" + base
                + ", allocated=" + allocated + ", global=" + skill.getMaxLevel() + ", effective=" + effective);
    }

    @Subcommand("resetlevellimit")
    @CommandCompletion("@skills @players")
    @CommandPermission("auraskills.command.cap.reset")
    public void resetLevelCap(CommandSender sender, Skill skill, String playerName) {
        OfflinePlayer player = getPlayer(sender, playerName);
        if (player == null) return;
        try {
            plugin.getPlayerSkillCapManager().reset(player.getUniqueId(), skill);
            sender.sendMessage("§a" + player.getName() + " " + skill.getId() + " cap reset to default value");
        } catch (IOException e) {
            sender.sendMessage("§cUnable to save player skill cap: " + e.getMessage());
        }
    }

    @Subcommand("addskilllimitpoints")
    @CommandCompletion("@players")
    @CommandPermission("auraskills.command.limitpoints.add")
    public void addSkillLimitPoints(CommandSender sender, String playerName, int amount) {
        OfflinePlayer player = getPlayer(sender, playerName);
        if (player == null || amount < 1) return;
        try {
            int balance = plugin.getPlayerSkillCapManager().addAvailablePoints(player.getUniqueId(), amount);
            sender.sendMessage("§a" + player.getName() + " now has " + balance + " available limit points.");
        } catch (IOException e) { sender.sendMessage("§cUnable to save limit points: " + e.getMessage()); }
    }

    @Subcommand("setskilllimitpoints")
    @CommandCompletion("@players")
    @CommandPermission("auraskills.command.limitpoints.set")
    public void setSkillLimitPoints(CommandSender sender, String playerName, int amount) {
        OfflinePlayer player = getPlayer(sender, playerName);
        if (player == null || amount < 0) return;
        try {
            plugin.getPlayerSkillCapManager().setAvailablePoints(player.getUniqueId(), amount);
            sender.sendMessage("§a" + player.getName() + " now has " + amount + " available limit points.");
        } catch (IOException e) { sender.sendMessage("§cUnable to save limit points: " + e.getMessage()); }
    }

    @Subcommand("getskilllimitpoints")
    @CommandCompletion("@players")
    @CommandPermission("auraskills.command.limitpoints.get")
    public void getSkillLimitPoints(CommandSender sender, String playerName) {
        OfflinePlayer player = getPlayer(sender, playerName);
        if (player == null) return;
        sender.sendMessage("§a" + player.getName() + " has "
                + plugin.getPlayerSkillCapManager().getAvailablePoints(player.getUniqueId()) + " available limit points.");
    }

    @Subcommand("takeskilllimitpoints")
    @CommandCompletion("@players")
    @CommandPermission("auraskills.command.limitpoints.take")
    public void takeSkillLimitPoints(CommandSender sender, String playerName, int amount) {
        OfflinePlayer player = getPlayer(sender, playerName);
        if (player == null || amount < 1) return;
        int current = plugin.getPlayerSkillCapManager().getAvailablePoints(player.getUniqueId());
        if (current < amount) { sender.sendMessage("§cPlayer does not have enough available limit points."); return; }
        try {
            int balance = plugin.getPlayerSkillCapManager().addAvailablePoints(player.getUniqueId(), -amount);
            sender.sendMessage("§a" + player.getName() + " now has " + balance + " available limit points.");
        } catch (IOException e) { sender.sendMessage("§cUnable to save limit points: " + e.getMessage()); }
    }

    @Subcommand("limitpoints")
    @CommandPermission("auraskills.command.limitpoints.use")
    public void openLimitPoints(Player player) {
        plugin.getSlate().openMenu(player, "limit_points");
    }

    private OfflinePlayer getPlayer(CommandSender sender, String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (!player.isOnline() && !player.hasPlayedBefore()) {
            sender.sendMessage("§cPlayer not found: " + playerName);
            return null;
        }
        return player;
    }
}
