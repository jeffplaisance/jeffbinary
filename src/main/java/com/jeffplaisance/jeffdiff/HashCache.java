package com.jeffplaisance.jeffdiff;

import java.util.Arrays;

/**
 * @author jplaisance
 */
public final class HashCache {
    private final long[] hashTable;
    private final long[] entries;
    private final int hashMask;
    private final int entriesSize;
    private final int entriesMask;
    private int top = 0;

    public HashCache(final int buckets, final int maxEntries) {
        final int hashSize = Integer.highestOneBit(buckets-1)<<1;
        hashMask = hashSize-1;
        hashTable = new long[hashSize];
        Arrays.fill(hashTable, -1);
        entriesSize = Integer.highestOneBit(maxEntries-1)<<1;
        entriesMask = entriesSize-1;
        entries = new long[entriesSize];
    }

    public long get(final int hash) {
        return hashTable[hash&hashMask];
    }

    public long getNext(final long entry) {
        final int addr = (int)(entry>>>32);
        if (addr < 0L || addr < top-entriesSize) return -1L;
        return entries[addr&entriesMask];
    }

    public void add(final int hash, final int index) {
        final int hashIndex = hash&hashMask;
        final long current = hashTable[hashIndex];
        entries[top&entriesMask] = current;
        hashTable[hashIndex] = (((long)top)<<32)|index;
        top++;
    }
}
