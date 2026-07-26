package dev.aurelium.auraskills.bukkit.requirement;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.util.armor.ArmorEquipEvent;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.entity.ThrownPotion;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Enforces configurable level gates before vanilla items are used or equipped. */
public class EquipmentRequirementListener implements Listener {

    private final AuraSkills plugin;
    private final EquipmentRequirementManager manager;
    private final Map<UUID, Long> lastMessage = new ConcurrentHashMap<>();

    public EquipmentRequirementListener(AuraSkills plugin, EquipmentRequirementManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        checkMaterial(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEquip(ArmorEquipEvent event) {
        checkMaterial(event.getPlayer(), event.getNewArmorPiece(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        checkMaterial(event.getPlayer(), item, event);
        if (!event.isCancelled() && (item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION)) {
            checkPotion(event.getPlayer(), item, event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.POTION) checkPotion(event.getPlayer(), event.getItem(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkMaterial(player, event.getConsumable(), event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPotionLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof ThrownPotion potion && potion.getShooter() instanceof Player player) {
            checkPotion(player, potion.getItem(), event);
        }
    }

    private void checkMaterial(Player player, ItemStack item, Cancellable event) {
        if (item == null || item.getType().isAir()) return;
        EquipmentRequirementManager.Requirement requirement = manager.getMaterialRequirement(item.getType());
        if (requirement != null) check(player, item.getType().name(), requirement, event);
    }

    private void checkPotion(Player player, ItemStack item, Cancellable event) {
        if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) return;
        EquipmentRequirementManager.Requirement requirement = manager.getPotionRequirement(potionMeta.getBasePotionType());
        if (requirement != null) check(player, potionMeta.getBasePotionType().name(), requirement, event);
    }

    private void check(Player player, String itemName, EquipmentRequirementManager.Requirement requirement, Cancellable event) {
        if (player.isOp() || player.hasPermission("auraskills.requirement.bypass")) {
            return;
        }
        User user = plugin.getUser(player);
        if (user.getSkillLevel(requirement.skill()) >= requirement.level()) return;
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        if (now - lastMessage.getOrDefault(player.getUniqueId(), 0L) >= 2000) {
            String message = manager.getDenyMessage()
                    .replace("{skill}", requirement.skill().getDisplayName(user.getLocale()))
                    .replace("{required}", String.valueOf(requirement.level()))
                    .replace("{item}", itemName);
            player.sendMessage(message.replace("<red>", "§c"));
            lastMessage.put(player.getUniqueId(), now);
        }
    }
}
