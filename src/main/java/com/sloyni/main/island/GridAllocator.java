package com.sloyni.main.island;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GridAllocator {
    public static class GridPos {
        public final int x;
        public final int z;

        public GridPos(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    private final Set<Long> occupied = new HashSet<>();
    private int nextIndex = 0;

    public void bootstrap(Collection<IslandData> islands) {
        occupied.clear();
        int max = 0;
        for (IslandData island : islands) {
            occupy(island.getGridX(), island.getGridZ());
            int idx = estimateIndex(island.getGridX(), island.getGridZ());
            if (idx >= max) {
                max = idx + 1;
            }
        }
        nextIndex = max;
    }

    public GridPos allocate() {
        GridPos pos;
        do {
            pos = spiral(nextIndex);
            nextIndex++;
        } while (isOccupied(pos.x, pos.z));
        occupy(pos.x, pos.z);
        return pos;
    }

    private GridPos spiral(int index) {
        if (index == 0) {
            return new GridPos(0, 0);
        }

        int x = 0;
        int z = 0;
        int dx = 1;
        int dz = 0;
        int segmentLength = 1;
        int segmentPassed = 0;
        int turnCount = 0;

        for (int i = 0; i < index; i++) {
            x += dx;
            z += dz;
            segmentPassed++;
            if (segmentPassed == segmentLength) {
                segmentPassed = 0;
                int oldDx = dx;
                dx = -dz;
                dz = oldDx;
                turnCount++;
                if (turnCount % 2 == 0) {
                    segmentLength++;
                }
            }
        }

        return new GridPos(x, z);
    }

    private int estimateIndex(int gx, int gz) {
        if (gx == 0 && gz == 0) {
            return 0;
        }

        int i = 0;
        while (i < 200000) {
            GridPos pos = spiral(i);
            if (pos.x == gx && pos.z == gz) {
                return i;
            }
            i++;
        }
        return 0;
    }

    private long pack(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    public boolean isOccupied(int x, int z) {
        return occupied.contains(pack(x, z));
    }

    public void occupy(int x, int z) {
        occupied.add(pack(x, z));
    }

    public void release(int x, int z) {
        occupied.remove(pack(x, z));
    }
}
