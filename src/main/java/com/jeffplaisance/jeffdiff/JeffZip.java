// Copyright 2010 Jeff Plaisance
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.

package com.jeffplaisance.jeffdiff;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author jplaisance
 */

public final class JeffZip {
    static {
        System.loadLibrary("jeffbinary");
    }

    private static final int blockSize = 6;

    private static void findMatch(final byte[] data, final int dataOffset, final int dataLength, final int frameStart, final int oldIndex, final int newIndex, final IntRef nextMatchStart, final IntRef nextMatchLength) {
        int start = 0;
        int end = blockSize;
        for (int i = 0; i < end; i++) {
            if (data[oldIndex+i] != data[newIndex+i]) {
                nextMatchStart.elem = 0;
                nextMatchLength.elem = 0;
                return;
            }
        }
        while(oldIndex+start > dataOffset && newIndex+start > frameStart && data[oldIndex+start-1] == data[newIndex+start-1]) start--;
        final int length = dataOffset+dataLength;
        while(newIndex+end < length && data[oldIndex+end] == data[newIndex+end]) end++;
        nextMatchStart.elem = start;
        nextMatchLength.elem = end-start;
    }

    private static int copy(final byte[] data, final byte[] out, int outPtr, final int from, final int to) {
        final int length = to-from;
        final int oldOutPtr = outPtr;
        if (length > 0) {
            outPtr+=VIntUtils.writeVSInt(out, outPtr, length);
            System.arraycopy(data, from, out, outPtr, length);
            outPtr+=length;
        }
        return outPtr-oldOutPtr;
    }

    private static final int HASH_SIZE = 32768;
    private static final int HASH_MASK = HASH_SIZE-1;

    public static int compress(final byte[] data, final int dataOffset, final int dataLength, final byte[] out, final int outOffset) {
        int outPtr = outOffset;
        outPtr+=VIntUtils.writeBase128Int(out, outOffset, dataLength);
        final IntRef matchStart = new IntRef(0);
        final IntRef matchLength = new IntRef(0);
        final long[] hashTable = new long[HASH_SIZE];
        Arrays.fill(hashTable, -1);
        final RollingHash hash = new RollingHash(blockSize);
        final int end = dataOffset+dataLength-blockSize;
        int frameIndex = dataOffset;
outer:  while (true) {
            if (frameIndex < end) {
                int index = frameIndex;
                hash.setup(data, frameIndex);
                while(true) {
                    final int hashIndex = hash.getHash()&HASH_MASK;
                    final long old = hashTable[hashIndex];
                    final int oldIndex = (int) old;
                    final int oldHash = (int)(old>>32);
                    hashTable[hashIndex] = (long)hash.getHash()<<32|index;
                    if (oldIndex >= 0 && oldHash == hash.getHash()) {
                        findMatch(data, dataOffset, dataLength, frameIndex, oldIndex, index, matchStart, matchLength);
                        if (matchLength.elem > 0) {
                            final int backtrack = index-oldIndex;
                            outPtr+=copy(data, out, outPtr, frameIndex, index+matchStart.elem);
                            outPtr+=VIntUtils.writeVSInt(out, outPtr, -(((matchLength.elem-blockSize)<<3)|(backtrack&0x7)));
                            outPtr+=VIntUtils.writeBase128Int(out, outPtr, backtrack>>3);
                            frameIndex = index+matchStart.elem+matchLength.elem;
                            continue outer;
                        }
                    }
                    if (index < end) {
                        hash.roll(data[index+blockSize]);
                        index++;
                        continue;
                    }
                    break;
                }
            }
            outPtr+=copy(data, out, outPtr, frameIndex, dataOffset+dataLength);
            return outPtr-outOffset;
        }
    }

    public static void decompress(final byte[] data, final int dataOffset, final byte[] out, final int outOffset) {
        int outPtr = outOffset;
        int dataPtr = dataOffset;
        final IntRef count = new IntRef(0);
        final int uncompressedLength = VIntUtils.parseBase128Int(data, dataOffset, count);
        dataPtr+=count.elem;
        while (outPtr < outOffset+uncompressedLength) {
            final int first = VIntUtils.parseVSInt(data, dataPtr, count);
            dataPtr+=count.elem;
            if (first <= 0) {
                final int second = VIntUtils.parseBase128Int(data, dataPtr, count);
                dataPtr+=count.elem;
                final int posFirst = -first;
                final int length = (posFirst>>3)+blockSize;
                final int backtrack = (second<<3)|(posFirst&0x7);
                final int index = outPtr-backtrack;
                if (index+length > outPtr) {
                    for (int i = 0; i < length; i++) {
                        out[outPtr+i] = out[index+i];
                    }
                } else {
                    System.arraycopy(out, index, out, outPtr, length);
                }
                outPtr+=length;
            } else {
                System.arraycopy(data, dataPtr, out, outPtr, first);
                dataPtr+=first;
                outPtr+=first;
            }
        }
    }

    public static native int compress(ByteBuffer data, long dataOffset, int dataLength, ByteBuffer out, long outOffset);

    public static native int decompress(ByteBuffer data, long dataOffset, ByteBuffer out, long outOffset);
}
