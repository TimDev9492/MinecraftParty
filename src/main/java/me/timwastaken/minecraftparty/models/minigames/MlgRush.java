package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.templates.InvLayoutBasedMinigame;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.enums.ItemType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MlgRush extends InvLayoutBasedMinigame implements GameEventListener {

    private static MinigameType type = MinigameType.MLG_RUSH;

    private final Random rnd;

    private final ArrayList<Block> placedBlocks;
    private final HashMap<UUID, Integer> gamesPlayed;
    private final HashMap<UUID, Integer> playerLives;
//    private final ConcurrentHashMap<UUID, HashMap<Integer, ItemType>> hotbarLayouts;

    private final UUID[] currentlyFighting;
    private Location[] fightSpawns;
    private Location[] bedLocations;
    private Location spectatorSpawn;
    private ItemStack[] fighterInv;
    private final Player[] players;

    private int startLives;
    private int resetDepth;
    private int deathY;
    private int buildHeight;
    private double bedProtectionRadius;
    private Material bedMaterial;

    public MlgRush(Player... players) {
        super(type, List.of(MinigameFlag.ZERO_DAMAGE), players);
        super.addGameEventListeners(this);
        this.players = players;
        rnd = new Random();
        placedBlocks = new ArrayList<>();
        gamesPlayed = new HashMap<>();
        playerLives = new HashMap<>();
        currentlyFighting = new UUID[2];
//        hotbarLayouts = new ConcurrentHashMap<>();
//        for (Player p : players) {
//            hotbarLayouts.put(p.getUniqueId(), loadHotbarLayout(p.getUniqueId()));
//            hotbarLayouts.get(p.getUniqueId()).forEach((k, v) -> System.out.println(p.getName() + " " + k + " -> " + v.toString()));
//        }
    }

//    private HashMap<Integer, ItemType> loadHotbarLayout(UUID id) {
//        // CREATE DATABASE LOOKUP
//        HashMap<Integer, ItemType> layout = DatabaseManager.getInvLayout(id, type);
//        if (layout != null) {
//            return layout;
//        } else {
//            HashMap<Integer, ItemType> hotbar = new HashMap<>();
//            hotbar.put(0, ItemType.WEAPON);
//            hotbar.put(1, ItemType.TOOL);
//            hotbar.put(7, ItemType.BLOCKS);
//            return hotbar;
//        }
//    }

    public int getDeathY() {
        return deathY;
    }

    public void addPlacedBlock(Block b) {
        placedBlocks.add(b);
    }

    public boolean isPlacedBlock(Block b) {
        return placedBlocks.contains(b);
    }

    public boolean isFighting(Player p) {
        return currentlyFighting[0] == p.getUniqueId() || currentlyFighting[1] == p.getUniqueId();
    }

    @Override
    public void onGameStart() {
        deathY = origin.getBlockY() - resetDepth;
        generateNewFightingPlayers();
    }

    @Override
    public void onGameEnd() {
        resetMap();
        saveLayoutsToDatabase();
    }

    public Material getBedMaterial() {
        return bedMaterial;
    }

    @Override
    public void onWorldLoaded() {
        ConfigurationSection section = MinecraftParty.getInstance().getConfig().getConfigurationSection("minigames." + type.getAlias());
        fightSpawns = new Location[]{
                new Location(gameWorld, section.getInt("spawn1.x") + 0.5, section.getInt("spawn1.y"), section.getInt("spawn1.z") + 0.5),
                new Location(gameWorld, section.getInt("spawn2.x") + 0.5, section.getInt("spawn2.y"), section.getInt("spawn2.z") + 0.5)
        };
        fightSpawns[0].setYaw((float) section.getDouble("spawn1.yaw"));
        fightSpawns[1].setYaw((float) section.getDouble("spawn2.yaw"));
        bedLocations = new Location[]{
                new Location(gameWorld, section.getInt("bed1.x") + 0.5, section.getInt("bed1.y"), section.getInt("bed1.z") + 0.5),
                new Location(gameWorld, section.getInt("bed2.x") + 0.5, section.getInt("bed2.y"), section.getInt("bed2.z") + 0.5)
        };
        spectatorSpawn = new Location(gameWorld, section.getInt("spectator_spawn.x") + 0.5, section.getInt("spectator_spawn.y"), section.getInt("spectator_spawn.z") + 0.5);
        spectatorSpawn.setPitch((float) section.getDouble("spectator_spawn.pitch"));
        spectatorSpawn.setYaw((float) section.getDouble("spectator_spawn.yaw"));
        resetDepth = section.getInt("reset_depth");
        buildHeight = origin.getBlockY() + section.getInt("build_height") - 1;
        bedMaterial = Material.valueOf(section.getString("bed_material"));
        bedProtectionRadius = section.getDouble("bed_protection_radius");
        startLives = section.getInt("lives");
        fighterInv = new ItemStack[]{
                new ItemStack(Material.valueOf(section.getString("weapon_material"))),
                new ItemStack(Material.valueOf(section.getString("tool_material"))),
                new ItemStack(Material.valueOf(section.getString("block_material")), 64)
        };
        setFallback(new HashMap<>() {{
            put(0, ItemType.WEAPON);
            put(1, ItemType.TOOL);
            put(2, ItemType.BLOCKS);
        }});
        setItemMap(new HashMap<>() {{
            put(ItemType.WEAPON, fighterInv[0]);
            put(ItemType.TOOL, fighterInv[1]);
            put(ItemType.BLOCKS, fighterInv[2]);
        }});
        ItemStack weapon = fighterInv[0];
        ItemMeta meta = weapon.getItemMeta();
        meta.addEnchant(Enchantment.KNOCKBACK, 2, false);
        weapon.setItemMeta(meta);
        ItemStack tool = fighterInv[1];
        meta = tool.getItemMeta();
        meta.addEnchant(Enchantment.DIG_SPEED, 2, false);
        meta.setUnbreakable(true);
        tool.setItemMeta(meta);

        for (Player p : players) {
            gamesPlayed.put(p.getUniqueId(), 0);
            playerLives.put(p.getUniqueId(), startLives);
            p.getInventory().clear();
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(spectatorSpawn);
        }
    }

    @Override
    public void onPlayerLeave(Player p) {
        Player other = currentlyFighting[0] == p.getUniqueId() ? Bukkit.getPlayer(currentlyFighting[1]) : Bukkit.getPlayer(currentlyFighting[0]);
        if (other == null) return;
        if (isFighting(p)) {
            updateInvLayout(other);
            updateInvLayout(p);
            resetMap();
            generateNewFightingPlayers();
        }
        gamesPlayed.remove(p.getUniqueId());
    }

    private void makeSpectator(Player p) {
        p.getInventory().clear();
        p.updateInventory();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(spectatorSpawn);
    }

    public void bedBroken(Player by, Block bed) {
        // Add point system
        if (!isFighting(by)) return;
        Player winner = by, looser;
        if (by.getUniqueId() == currentlyFighting[0]) {
            looser = Bukkit.getPlayer(currentlyFighting[1]);
        } else {
            looser = Bukkit.getPlayer(currentlyFighting[0]);
        }
        if (bed.getLocation().distanceSquared(getSpawn(winner)) < bed.getLocation().distanceSquared(getSpawn(looser))) {
            return;
        }
        updateInvLayout(winner);
        updateInvLayout(looser);
        makeSpectator(winner);
        makeSpectator(looser);
        winner.playSound(winner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        looser.playSound(looser.getLocation(), Sound.ENTITY_CAT_HISS, 1f, 1f);
        removeLife(looser.getUniqueId());
        resetMap();
        if (gamesPlayed.keySet().size() > 1) generateNewFightingPlayers();
    }

    public void teleportBack(Player p, boolean update) {
        if (!isFighting(p)) return;
        p.setFallDistance(0);
        if (p.getUniqueId() == currentlyFighting[0]) {
            p.teleport(fightSpawns[0]);
        } else {
            p.teleport(fightSpawns[1]);
        }
        p.setGameMode(GameMode.SURVIVAL);
        if (update) {
            updateInvLayout(p);
        }
        resetInventory(p);
    }

//    private void resetInventory(Player p) {
//        updateInvLayout(p);
//        p.getInventory().clear();
//        HashMap<Integer, ItemType> hotbar = hotbarLayouts.get(p.getUniqueId());
//        if (hotbar.isEmpty()) p.getInventory().addItem(fighterInv);
//        else {
//            for (int slot : hotbar.keySet()) {
//                p.getInventory().setItem(slot, getItemStackFromType(hotbar.get(slot)));
//            }
//        }
//    }

    private Location getSpawn(Player p) {
        if (!isFighting(p)) return null;
        if (p.getUniqueId() == currentlyFighting[0]) {
            return fightSpawns[0];
        } else {
            return fightSpawns[1];
        }
    }

    private void removeLife(UUID uuid) {
        int next = playerLives.get(uuid) - 1;
        playerLives.put(uuid, next);
        if (next == 0) {
            playerOut(uuid);
        }
        checkEnd();
    }

    public int getBuildHeight() {
        return buildHeight;
    }

    private void playerOut(UUID id) {
        gamesPlayed.remove(id);
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        p.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You're out", ChatColor.GRAY + "out of lives", 10, 80, 10);
    }

    private void checkEnd() {
        if (gamesPlayed.size() == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + Bukkit.getPlayer(gamesPlayed.keySet().iterator().next()).getName(), ChatColor.GRAY + "won the game", 10, 80, 10);
            });
            endGame();
        }
    }

    public void resetMap() {
        for (int i = placedBlocks.size() - 1; i >= 0; i--) {
            placedBlocks.get(i).setType(Material.AIR);
            placedBlocks.remove(i);
        }
    }

    private void generateNewFightingPlayers() {
        int minGamesPlayed = -1;
        int secondMin = -1;
        for (UUID id : gamesPlayed.keySet()) {
            if (minGamesPlayed == -1) minGamesPlayed = gamesPlayed.get(id);
            else if (gamesPlayed.get(id) < minGamesPlayed) {
                minGamesPlayed = gamesPlayed.get(id);
            } else if (secondMin == -1 || gamesPlayed.get(id) < secondMin) {
                secondMin = gamesPlayed.get(id);
            }
        }
        ArrayList<UUID> possiblePlayers = new ArrayList<>();
        for (UUID id : gamesPlayed.keySet()) {
            if (gamesPlayed.get(id) == minGamesPlayed) possiblePlayers.add(id);
        }
        if (possiblePlayers.size() == 1) {
            currentlyFighting[0] = possiblePlayers.get(0);
            possiblePlayers.clear();
            for (UUID id : gamesPlayed.keySet()) {
                if (gamesPlayed.get(id) == secondMin) possiblePlayers.add(id);
            }
            currentlyFighting[1] = possiblePlayers.get(rnd.nextInt(possiblePlayers.size()));
        } else {
            int randomIndex = rnd.nextInt(possiblePlayers.size());
            currentlyFighting[0] = possiblePlayers.get(randomIndex);
            possiblePlayers.remove(randomIndex);
            randomIndex = rnd.nextInt(possiblePlayers.size());
            currentlyFighting[1] = possiblePlayers.get(randomIndex);
        }
        gamesPlayed.put(currentlyFighting[0], gamesPlayed.get(currentlyFighting[0]) + 1);
        gamesPlayed.put(currentlyFighting[1], gamesPlayed.get(currentlyFighting[1]) + 1);
        Player p1 = Bukkit.getPlayer(currentlyFighting[0]);
        Player p2 = Bukkit.getPlayer(currentlyFighting[1]);

        teleportBack(p1, false);
        teleportBack(p2, false);
    }

    public boolean isNearOwnBed(Player player, Block block) {
        if (!isFighting(player)) return false;
        Location comp = block.getLocation().add(0.5, 0, 0.5);
        Location bed;
        Location spawn;
        if (player.getUniqueId() == currentlyFighting[0]) {
            bed = bedLocations[0];
            spawn = fightSpawns[0];
        } else {
            bed = bedLocations[1];
            spawn = fightSpawns[1];
        }
        return bed.distance(comp) <= bedProtectionRadius || spawn.distance(comp) <= bedProtectionRadius;
    }

    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        // instance check already happened
        Player p = (Player) event.getEntity();
        Material mat = event.getItem().getItemStack().getType();
        int amount = event.getItem().getItemStack().getAmount();
        Material blockMat = getItemMap().get(ItemType.BLOCKS).getType();
        int amountInInv = countMaterials(p.getInventory(), blockMat);
        if (amountInInv >= blockMat.getMaxStackSize()) event.setCancelled(true);
        if (amountInInv + amount > blockMat.getMaxStackSize()) {
            event.getItem().getItemStack().setAmount(blockMat.getMaxStackSize() - amountInInv);
        }
    }

    private int countMaterials(Inventory inv, Material mat) {
        int amount = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() == mat) amount += stack.getAmount();
        }
        return amount;
    }

//    private void updateInvLayout(Player p) {
//        HashMap<Integer, ItemType> layout = new HashMap<>();
//        for (int i = 0; i < p.getInventory().getContents().length; i++) {
//            ItemStack current = p.getInventory().getItem(i);
//            if (current == null) continue;
//            if (current.equals(fighterInv[0])) layout.put(i, ItemType.WEAPON);
//            if (current.equals(fighterInv[1])) layout.put(i, ItemType.TOOL);
//            if (current.equals(fighterInv[2])) layout.put(i, ItemType.BLOCKS);
//        }
//        if (!layout.isEmpty()) {
//            hotbarLayouts.put(p.getUniqueId(), layout);
//            System.out.println("updated " + p.getName() + "'s inventory layout: " + layout);
//        }
//    }

//    public boolean saveLayoutToDatabase(Player p) {
//        HashMap<Integer, ItemType> layout = new HashMap<>();
//        for (int i = 0; i < p.getInventory().getContents().length; i++) {
//            ItemStack current = p.getInventory().getItem(i);
//            if (current == null) continue;
//            if (current.equals(fighterInv[0])) layout.put(i, ItemType.WEAPON);
//            if (current.equals(fighterInv[1])) layout.put(i, ItemType.TOOL);
//            if (current.equals(fighterInv[2])) layout.put(i, ItemType.BLOCKS);
//        }
//        return DatabaseManager.saveInvLayout(p.getUniqueId(), type, layout);
//    }
//
//    public void saveLayoutsToDatabase() {
//        hotbarLayouts.forEach((uuid, map) -> System.out.println(Bukkit.getPlayer(uuid) + " " + DatabaseManager.saveInvLayout(uuid, type, map)));
//    }

}
