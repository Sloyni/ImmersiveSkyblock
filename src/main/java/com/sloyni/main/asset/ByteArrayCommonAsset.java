package com.sloyni.main.asset;

import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import java.util.concurrent.CompletableFuture;

public class ByteArrayCommonAsset extends CommonAsset {
    private final byte[] data;

    public ByteArrayCommonAsset(String name, byte[] data) {
        super(name, data);
        this.data = data;
    }

    @Override
    protected CompletableFuture<byte[]> getBlob0() {
        return CompletableFuture.completedFuture(data);
    }
}