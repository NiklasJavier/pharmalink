package de.jklein.pharmalink.domain;

import java.time.Instant;

public abstract class TrackableAsset {

    protected final String assetId;
    protected String owner;
    protected final Instant createdAt;

    protected TrackableAsset(String assetId, String owner) {
        this.assetId = assetId;
        this.owner = owner;
        this.createdAt = Instant.now();
    }

    public abstract String getAssetType();

    public String getAssetId() { return assetId; }
    public String getOwner() { return owner; }
}