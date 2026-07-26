package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.auraskills.bukkit.menus.shared.SkillItem;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.slate.builder.MenuBuilder;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashSet;
import java.util.Map;

/** Skills overview used to choose a skill for cap-point allocation. */
public class LimitPointsMenu {
    private final AuraSkills plugin;
    public LimitPointsMenu(AuraSkills plugin) { this.plugin = plugin; }

    public void build(MenuBuilder menu) {
        var global = new GlobalItems(plugin);
        menu.item("close", global::close);
        menu.fillItem(global::fill);
        menu.item("your_skills", item -> item.replace("player", p -> p.player().getName()));
        menu.item("stats", item -> {
            item.onClick(c -> plugin.getSlate().openMenu(c.player(), "stats", Map.of("previous_menu", "limit_points")));
            item.modify(i -> {
                if (i.item().getItemMeta() instanceof SkullMeta meta) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(i.player().getUniqueId()));
                    i.item().setItemMeta(meta);
                }
                return i.item();
            });
        });
        menu.item("points", item -> {
            item.replace("points", p -> String.valueOf(plugin.getPlayerSkillCapManager().getAvailablePoints(p.player().getUniqueId())));
            item.modify(i -> {
                ItemStack stack = i.item();
                stack.setAmount(Math.min(64, Math.max(1, plugin.getPlayerSkillCapManager().getAvailablePoints(i.player().getUniqueId()))));
                return stack;
            });
        });
        var skillItem = new SkillItem(plugin);
        menu.template("skill", Skill.class, template -> {
            skillItem.baseSkillItem(template);
            template.replace("cap", p -> String.valueOf(plugin.getPlayerSkillCapManager().getEffectiveCap(p.player().getUniqueId(), p.value())));
            template.replace("allocated", p -> String.valueOf(plugin.getPlayerSkillCapManager().getAllocatedPoints(p.player().getUniqueId(), p.value())));
            template.onClick(c -> {
                User user = plugin.getUser(c.player());
                if (user.hasSkillPermission(c.value())
                        && plugin.getPlayerSkillCapManager().getEffectiveCap(c.player().getUniqueId(), c.value()) < c.value().getMaxLevel()) {
                    plugin.getSlate().openMenu(c.player(), "limit_point_confirm", Map.of("skill", c.value(), "pending", 0));
                } else {
                    c.player().sendMessage("§c没有可分配的技能上限点，或该技能已达到全局上限。");
                }
            });
            template.definedContexts(m -> new HashSet<>(plugin.getSkillManager().getEnabledSkills()));
        });
    }

}
