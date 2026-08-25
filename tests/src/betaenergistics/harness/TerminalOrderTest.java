package betaenergistics.harness;

import betaenergistics.storage.BE_ItemKey;
import betaenergistics.terminal.BE_TerminalOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Host contract for terminal ordering shared by all GUI containers. */
public final class TerminalOrderTest {
    public static void main(String[] arguments) {
        Entry wool = new Entry(35, 2, 9, "Wool");
        Entry apple = new Entry(260, 0, 4, "Apple");
        Entry stone = new Entry(1, 0, 9, "Stone");
        List<Entry> entries = new ArrayList<Entry>(Arrays.asList(wool, apple, stone));
        BE_TerminalOrder.NameResolver names = key -> {
            for (Entry entry : entries) if (entry.key.equals(key)) return entry.name;
            return "";
        };
        BE_TerminalOrder.sort(entries, BE_TerminalOrder.BY_ID, names);
        require(entries.equals(Arrays.asList(stone, wool, apple)), "ID order");
        BE_TerminalOrder.sort(entries, BE_TerminalOrder.BY_NAME, names);
        require(entries.equals(Arrays.asList(apple, stone, wool)), "name order");
        BE_TerminalOrder.sort(entries, BE_TerminalOrder.BY_QUANTITY, names);
        require(entries.equals(Arrays.asList(stone, wool, apple)), "quantity tie order");
        require(BE_TerminalOrder.normalize(-1) == BE_TerminalOrder.BY_QUANTITY,
                "negative mode normalization");
        System.out.println("TerminalOrderTest passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class Entry implements BE_TerminalOrder.Entry {
        final BE_ItemKey key;
        final int count;
        final String name;

        Entry(int id, int damage, int count, String name) {
            this.key = new BE_ItemKey(id, damage);
            this.count = count;
            this.name = name;
        }

        public BE_ItemKey key() { return key; }
        public int count() { return count; }
    }
}
