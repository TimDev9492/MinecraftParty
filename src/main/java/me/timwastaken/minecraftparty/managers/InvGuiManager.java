package me.timwastaken.minecraftparty.managers;

import me.timwastaken.minecraftparty.models.enums.ItemType;
import me.timwastaken.minecraftparty.models.other.InventoryKit;
import me.timwastaken.minecraftparty.models.templates.InvLayoutBasedMinigame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvGuiManager implements Listener {

    private static Inventory menu;
    private static ItemStack placeHolder;
    private static ItemStack saveItem;
    private static ItemStack cancelItem;

    private static final String prefix = ChatColor.DARK_RED + "" + ChatColor.BOLD + "InvManager" + ChatColor.GRAY + " - ";
    private static final String menuTitle = ChatColor.RED + "" + ChatColor.ITALIC + "Choose a kit";

    public static void init() {
        menu = Bukkit.createInventory(null, (int) Math.ceil(KitManager.getInventoryKits().size() / 9f) * 9, prefix + menuTitle);
        KitManager.getInventoryKits().forEach((alias, kitInv) -> {
            ItemStack item = new ItemStack(Material.ENDER_CHEST);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GRAY + "" + ChatColor.BOLD + kitInv.getName());
            meta.setLore(List.of(kitInv.getAlias()));
            item.setItemMeta(meta);
            menu.addItem(item);
        });
        placeHolder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeHolder.getItemMeta();
        meta.setDisplayName(" ");
        placeHolder.setItemMeta(meta);
        saveItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        meta = saveItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Save layout");
        saveItem.setItemMeta(meta);
        cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        meta = cancelItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Cancel");
        cancelItem.setItemMeta(meta);
    }

    public static Inventory getConfigGui() {
        return menu;
    }

    @EventHandler
    public static void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            if (event.getView().getTitle().startsWith(prefix)) {
                if (event.getClickedInventory() != null) {
                    if (event.getView().getTitle().endsWith(menuTitle) && event.getClickedInventory().getType() != InventoryType.PLAYER) {
                        if (event.getCurrentItem() != null) {
                            ItemStack clicked = event.getCurrentItem();

                            InventoryKit clickedKit = KitManager.getKit(clicked.getItemMeta().getLore().get(0));
                            HashMap<Integer, ItemType> playerInvLayout = DatabaseManager.getInvLayout(p.getUniqueId(), clickedKit);
                            p.openInventory(fromLayout(clickedKit, playerInvLayout));
                        }
                        event.setCancelled(true);
                    } else {
                        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getClickedInventory().getType() == InventoryType.PLAYER) {
                            p.playSound(p.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1f, 1f);
                            event.setCancelled(true);
                        }
                        if (event.getCurrentItem() != null) {
                            if (areSimilar(event.getCurrentItem(), placeHolder))
                                event.setCancelled(true);
                            else if (areSimilar(event.getCurrentItem(), cancelItem)) {
                                event.setCancelled(true);
                                cancel(p);
                            } else if (areSimilar(event.getCurrentItem(), saveItem)) {
                                event.setCancelled(true);
                                String alias = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                                save(p, event.getClickedInventory(), KitManager.getKit(alias));
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        for (ItemStack stack : event.getPlayer().getInventory().getContents()) {
            if (stack == null || stack.getItemMeta() == null || !stack.getItemMeta().hasLore() || !stack.getItemMeta().getLore().get(0).equals("."))
                continue;
            stack.setAmount(0);
        }
    }

    private static Inventory fromLayout(InventoryKit clickedKit, HashMap<Integer, ItemType> playerInvLayout) {
        if (playerInvLayout == null) playerInvLayout = clickedKit.getFallback();
        Inventory customInv = Bukkit.createInventory(null, 45, prefix + clickedKit.getName());
        HashMap<ItemType, ItemStack> itemMap = clickedKit.toItemMap();
        for (int i = 1; i < 7; i++) customInv.setItem(i, placeHolder);
        customInv.setItem(7, cancelItem);
        ItemStack customSave = saveItem.clone();
        ItemMeta saveMeta = customSave.getItemMeta();
        saveMeta.setLore(List.of(clickedKit.getAlias()));
        customSave.setItemMeta(saveMeta);
        customInv.setItem(8, customSave);
        playerInvLayout.forEach((slot, itemType) -> {
            int translatedSlot = slot == 40 ? 0 : (slot < 9 ? (slot - 9 + customInv.getSize()) % customInv.getSize() : slot);
            ItemStack itemStack = itemMap.get(itemType);
            if (itemStack != null) {
                ItemMeta meta = itemStack.getItemMeta();
                meta.setLore(List.of("."));
                itemStack.setItemMeta(meta);
                customInv.setItem(translatedSlot, itemStack);
            }
        });
        return customInv;
    }

    private static void cancel(Player p) {
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
        p.sendMessage(ChatColor.RED + "No changes got applied");
    }

    private static void save(Player p, Inventory customInv, InventoryKit kit) {
        // save layout here
        if (countRelevantItems(customInv) == kit.toItemMap().size()) {
            HashMap<Integer, ItemType> playerLayout = getLayoutFromInv(customInv, kit);
            if (!DatabaseManager.saveInvLayout(p.getUniqueId(), kit, playerLayout)) {
                p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "An error occurred...");
                return;
            }
        } else {
            cancel(p);
            return;
        }
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        p.sendMessage(ChatColor.GREEN + "Inventory layout saved successfully!");
        if (GameManager.getActiveMinigame() instanceof InvLayoutBasedMinigame invLayoutBasedMinigame) {
            invLayoutBasedMinigame.reloadPlayerInventory(p);
        }
    }

    private static HashMap<Integer, ItemType> getLayoutFromInv(Inventory customInv, InventoryKit kit) {
        HashMap<Integer, ItemType> layout = new HashMap<>();
        HashMap<ItemType, ItemStack> itemMap = kit.toItemMap();
        for (int i = 0; i < customInv.getContents().length; i++) {
            ItemStack content = customInv.getContents()[i];
            if (content == null) continue;
            for (Map.Entry<ItemType, ItemStack> entry : itemMap.entrySet()) {
                ItemStack stack = entry.getValue();
                ItemType type = entry.getKey();
                if (areSimilarWithCount(stack, content) && !layout.containsValue(type)) {
                    int translatedSlot = i == 0 ? 40 : (i > 35 ? (i + 9) % customInv.getSize() : i);
                    System.out.println(translatedSlot + " -> " + type);
                    layout.put(translatedSlot, type);
                    break;
                }
            }
        }
        return layout;
    }

    private static int countRelevantItems(Inventory inv) {
        int count = 0;
        for (ItemStack content : inv.getContents()) {
            if (content != null && !areSimilar(content, saveItem) && !areSimilar(content, cancelItem) && !areSimilar(content, placeHolder))
                count++;
        }
        return count;
    }

    private static boolean areSimilarWithCount(ItemStack one, ItemStack other) {
        return areSimilar(one, other) && one.getAmount() == other.getAmount();
    }

    private static boolean areSimilar(ItemStack one, ItemStack other) {
        return one.getType() == other.getType() && (!one.hasItemMeta() || !other.hasItemMeta() || one.getItemMeta().getDisplayName().equals(other.getItemMeta().getDisplayName()));
    }

}
