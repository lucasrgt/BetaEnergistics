package betaenergistics.terminal;

import betaenergistics.storage.BE_ItemKey;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Stable ordering shared by every item terminal. */
public final class BE_TerminalOrder {
    public static final int BY_ID = 0;
    public static final int BY_NAME = 1;
    public static final int BY_QUANTITY = 2;

    private BE_TerminalOrder() {}

    public static <T extends Entry> void sort(
            List<T> entries, int mode, final NameResolver names) {
        final int selected = normalize(mode);
        Collections.sort(entries, new Comparator<T>() {
            public int compare(T left, T right) {
                if (selected == BY_NAME) return compareNames(left, right, names);
                if (selected == BY_QUANTITY) return compareQuantities(left, right);
                return compareIds(left, right);
            }
        });
    }

    public static int normalize(int mode) {
        int normalized = mode % 3;
        return normalized < 0 ? normalized + 3 : normalized;
    }

    private static int compareNames(Entry left, Entry right, NameResolver names) {
        int compared = names.name(left.key()).compareToIgnoreCase(names.name(right.key()));
        return compared != 0 ? compared : left.key().itemId - right.key().itemId;
    }

    private static int compareQuantities(Entry left, Entry right) {
        int compared = right.count() - left.count();
        return compared != 0 ? compared : left.key().itemId - right.key().itemId;
    }

    private static int compareIds(Entry left, Entry right) {
        int compared = left.key().itemId - right.key().itemId;
        return compared != 0 ? compared : left.key().damageValue - right.key().damageValue;
    }

    public interface Entry {
        BE_ItemKey key();
        int count();
    }

    public interface NameResolver {
        String name(BE_ItemKey key);
    }
}
