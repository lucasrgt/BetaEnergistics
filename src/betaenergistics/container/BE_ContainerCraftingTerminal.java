package betaenergistics.container;

import betaenergistics.crafting.BE_CraftingCalculator;
import betaenergistics.mod_BetaEnergistics;
import betaenergistics.network.BE_PacketHandler;
import betaenergistics.network.BE_StorageNetwork;
import betaenergistics.storage.BE_ItemKey;
import betaenergistics.terminal.BE_TerminalOrder;
import betaenergistics.tile.BE_TileAutocrafter;
import betaenergistics.tile.BE_TileCraftingTerminal;

import net.minecraft.src.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Container for the Crafting Terminal.
 * Combines the Grid Terminal's virtual network grid with a 3x3 crafting matrix + result slot.
 * Supports auto-fill from network, auto-refill after craft, and craft-to-network.
 *
 * Slot layout:
 *   0-8:   Crafting matrix (from TileCraftingTerminal IInventory)
 *   9:     Craft result
 *   10-36: Player inventory
 *   37-45: Hotbar
 */
public class BE_ContainerCraftingTerminal extends Container {
    public static final int SORT_BY_ID = BE_TerminalOrder.BY_ID;
    public static final int SORT_BY_NAME = BE_TerminalOrder.BY_NAME;
    public static final int SORT_BY_QUANTITY = BE_TerminalOrder.BY_QUANTITY;
    public static final String[] SORT_NAMES = {"ID", "Name", "Qty"};

    private static final int RESULT_SLOT = 9;
    private static final int PLAYER_INV_START = 10;
    private static final int PLAYER_INV_END = 37;
    private static final int HOTBAR_START = 37;
    private static final int HOTBAR_END = 46;

    private BE_TileCraftingTerminal tile;
    private IInventory craftResult = new InventoryCraftResult();
    private List<BE_GridEntry> cachedItems = new ArrayList<BE_GridEntry>();
    private int sortMode = SORT_BY_ID;
    private static final BE_TerminalOrder.NameResolver ITEM_NAMES =
            new BE_TerminalOrder.NameResolver() {
                public String name(BE_ItemKey key) { return getItemName(key); }
            };

    /** Tracks what item should be in each craft slot for auto-refill. */
    private BE_ItemKey[] recipeTemplate = new BE_ItemKey[9];
    private String statusText = "";

    public BE_ContainerCraftingTerminal(InventoryPlayer playerInv, BE_TileCraftingTerminal tile) {
        this.tile = tile;

        // Craft matrix slots (0-8)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(tile, col + row * 3, 12 + col * 18, 96 + row * 18));
            }
        }

        // Craft result slot (9)
        this.addSlot(new SlotCraftTerminalResult(playerInv.player, tile, craftResult, 0, 94, 114));

        // Player inventory (10-36)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 158 + row * 18));
            }
        }

        // Hotbar (37-45)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 216));
        }

        // Initialize recipe template from current contents
        for (int i = 0; i < 9; i++) {
            ItemStack s = tile.getStackInSlot(i);
            if (s != null) {
                recipeTemplate[i] = new BE_ItemKey(s.itemID, s.getItemDamage());
            }
        }

        updateCraftResult();
        refreshItems();
    }

    /** Update the crafting result based on current matrix contents. */
    public void updateCraftResult() {
        InventoryCrafting temp = new InventoryCrafting(new Container() {
            public boolean isUsableByPlayer(EntityPlayer p) { return false; }
        }, 3, 3);
        for (int i = 0; i < 9; i++) {
            temp.setInventorySlotContents(i, tile.getStackInSlot(i));
        }
        ItemStack result = CraftingManager.getInstance().findMatchingRecipe(temp);
        craftResult.setInventorySlotContents(0, result);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inv) {
        updateCraftResult();
    }

    @Override
    public void updateCraftingResults() {
        super.updateCraftingResults();
        updateCraftResult();
        refreshItems();
        // Update recipe template for any occupied slot
        for (int i = 0; i < 9; i++) {
            ItemStack s = tile.getStackInSlot(i);
            if (s != null) {
                recipeTemplate[i] = new BE_ItemKey(s.itemID, s.getItemDamage());
            }
        }
    }

    // Action type constants for multiplayer packets
    public static final int ACTION_FILL = 0;
    public static final int ACTION_CLEAR = 1;
    public static final int ACTION_CRAFT_TO_NET = 2;
    public static final int ACTION_REQUEST_MISSING = 3;

    /** Send a crafting action to the server (multiplayer) or execute locally (singleplayer). */
    public void sendAction(int actionType) {
        if (mod_BetaEnergistics.isMultiplayer()) {
            BE_PacketHandler.sendToServer(BE_PacketHandler.buildCraftingAction(tile, actionType));
            return;
        }
        executeAction(actionType);
    }

    /** Execute a crafting action locally (called on server in multiplayer, or directly in singleplayer). */
    public void executeAction(int actionType) {
        switch (actionType) {
            case ACTION_FILL: fillFromNetwork(); break;
            case ACTION_CLEAR: clearToNetwork(); break;
            case ACTION_CRAFT_TO_NET: craftToNetwork(); break;
            case ACTION_REQUEST_MISSING: requestMissingCrafts(); break;
        }
    }

    /** Fill empty crafting slots from network storage based on the recipe template. */
    public void fillFromNetwork() {
        statusText = "";
        List<String> missing = new ArrayList<String>();

        for (int i = 0; i < 9; i++) {
            if (recipeTemplate[i] == null) continue;
            ItemStack current = tile.getStackInSlot(i);
            if (current != null && current.stackSize > 0) continue;

            int extracted = tile.extractItem(recipeTemplate[i], 1);
            if (extracted > 0) {
                tile.setInventorySlotContents(i, new ItemStack(recipeTemplate[i].itemId, 1, recipeTemplate[i].damageValue));
            } else {
                Item item = Item.itemsList[recipeTemplate[i].itemId];
                String name = item != null ? StringTranslate.getInstance().translateNamedKey(
                    item.getItemNameIS(new ItemStack(recipeTemplate[i].itemId, 1, recipeTemplate[i].damageValue))) : "?";
                boolean craftable = isCraftable(recipeTemplate[i]);
                missing.add(name + (craftable ? " [C]" : ""));
            }
        }

        if (!missing.isEmpty()) {
            StringBuilder sb = new StringBuilder("Need: ");
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(missing.get(i));
            }
            statusText = sb.toString();
        }

        updateCraftResult();
    }

    /** Check if an item is craftable via network autocrafters. */
    private boolean isCraftable(BE_ItemKey key) {
        BE_StorageNetwork network = tile.getNetwork();
        if (network == null) return false;
        BE_CraftingCalculator calc = new BE_CraftingCalculator(network);
        Map<BE_ItemKey, List<BE_TileAutocrafter>> craftable = calc.getCraftableItems();
        return craftable.containsKey(key);
    }

    /** Clear crafting grid and return items to network. */
    public void clearToNetwork() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = tile.getStackInSlot(i);
            if (stack != null) {
                BE_ItemKey key = new BE_ItemKey(stack.itemID, stack.getItemDamage());
                int inserted = tile.insertItem(key, stack.stackSize);
                if (inserted >= stack.stackSize) {
                    tile.setInventorySlotContents(i, null);
                } else if (inserted > 0) {
                    stack.stackSize -= inserted;
                }
            }
        }
        updateCraftResult();
        statusText = "";
    }

    /** Craft one item and send result directly to network. Returns true if successful. */
    public boolean craftToNetwork() {
        ItemStack result = craftResult.getStackInSlot(0);
        if (result == null) return false;

        BE_ItemKey key = new BE_ItemKey(result.itemID, result.getItemDamage());
        int inserted = tile.insertItem(key, result.stackSize);
        if (inserted <= 0) return false;

        // Remember items before consumption for auto-refill
        int[] prevIds = new int[9];
        int[] prevDmg = new int[9];
        for (int i = 0; i < 9; i++) {
            ItemStack s = tile.getStackInSlot(i);
            if (s != null) {
                prevIds[i] = s.itemID;
                prevDmg[i] = s.getItemDamage();
            }
        }

        // Consume ingredients
        for (int i = 0; i < 9; i++) {
            ItemStack stack = tile.getStackInSlot(i);
            if (stack != null) {
                if (stack.getItem() != null && stack.getItem().hasContainerItem()) {
                    ItemStack container = new ItemStack(stack.getItem().getContainerItem());
                    tile.decrStackSize(i, 1);
                    tile.setInventorySlotContents(i, container);
                } else {
                    tile.decrStackSize(i, 1);
                }
            }
        }

        autoRefillFromNetwork(prevIds, prevDmg);
        updateCraftResult();
        return true;
    }

    /** Request crafts for missing craftable items in the recipe template. */
    public boolean requestMissingCrafts() {
        BE_StorageNetwork network = tile.getNetwork();
        if (network == null) return false;

        BE_CraftingCalculator calc = new BE_CraftingCalculator(network);
        Map<BE_ItemKey, List<BE_TileAutocrafter>> craftable = calc.getCraftableItems();
        boolean requested = false;

        for (int i = 0; i < 9; i++) {
            if (recipeTemplate[i] == null) continue;
            ItemStack current = tile.getStackInSlot(i);
            if (current != null) continue;

            List<BE_TileAutocrafter> crafters = craftable.get(recipeTemplate[i]);
            if (crafters != null && !crafters.isEmpty()) {
                BE_TileAutocrafter crafter = crafters.get(0);
                int slot = crafter.findPattern(recipeTemplate[i]);
                if (slot >= 0) {
                    crafter.requestCraft(slot);
                    requested = true;
                }
            }
        }

        if (requested) {
            statusText = "Craft requested!";
        }
        return requested;
    }

    /** Auto-refill consumed crafting slots from network. */
    public void autoRefillFromNetwork(int[] prevIds, int[] prevDmg) {
        for (int i = 0; i < 9; i++) {
            if (prevIds[i] > 0 && tile.getStackInSlot(i) == null) {
                BE_ItemKey key = new BE_ItemKey(prevIds[i], prevDmg[i]);
                int extracted = tile.extractItem(key, 1);
                if (extracted > 0) {
                    tile.setInventorySlotContents(i, new ItemStack(prevIds[i], 1, prevDmg[i]));
                }
            }
        }
    }

    // === Virtual grid methods (same as ContainerGrid) ===

    public int getSortMode() { return sortMode; }

    public void cycleSortMode() { sortMode = BE_TerminalOrder.normalize(sortMode + 1); }

    public void refreshItems() {
        cachedItems.clear();
        Map<BE_ItemKey, Integer> networkItems = tile.getNetworkItems();
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

    public List<BE_GridEntry> getItems() { return cachedItems; }

    public BE_TileCraftingTerminal getTile() { return tile; }

    public String getStatusText() { return statusText; }

    public void handleGridClick(BE_ItemKey key, int button, boolean shiftHeld, EntityPlayer player) {
        // In multiplayer, send packet to server
        if (mod_BetaEnergistics.isMultiplayer()) {
            BE_PacketHandler.sendToServer(
                BE_PacketHandler.buildGridClick(tile, key, button, shiftHeld));
            return;
        }

        ItemStack held = player.inventory.getItemStack();

        if (key == null && held != null) {
            BE_ItemKey insertKey = new BE_ItemKey(held.itemID, held.getItemDamage());
            int toInsert = (button == 1) ? 1 : held.stackSize;
            int inserted = tile.insertItem(insertKey, toInsert);
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
            if (item != null) maxExtract = Math.min(maxExtract, item.getItemStackLimit());
            int extracted = tile.extractItem(key, maxExtract);
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
                    int extracted = tile.extractItem(key, (button == 1) ? 1 : space);
                    if (extracted > 0) {
                        held.stackSize += extracted;
                        player.inventory.setItemStack(held);
                    }
                }
            } else {
                BE_ItemKey insertKey = new BE_ItemKey(held.itemID, held.getItemDamage());
                int inserted = tile.insertItem(insertKey, held.stackSize);
                if (inserted > 0) {
                    held.stackSize -= inserted;
                    if (held.stackSize <= 0) {
                        player.inventory.setItemStack(null);
                    }
                }
            }
        }
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return tile.worldObj.getBlockTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile
            && player.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) <= 64.0;
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        Slot slot = (Slot) this.slots.get(slotIndex);
        if (slot == null || !slot.getHasStack()) return null;

        ItemStack slotStack = slot.getStack();
        ItemStack result = slotStack.copy();

        if (slotIndex == RESULT_SLOT) {
            // Shift-click result: insert into network, fallback to player inv
            BE_ItemKey key = new BE_ItemKey(slotStack.itemID, slotStack.getItemDamage());
            int inserted = tile.insertItem(key, slotStack.stackSize);
            if (inserted > 0) {
                slotStack.stackSize -= inserted;
            }
            if (slotStack.stackSize > 0) {
                this.func_28125_a(slotStack, PLAYER_INV_START, HOTBAR_END, true);
            }
        } else if (slotIndex >= 0 && slotIndex < 9) {
            // Shift-click craft matrix: move to player inventory
            this.func_28125_a(slotStack, PLAYER_INV_START, HOTBAR_END, true);
        } else if (slotIndex >= PLAYER_INV_START && slotIndex < PLAYER_INV_END) {
            // Shift-click player inv: try network, then hotbar
            BE_ItemKey key = new BE_ItemKey(slotStack.itemID, slotStack.getItemDamage());
            int inserted = tile.insertItem(key, slotStack.stackSize);
            if (inserted > 0) {
                slotStack.stackSize -= inserted;
            }
            if (slotStack.stackSize > 0) {
                this.func_28125_a(slotStack, HOTBAR_START, HOTBAR_END, false);
            }
        } else if (slotIndex >= HOTBAR_START && slotIndex < HOTBAR_END) {
            // Shift-click hotbar: try network, then player inv
            BE_ItemKey key = new BE_ItemKey(slotStack.itemID, slotStack.getItemDamage());
            int inserted = tile.insertItem(key, slotStack.stackSize);
            if (inserted > 0) {
                slotStack.stackSize -= inserted;
            }
            if (slotStack.stackSize > 0) {
                this.func_28125_a(slotStack, PLAYER_INV_START, PLAYER_INV_END, false);
            }
        }

        if (slotStack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }

        if (slotStack.stackSize == result.stackSize) return null;

        slot.onPickupFromSlot(slotStack);
        return result;
    }

    @Override
    public void onCraftGuiClosed(EntityPlayer player) {
        super.onCraftGuiClosed(player);
        // Don't drop craft matrix items — they're persisted in the tile entity
    }

    /** Custom result slot that auto-refills crafting grid from network after pickup. */
    private class SlotCraftTerminalResult extends SlotCrafting {
        public SlotCraftTerminalResult(EntityPlayer player, IInventory craftMatrix,
                                       IInventory result, int index, int x, int y) {
            super(player, craftMatrix, result, index, x, y);
        }

        @Override
        public void onPickupFromSlot(ItemStack stack) {
            // Remember what was in each slot before consumption
            int[] prevIds = new int[9];
            int[] prevDmg = new int[9];
            for (int i = 0; i < 9; i++) {
                ItemStack s = tile.getStackInSlot(i);
                if (s != null) {
                    prevIds[i] = s.itemID;
                    prevDmg[i] = s.getItemDamage();
                }
            }

            super.onPickupFromSlot(stack);

            // Auto-refill from network
            autoRefillFromNetwork(prevIds, prevDmg);
            updateCraftResult();
        }
    }

    // ====== Multiplayer receive methods ======

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

    /** Receive status text from server packet. */
    public void receiveStatusText(String text) {
        this.statusText = text;
    }

    /** Apply the current sort mode to cached items. */
    private void applySortOrder() {
        BE_TerminalOrder.sort(cachedItems, sortMode, ITEM_NAMES);
    }

    /** Entry in the virtual grid. */
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
