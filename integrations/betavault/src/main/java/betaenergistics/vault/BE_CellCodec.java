package betaenergistics.vault;

import betaenergistics.storage.BE_ItemKey;
import betavault.codec.VaultCodec;
import betavault.codec.VaultReader;
import betavault.codec.VaultWriter;
import betavault.core.SchemaId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Deterministic BetaVault schema owned by BetaEnergistics. */
public final class BE_CellCodec implements VaultCodec<BE_CellRecord> {
    public static final BE_CellCodec INSTANCE = new BE_CellCodec();
    private static final SchemaId SCHEMA = SchemaId.parse("betaenergistics.storage-cell/1");

    private BE_CellCodec() {}

    @Override public SchemaId schema() { return SCHEMA; }

    @Override public Class<BE_CellRecord> type() { return BE_CellRecord.class; }

    @Override public void encode(VaultWriter writer, BE_CellRecord value) {
        writer.writeInt(value.tier());
        writer.writeInt(value.capacity());
        writer.writeInt(value.contents().size());
        for (Map.Entry<BE_ItemKey, Integer> entry : value.contents().entrySet()) {
            writer.writeInt(entry.getKey().itemId);
            writer.writeInt(entry.getKey().damageValue);
            writer.writeInt(entry.getValue().intValue());
        }
    }

    @Override public BE_CellRecord decode(VaultReader reader) {
        int tier = reader.readInt();
        int capacity = reader.readInt();
        int size = reader.readInt();
        if (size < 0 || size > 63) throw new IllegalArgumentException("cell entry count");
        Map<BE_ItemKey, Integer> contents = new LinkedHashMap<BE_ItemKey, Integer>();
        for (int index = 0; index < size; index++) {
            BE_ItemKey key = new BE_ItemKey(reader.readInt(), reader.readInt());
            if (contents.put(key, Integer.valueOf(reader.readInt())) != null) {
                throw new IllegalArgumentException("duplicate encoded item key");
            }
        }
        return new BE_CellRecord(tier, capacity, contents);
    }
}
