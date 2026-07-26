package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.slate.builder.MenuBuilder;
import dev.aurelium.slate.menu.ActiveMenu;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Map;

/** Confirmation screen for allocating one or more cap points to one skill. */
public class LimitPointConfirmMenu {
    private final AuraSkills plugin;
    public LimitPointConfirmMenu(AuraSkills plugin) { this.plugin = plugin; }

    public void build(MenuBuilder menu) {
        var global = new GlobalItems(plugin);
        menu.item("close", global::close);
        menu.fillItem(global::fill);
        menu.replaceTitle("skill", p -> skill(p.menu()).getDisplayName(p.locale(), false));
        menu.item("points", item -> {
            item.replace("points", p -> String.valueOf(remaining(p.player().getUniqueId(), pending(p.menu()))));
            item.modify(i -> {
                int points = remaining(i.player().getUniqueId(), pending(i.menu()));
                return points > 0 ? withAmount(i.item(), points) : null;
            });
        });
        menu.item("skill", item -> {
            item.replace("skill", p -> skill(p.menu()).getDisplayName(p.locale(), false));
            item.replace("pending", p -> String.valueOf(pending(p.menu())));
            item.replace("cap", p -> String.valueOf(plugin.getPlayerSkillCapManager().getEffectiveCap(p.player().getUniqueId(), skill(p.menu()))));
            item.modify(i -> withAmount(i.item(), pending(i.menu())));
        });
        menu.item("add", item -> {
            item.onClick(c -> reopen(c.player(), skill(c.menu()), Math.min(pending(c.menu()) + 1, maxPending(c.player().getUniqueId(), skill(c.menu())))));
        });
        menu.item("remove", item -> {
            item.onClick(c -> reopen(c.player(), skill(c.menu()), Math.max(0, pending(c.menu()) - 1)));
        });
        menu.item("back", item -> item.onClick(c -> plugin.getSlate().openMenu(c.player(), "limit_points")));
        menu.item("confirm", item -> {
            item.onClick(c -> confirm(c.player(), skill(c.menu()), pending(c.menu())));
        });
    }

    private void confirm(org.bukkit.entity.Player player, Skill skill, int points) {
        if (points <= 0) {
            player.sendMessage("\u00A7c\u8BF7\u5148\u70B9\u51FB\u00A7a+1\u00A7c\u9009\u62E9\u8981\u5206\u914D\u7684\u70B9\u6570\u3002");
            return;
        }
        try {
            if (plugin.getPlayerSkillCapManager().allocate(player.getUniqueId(), skill, points)) {
                player.sendMessage("§a已向 " + skill.getDisplayName(plugin.getUser(player).getLocale()) + " 分配 " + points + " 点技能上限。");
                plugin.getSlate().openMenu(player, "limit_points");
            } else player.sendMessage("§c无法分配技能点：余额不足或技能已达到全局上限。");
        } catch (IOException e) { player.sendMessage("§c保存技能上限点失败：" + e.getMessage()); }
    }

    private void reopen(org.bukkit.entity.Player player, Skill skill, int pending) {
        plugin.getSlate().openMenu(player, "limit_point_confirm", Map.of("skill", skill, "pending", pending));
    }

    private int maxPending(java.util.UUID uuid, Skill skill) {
        return Math.max(0, Math.min(plugin.getPlayerSkillCapManager().getAvailablePoints(uuid), skill.getMaxLevel() - plugin.getPlayerSkillCapManager().getEffectiveCap(uuid, skill)));
    }
    private int available(java.util.UUID uuid) { return plugin.getPlayerSkillCapManager().getAvailablePoints(uuid); }
    private int remaining(java.util.UUID uuid, int pending) { return Math.max(0, available(uuid) - pending); }
    private Skill skill(ActiveMenu menu) { return (Skill) menu.getProperty("skill"); }
    private int pending(ActiveMenu menu) { return (Integer) menu.getProperty("pending", 0); }
    private ItemStack withAmount(ItemStack item, int value) {
        item.setAmount(Math.min(64, Math.max(1, value)));
        return item;
    }
}
