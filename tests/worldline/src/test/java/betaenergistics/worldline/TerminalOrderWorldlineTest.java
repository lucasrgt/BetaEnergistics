package betaenergistics.worldline;

import betaenergistics.storage.BE_ItemKey;
import betaenergistics.terminal.BE_TerminalOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import worldline.test.WorldlineSpec;
import static worldline.test.Expect.expect;
import static worldline.test.Worldline.describe;
import static worldline.test.Worldline.test;

/** Public TestKit contract for deterministic terminal ordering. */
public final class TerminalOrderWorldlineTest extends WorldlineSpec {
    @Override protected void define() {
        describe("BetaEnergistics terminal ordering", () -> {
            test("orders equal quantities by stable item ID", context -> {
                List<Entry> entries = new ArrayList<Entry>(Arrays.asList(
                        new Entry(35, 12), new Entry(1, 12), new Entry(260, 3)));
                BE_TerminalOrder.sort(entries, BE_TerminalOrder.BY_QUANTITY, key -> "");
                expect(entries.get(0).key().itemId).toEqual(1);
                expect(entries.get(1).key().itemId).toEqual(35);
                expect(entries.get(2).key().itemId).toEqual(260);
            }).tag("terminal");
            test("normalizes sort modes", context -> {
                expect(BE_TerminalOrder.normalize(4)).toEqual(BE_TerminalOrder.BY_NAME);
                expect(BE_TerminalOrder.normalize(-1)).toEqual(BE_TerminalOrder.BY_QUANTITY);
            }).tag("invariant");
        });
    }

    private static final class Entry implements BE_TerminalOrder.Entry {
        private final BE_ItemKey key;
        private final int count;

        Entry(int id, int count) {
            this.key = new BE_ItemKey(id);
            this.count = count;
        }

        public BE_ItemKey key() { return key; }
        public int count() { return count; }
    }
}
