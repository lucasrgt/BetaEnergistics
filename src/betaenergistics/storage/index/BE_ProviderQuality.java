package betaenergistics.storage.index;

/** How precisely a storage provider reports changes. */
public enum BE_ProviderQuality {
    PUSH,
    DIRTY_NOTIFY,
    POLL
}
