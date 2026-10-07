package com.sloyni.main.island;

import java.util.ArrayList;
import java.util.List;

public class IslandSchematic {
    public static final String BEDROCK_BLOCK_ID = "Rock_Bedrock";

    private String id;
    private int width;
    private int height;
    private int length;
    private final List<BlockData> blocks = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public List<BlockData> getBlocks() {
        return blocks;
    }

    public boolean hasBedrockBlock() {
        for (BlockData block : blocks) {
            if (isBedrockId(block.getBlockId())) {
                return true;
            }
        }
        return false;
    }

    public BlockData findFirstBedrockBlock() {
        for (BlockData block : blocks) {
            if (isBedrockId(block.getBlockId())) {
                return block;
            }
        }
        return null;
    }

    public static boolean isBedrockId(String blockId) {
        return blockId != null && BEDROCK_BLOCK_ID.equalsIgnoreCase(blockId);
    }

    public static class BlockData {
        private int x;
        private int y;
        private int z;
        private String blockId;
        private int rotation;
        private int filler;
        private String fluidId;
        private int fluidLevel;
        private int settings;
        private String blockEntityData;

        public BlockData() {
        }

        public BlockData(int x, int y, int z, String blockId, int rotation, int filler) {
            this(x, y, z, blockId, rotation, filler, null, 0, 0, null);
        }

        public BlockData(int x, int y, int z, String blockId, int rotation, int filler,
                         String fluidId, int fluidLevel, int settings) {
            this(x, y, z, blockId, rotation, filler, fluidId, fluidLevel, settings, null);
        }

        public BlockData(int x, int y, int z, String blockId, int rotation, int filler,
                         String fluidId, int fluidLevel, int settings,
                         String blockEntityData) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
            this.rotation = rotation;
            this.filler = filler;
            this.fluidId = fluidId;
            this.fluidLevel = fluidLevel;
            this.settings = settings;
            this.blockEntityData = blockEntityData;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getZ() {
            return z;
        }

        public void setZ(int z) {
            this.z = z;
        }

        public String getBlockId() {
            return blockId;
        }

        public void setBlockId(String blockId) {
            this.blockId = blockId;
        }

        public int getRotation() {
            return rotation;
        }

        public void setRotation(int rotation) {
            this.rotation = rotation;
        }

        public int getFiller() {
            return filler;
        }

        public void setFiller(int filler) {
            this.filler = filler;
        }

        public String getFluidId() {
            return fluidId;
        }

        public void setFluidId(String fluidId) {
            this.fluidId = fluidId;
        }

        public int getFluidLevel() {
            return fluidLevel;
        }

        public void setFluidLevel(int fluidLevel) {
            this.fluidLevel = fluidLevel;
        }

        public int getSettings() {
            return settings;
        }

        public void setSettings(int settings) {
            this.settings = settings;
        }

        public String getBlockEntityData() {
            return blockEntityData;
        }

        public void setBlockEntityData(String blockEntityData) {
            this.blockEntityData = blockEntityData;
        }
    }
}
