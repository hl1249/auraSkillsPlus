package dev.aurelium.auraskills.bukkit.requirement;

import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.config.ConfigurateLoader;
import org.bukkit.Material;
import org.bukkit.potion.PotionType;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Loads configured skill requirements for vanilla equipment, potions, and arrows. */
public class EquipmentRequirementManager {

    public record Requirement(Skill skill, int level) { }

    private final AuraSkills plugin;
    private final Map<Material, Requirement> materialRequirements = new HashMap<>();
    private final Map<PotionType, Requirement> potionRequirements = new HashMap<>();
    private Requirement defaultPotionRequirement;
    private String denyMessage = "<red>{skill} level {required} is required to use {item}.";

    public EquipmentRequirementManager(AuraSkills plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        materialRequirements.clear();
        potionRequirements.clear();
        defaultPotionRequirement = null;
        try {
            ConfigurateLoader loader = new ConfigurateLoader(plugin, TypeSerializerCollection.builder().build());
            ConfigurationNode config = loader.loadUserFile("equipment_requirements.yml");
            denyMessage = config.node("deny-message").getString(denyMessage);
            loadMaterialGroups(config.node("requirements"));
            loadMaterialGroups(config.node("arrow-requirements"));
            loadPotionGroups(config.node("consumable-requirements"));
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to load equipment_requirements.yml: " + e.getMessage());
        }
    }

    private void loadMaterialGroups(ConfigurationNode root) {
        for (Map.Entry<Object, ? extends ConfigurationNode> skillEntry : root.childrenMap().entrySet()) {
            Skill skill = getSkill(String.valueOf(skillEntry.getKey()));
            if (skill == null) continue;
            for (Map.Entry<Object, ? extends ConfigurationNode> materialEntry : skillEntry.getValue().childrenMap().entrySet()) {
                try {
                    Material material = Material.valueOf(String.valueOf(materialEntry.getKey()).toUpperCase(Locale.ROOT));
                    int level = materialEntry.getValue().getInt(-1);
                    if (level >= 0 && !materialRequirements.containsKey(material)) {
                        materialRequirements.put(material, new Requirement(skill, level));
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring unknown material in equipment_requirements.yml: " + materialEntry.getKey());
                }
            }
        }
    }

    private void loadPotionGroups(ConfigurationNode root) {
        for (Map.Entry<Object, ? extends ConfigurationNode> skillEntry : root.childrenMap().entrySet()) {
            Skill skill = getSkill(String.valueOf(skillEntry.getKey()));
            if (skill == null) continue;
            int defaultRequiredLevel = skillEntry.getValue().node("default-required-level").getInt(-1);
            if (defaultRequiredLevel >= 0 && defaultPotionRequirement == null) {
                defaultPotionRequirement = new Requirement(skill, defaultRequiredLevel);
            }
            for (Map.Entry<Object, ? extends ConfigurationNode> potionEntry : skillEntry.getValue().node("potion-effects").childrenMap().entrySet()) {
                try {
                    PotionType type = PotionType.valueOf(String.valueOf(potionEntry.getKey()).toUpperCase(Locale.ROOT));
                    int level = potionEntry.getValue().getInt(-1);
                    if (level >= 0) potionRequirements.put(type, new Requirement(skill, level));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring unknown potion type in equipment_requirements.yml: " + potionEntry.getKey());
                }
            }
        }
    }

    private Skill getSkill(String key) {
        Skill skill = plugin.getSkillRegistry().getOrNull(NamespacedId.fromDefault(key.toLowerCase(Locale.ROOT)));
        return skill != null && skill.isEnabled() ? skill : null;
    }

    public Requirement getMaterialRequirement(Material material) {
        return materialRequirements.get(material);
    }

    public Requirement getPotionRequirement(PotionType type) {
        return potionRequirements.getOrDefault(type, defaultPotionRequirement);
    }

    public String getDenyMessage() {
        return denyMessage;
    }
}
