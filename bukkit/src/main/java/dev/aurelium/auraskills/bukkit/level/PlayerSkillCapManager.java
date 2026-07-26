package dev.aurelium.auraskills.bukkit.level;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.util.file.FileUtil;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persists player cap bases and the unspent/allocated cap-point balance. */
public class PlayerSkillCapManager {

    private final AuraSkills plugin;
    private final File file;
    private final Map<UUID, Map<String, Integer>> baseCaps = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> allocatedPoints = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> availablePoints = new ConcurrentHashMap<>();
    private int defaultCap = 5;

    public PlayerSkillCapManager(AuraSkills plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player_caps.yml");
        load();
    }

    public synchronized void load() {
        baseCaps.clear();
        allocatedPoints.clear();
        availablePoints.clear();
        try {
            ConfigurationNode root = FileUtil.loadYamlFile(file);
            defaultCap = Math.max(0, root.node("default-cap").getInt(5));
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.node("players").childrenMap().entrySet()) {
                try {
                    UUID uuid = UUID.fromString(String.valueOf(entry.getKey()));
                    ConfigurationNode player = entry.getValue();
                    Map<String, Integer> bases = readSkillMap(player.node("base-caps"));
                    if (bases.isEmpty()) bases = readSkillMap(player.node("skills")); // v1 migration
                    Map<String, Integer> allocated = readSkillMap(player.node("allocated-points"));
                    int available = Math.max(0, player.node("available-points").getInt(0));
                    if (!bases.isEmpty()) baseCaps.put(uuid, new ConcurrentHashMap<>(bases));
                    if (!allocated.isEmpty()) allocatedPoints.put(uuid, new ConcurrentHashMap<>(allocated));
                    if (available > 0) availablePoints.put(uuid, available);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring invalid UUID in player_caps.yml: " + entry.getKey());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to load player_caps.yml: " + e.getMessage());
        }
    }

    public synchronized int getEffectiveCap(UUID uuid, Skill skill) {
        return Math.min(getConfiguredCap(uuid, skill), skill.getMaxLevel());
    }

    public synchronized int getConfiguredCap(UUID uuid, Skill skill) {
        return getBaseCap(uuid, skill) + getAllocatedPoints(uuid, skill);
    }

    public synchronized int getBaseCap(UUID uuid, Skill skill) {
        return baseCaps.getOrDefault(uuid, Map.of()).getOrDefault(key(skill), defaultCap);
    }

    public synchronized int getAllocatedPoints(UUID uuid, Skill skill) {
        return allocatedPoints.getOrDefault(uuid, Map.of()).getOrDefault(key(skill), 0);
    }

    public synchronized int getAvailablePoints(UUID uuid) {
        return availablePoints.getOrDefault(uuid, 0);
    }

    public synchronized int add(UUID uuid, Skill skill, int amount) throws IOException {
        int cap = Math.max(0, getBaseCap(uuid, skill) + amount);
        set(uuid, skill, cap);
        return cap;
    }

    public synchronized void set(UUID uuid, Skill skill, int cap) throws IOException {
        Map<UUID, Map<String, Integer>> bases = copy(baseCaps);
        bases.computeIfAbsent(uuid, id -> new HashMap<>()).put(key(skill), Math.max(0, cap));
        commit(bases, copy(allocatedPoints), new HashMap<>(availablePoints));
    }

    public synchronized void reset(UUID uuid, Skill skill) throws IOException {
        Map<UUID, Map<String, Integer>> bases = copy(baseCaps);
        Map<String, Integer> playerBases = bases.get(uuid);
        if (playerBases != null) {
            playerBases.remove(key(skill));
            if (playerBases.isEmpty()) bases.remove(uuid);
        }
        commit(bases, copy(allocatedPoints), new HashMap<>(availablePoints));
    }

    public synchronized int addAvailablePoints(UUID uuid, int amount) throws IOException {
        int balance = Math.max(0, getAvailablePoints(uuid) + amount);
        Map<UUID, Integer> available = new HashMap<>(availablePoints);
        if (balance == 0) available.remove(uuid); else available.put(uuid, balance);
        commit(copy(baseCaps), copy(allocatedPoints), available);
        return balance;
    }

    public synchronized void setAvailablePoints(UUID uuid, int amount) throws IOException {
        Map<UUID, Integer> available = new HashMap<>(availablePoints);
        if (amount <= 0) available.remove(uuid); else available.put(uuid, amount);
        commit(copy(baseCaps), copy(allocatedPoints), available);
    }

    public synchronized boolean allocate(UUID uuid, Skill skill, int points) throws IOException {
        if (points < 1 || getAvailablePoints(uuid) < points || getEffectiveCap(uuid, skill) + points > skill.getMaxLevel()) return false;
        Map<UUID, Map<String, Integer>> allocated = copy(allocatedPoints);
        allocated.computeIfAbsent(uuid, id -> new HashMap<>()).merge(key(skill), points, Integer::sum);
        Map<UUID, Integer> available = new HashMap<>(availablePoints);
        int balance = getAvailablePoints(uuid) - points;
        if (balance == 0) available.remove(uuid); else available.put(uuid, balance);
        commit(copy(baseCaps), allocated, available);
        return true;
    }

    private Map<String, Integer> readSkillMap(ConfigurationNode node) {
        Map<String, Integer> values = new HashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            int value = entry.getValue().getInt(-1);
            if (value >= 0) values.put(String.valueOf(entry.getKey()).toLowerCase(), value);
        }
        return values;
    }

    private void commit(Map<UUID, Map<String, Integer>> bases, Map<UUID, Map<String, Integer>> allocated,
                        Map<UUID, Integer> available) throws IOException {
        save(bases, allocated, available);
        baseCaps.clear(); baseCaps.putAll(toConcurrent(bases));
        allocatedPoints.clear(); allocatedPoints.putAll(toConcurrent(allocated));
        availablePoints.clear(); availablePoints.putAll(available);
    }

    private void save(Map<UUID, Map<String, Integer>> bases, Map<UUID, Map<String, Integer>> allocated,
                      Map<UUID, Integer> available) throws IOException {
        ConfigurationNode root = FileUtil.loadYamlFile(file);
        root.node("version").set(2);
        root.node("default-cap").set(defaultCap);
        root.node("players").set(null);
        for (UUID uuid : union(bases, allocated, available)) {
            for (Map.Entry<String, Integer> entry : bases.getOrDefault(uuid, Map.of()).entrySet())
                root.node("players", uuid.toString(), "base-caps", entry.getKey()).set(entry.getValue());
            for (Map.Entry<String, Integer> entry : allocated.getOrDefault(uuid, Map.of()).entrySet())
                root.node("players", uuid.toString(), "allocated-points", entry.getKey()).set(entry.getValue());
            int balance = available.getOrDefault(uuid, 0);
            if (balance > 0) root.node("players", uuid.toString(), "available-points").set(balance);
        }
        FileUtil.saveYamlFile(file, root);
    }

    private static Map<UUID, Map<String, Integer>> copy(Map<UUID, Map<String, Integer>> source) {
        Map<UUID, Map<String, Integer>> copy = new HashMap<>();
        source.forEach((uuid, values) -> copy.put(uuid, new HashMap<>(values)));
        return copy;
    }

    private static Map<UUID, Map<String, Integer>> toConcurrent(Map<UUID, Map<String, Integer>> source) {
        Map<UUID, Map<String, Integer>> result = new ConcurrentHashMap<>();
        source.forEach((uuid, values) -> result.put(uuid, new ConcurrentHashMap<>(values)));
        return result;
    }

    private static java.util.Set<UUID> union(Map<UUID, ?> first, Map<UUID, ?> second, Map<UUID, ?> third) {
        java.util.Set<UUID> ids = new java.util.HashSet<>(first.keySet());
        ids.addAll(second.keySet()); ids.addAll(third.keySet());
        return ids;
    }

    private static String key(Skill skill) { return skill.getId().toString().toLowerCase(); }
}
