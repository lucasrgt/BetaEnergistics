package betaenergistics.container;

import betaenergistics.crafting.BE_CraftingPlan;
import betaenergistics.mod_BetaEnergistics;
import betaenergistics.network.BE_PacketHandler;
import betaenergistics.storage.BE_ItemKey;
import betaenergistics.terminal.BE_TerminalOrder;
import betaenergistics.tile.BE_TileAutocrafter;
import betaenergistics.tile.BE_TileGrid;
import betaenergistics.tile.BE_TileTerminalBase;

import net.minecraft.src.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Container for the Grid Terminal.
 * No machine slots — just the player inventory + virtual network storage.
 * Items in the network are NOT real slots; they're rendered as a virtual grid.
 */
public class BE_ContainerGrid extends BE_ContainerTerminalBase {
    public static final int SORT_BY_ID = BE_TerminalOrder.BY_ID;
    public static final int SORT_BY_NAME = BE_TerminalOrder.BY_NAME;
    public static final int SORT_BY_QUANTITY = BE_TerminalOrder.BY_QUANTITY;
    public static final String[] SORT_NAMES = {"ID", "Name", "Qty"};

    // View modes
    public static final int VIEW_STORED = 0;
    public static final int VIEW_CRAFTABLE = 1;
    public static final int VIEW_PREVIEW = 2;

    // Request actions
    public static final int ACTION_SELECT = 0;
    public static final int ACTION_CONFIRM = 1;
    public static final int ACTION_CANCEL = 2;
    public static final int ACTION_INC_QTY = 3;
    public static final int ACTION_DEC_QTY = 4;

    private BE_TileGrid grid;
    private List<BE_GridEntry> cachedItems = new ArrayList<BE_GridEntry>();
    private int lastItemHash = 0;
    private int sortMode = SORT_BY_ID;
    private static final BE_TerminalOrder.NameResolver ITEM_NAMES =
            new BE_TerminalOrder.NameResolver() {
                public String name(BE_ItemKey key) { return getItemName(key); }
            };

    // Crafting request state
    private int viewMode = VIEW_STORED;
    private List<CraftableEntry> craftableItems = new ArrayList<CraftableEntry>();
    private BE_ItemKey selectedItem;
    private int requestQuantity = 1;
    private BE_CraftingPlan currentPlan;
    private List<PlanEntry> planEntries = new ArrayList<PlanEntry>();

    public BE_ContainerGrid(InventoryPlayer playerInv, BE_TileGrid grid) {
        this.grid = grid;
        addPlayerInventory(playerInv);
        refreshItems();
    }

    @Override
    protected BE_TileTerminalBase getTerminalTile() {
        return grid;
    }

    public int getSortMode() {
        return sortMode;
    }

    public void cycleSortMode() {
        sortMode = BE_TerminalOrder.normalize(sortMode + 1);
    }

    public void setSortMode(int mode) {
        sortMode = BE_TerminalOrder.normalize(mode);
    }

    /** Refresh the cached item list from the network. */
    public void refreshItems() {
        cachedItems.clear();
        Map<BE_ItemKey, Integer> networkItems = grid.getNetworkItems();
        if (networkItems != null) {
            for (Map.Entry<BE_ItemKey, Integer> entry : networkItems.entrySet()) {
                cachedItems.add(new BE_GridEntry(entry.getKey(), entry.getValue()));
            }
            applySortOrder();
        }
    }

    private static String getItemName(BE_ItemKey key) {
        Item item = Item.itemsList[key.itemId];
        if (item == null) return "";
        String name = item.getItemNameIS(new ItemStack(key.itemId, 1, key.damageValue));
        if (name == null) return "";
        return StringTranslate.getInstance().translateNamedKey(name);
    }

    @Override
    public void updateCraftingResults() {
        super.updateCraftingResults();
        refreshItems();
    }

    public List<BE_GridEntry> getItems() {
        return cachedItems;
    }

    public BE_TileGrid getGrid() {
        return grid;
    }

    /**
     * Handle click on a virtual grid cell (not a real slot).
     */
    public void handleGridClick(BE_ItemKey key, int button, boolean shiftHeld, EntityPlayer player) {
        if (mod_BetaEnergistics.isMultiplayer()) {
            BE_PacketHandler.sendToServer(
                BE_PacketHandler.buildGridClick(grid, key, button, shiftHeld));
            return;
        }
        ItemStack held = player.inventory.getItemStack();

        if (key == null && held != null) {
            BE_ItemKey insertKey = new BE_ItemKey(held.itemID, held.getItemDamage());
            int toInsert = (button == 1) ? 1 : held.stackSize;
            int inserted = grid.insertItem(insertKey, toInsert);
            if (inserted > 0) {
                held.stackSize -= inserted;
                if (held.stackSize <= 0) {
                    player.inventory.setItemStack(null);
                } else {
                    player.inventory.setItemStack(held);
                }
            }
            return;
        }

        if (key != null && held == null) {
            int maxExtract = (button == 1) ? 1 : 64;
            Item item = Item.itemsList[key.itemId];
            if (item != null) {
                maxExtract = Math.min(maxExtract, item.getItemStackLimit());
            }
            int extracted = grid.extractItem(key, maxExtract);
            if (extracted > 0) {
                ItemStack stack = new ItemStack(key.itemId, extracted, key.damageValue);
                player.inventory.setItemStack(stack);
            }
            return;
        }

        if (key != null && held != null) {
            BE_ItemKey heldKey = new BE_ItemKey(held.itemID, held.getItemDamage());
            if (heldKey.equals(key)) {
                Item item = Item.itemsList[key.itemId];
                int maxStack = (item != null) ? item.getItemStackLimit() : 64;
                int space = maxStack - held.stackSize;
                if (space > 0) {
                    int extracted = grid.extractItem(key, (button == 1) ? 1 : space);
                    if (extracted > 0) {
                        held.stackSize += extracted;
                        player.inventory.setItemStack(held);
                    }
                }
            } else {
                BE_ItemKey insertKey = new BE_ItemKey(held.itemID, held.getItemDamage());
                int inserted = grid.insertItem(insertKey, held.stackSize);
                if (inserted > 0) {
                    held.stackSize -= inserted;
                    if (held.stackSize <= 0) {
                        player.inventory.setItemStack(null);
                    }
                }
            }
        }
    }

    // ====== View mode + crafting request ======

    public int getViewMode() { return viewMode; }

    public void setViewMode(int mode) {
        if (mode == VIEW_STORED) {
            viewMode = VIEW_STORED;
            refreshItems();
        } else if (mode == VIEW_CRAFTABLE) {
            viewMode = VIEW_CRAFTABLE;
            refreshCraftableItems();
        }
    }

    public void refreshCraftableItems() {
        craftableItems.clear();
        Map<BE_ItemKey, List<BE_TileAutocrafter>> craftable = grid.getCraftableItems();
        if (craftable != null) {
            for (Map.Entry<BE_ItemKey, List<BE_TileAutocrafter>> entry : craftable.entrySet()) {
                craftableItems.add(new CraftableEntry(entry.getKey(), entry.getValue().size()));
            }
            Collections.sort(craftableItems, new Comparator<CraftableEntry>() {
                public int compare(CraftableEntry a, CraftableEntry b) {
                    int cmp = a.key.itemId - b.key.itemId;
                    return cmp != 0 ? cmp : a.key.damageValue - b.key.damageValue;
                }
            });
        }
    }

    public List<CraftableEntry> getCraftableItems() { return craftableItems; }
    public BE_ItemKey getSelectedItem() { return selectedItem; }
    public int getRequestQuantity() { return requestQuantity; }
    public BE_CraftingPlan getCurrentPlan() { return currentPlan; }
    public List<PlanEntry> getPlanEntries() { return planEntries; }

    public void sendRequestAction(int actionType, BE_ItemKey key, int quantity) {
        if (mod_BetaEnergistics.isMultiplayer()) {
            BE_PacketHandler.sendToServer(
                BE_PacketHandler.buildRequestAction(grid, actionType, key, quantity));
            return;
        }
        executeRequestAction(actionType, key, quantity);
    }

    public void executeRequestAction(int actionType, BE_ItemKey key, int quantity) {
        switch (actionType) {
            case ACTION_SELECT: selectItem(key, quantity); break;
            case ACTION_CONFIRM: confirmCraft(); break;
            case ACTION_CANCEL: cancelPreview(); break;
            case ACTION_INC_QTY: incrementQuantity(); break;
            case ACTION_DEC_QTY: decrementQuantity(); break;
        }
    }

    private void selectItem(BE_ItemKey key, int quantity) {
        this.selectedItem = key;
        this.requestQuantity = Math.max(1, quantity);
        recalculatePlan();
        this.viewMode = VIEW_PREVIEW;
    }

    private void recalculatePlan() {
        planEntries.clear();
        if (selectedItem == null) return;
        currentPlan = grid.calculatePlan(selectedItem, requestQuantity);
        if (currentPlan == null) return;
        for (Map.Entry<BE_ItemKey, Integer> entry : currentPlan.itemsToTake.entrySet()) {
            planEntries.add(new PlanEntry(entry.getKey(), entry.getValue(), PlanEntry.TYPE_TAKE));
        }
        for (Map.Entry<BE_ItemKey, Integer> entry : currentPlan.itemsToCraft.entrySet()) {
            planEntries.add(new PlanEntry(entry.getKey(), entry.getValue(), PlanEntry.TYPE_CRAFT));
        }
        for (Map.Entry<BE_ItemKey, Integer> entry : currentPlan.missing.entrySet()) {
            planEntries.add(new PlanEntry(entry.getKey(), entry.getValue(), PlanEntry.TYPE_MISSING));
        }
    }

    private boolean confirmCraft() {
        if (selectedItem == null || currentPlan == null || !currentPlan.isComplete()) return false;
        boolean result = grid.requestCraft(selectedItem, requestQuantity);
        if (result) cancelPreview();
        return result;
    }

    public void cancelPreview() {
        viewMode = VIEW_CRAFTABLE;
        selectedItem = null;
        currentPlan = null;
        planEntries.clear();
        requestQuantity = 1;
        refreshCraftableItems();
    }

    private void incrementQuantity() {
        requestQuantity = Math.min(requestQuantity + 1, 999);
        recalculatePlan();
    }

    private void decrementQuantity() {
        requestQuantity = Math.max(requestQuantity - 1, 1);
        recalculatePlan();
    }

    /** Entry for a craftable item. */
    public static class CraftableEntry {
        public final BE_ItemKey key;
        public final int crafterCount;
        public CraftableEntry(BE_ItemKey key, int crafterCount) {
            this.key = key;
            this.crafterCount = crafterCount;
        }
    }

    /** Entry for the crafting plan display. */
    public static class PlanEntry {
        public static final int TYPE_TAKE = 0;
        public static final int TYPE_CRAFT = 1;
        public static final int TYPE_MISSING = 2;
        public final BE_ItemKey key;
        public final int count;
        public final int type;
        public PlanEntry(BE_ItemKey key, int count, int type) {
            this.key = key;
            this.count = count;
            this.type = type;
        }
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        Slot slot = (Slot) this.slots.get(slotIndex);
        if (slot == null || !slot.getHasStack()) return null;

        ItemStack slotStack = slot.getStack();
        ItemStack result = slotStack.copy();

        BE_ItemKey key = new BE_ItemKey(slotStack.itemID, slotStack.getItemDamage());
        int inserted = grid.insertItem(key, slotStack.stackSize);
        if (inserted > 0) {
            slotStack.stackSize -= inserted;
            if (slotStack.stackSize <= 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
            return result;
        }
        return null;
    }

    /**
     * Receive network item data from a server packet (multiplayer client-side).
     */
    public void receiveNetworkItems(int[] data, int offset, int numEntries) {
        cachedItems.clear();
        for (int i = 0; i < numEntries; i++) {
            int idx = offset + i * 3;
            if (idx + 2 >= data.length) break;
            BE_ItemKey key = new BE_ItemKey(data[idx], data[idx + 1]);
            int count = data[idx + 2];
            cachedItems.add(new BE_GridEntry(key, count));
        }
        applySortOrder();
    }

    /** Apply the current sort mode to cached items. */
    private void applySortOrder() {
        BE_TerminalOrder.sort(cachedItems, sortMode, ITEM_NAMES);
    }

    /** Entry in the virtual grid: an item type + count. */
    public static class BE_GridEntry implements BE_TerminalOrder.Entry {
        public final BE_ItemKey key;
        public int count;

        public BE_GridEntry(BE_ItemKey key, int count) {
            this.key = key;
            this.count = count;
        }

        public BE_ItemKey key() { return key; }
        public int count() { return count; }
    }
}
