package betaenergistics.harness;

import betaenergistics.crafting.BE_CraftingPlan;
import betaenergistics.storage.BE_AccessMode;
import betaenergistics.storage.BE_ItemKey;

/** Dependency-free regression checks for stable domain primitives. */
public final class CoreContractTest {
    public static void main(String[] arguments) {
        BE_ItemKey stone = new BE_ItemKey(1, 0);
        BE_ItemKey copy = new BE_ItemKey(1);
        require(stone.equals(copy), "equal item keys");
        require(stone.hashCode() == copy.hashCode(), "equal key hashes");
        require(!stone.equals(new BE_ItemKey(1, 1)), "damage distinguishes keys");
        require(BE_AccessMode.INSERT_ONLY.allowsInsert(), "insert-only insertion");
        require(!BE_AccessMode.INSERT_ONLY.allowsExtract(), "insert-only extraction");

        BE_CraftingPlan first = new BE_CraftingPlan();
        first.addToTake(stone, 3);
        BE_CraftingPlan second = new BE_CraftingPlan();
        second.addToTake(copy, 2);
        second.addMissing(new BE_ItemKey(4), 1);
        first.merge(second);
        require(first.itemsToTake.get(stone).intValue() == 5, "plan conservation");
        require(!first.isComplete(), "missing input visibility");
        System.out.println("  core contracts: item identity, access mode, crafting conservation");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
