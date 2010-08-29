package com.jeffplaisance.jeffdiff;

import org.jboss.netty.buffer.ChannelBuffer;

/**
* @author jplaisance
*/
public final class JeffDiff {
    private static HashCache makeTable(final ChannelBuffer buffer, final int b) {
        final int hashes = buffer.readableBytes()/b;
        final HashCache hashTable = new HashCache(hashes, hashes);
        final int start = buffer.readerIndex();
        final RollingHash rollingHash = new RollingHash(b);
        while(buffer.readableBytes() >= b) {
            final int index = buffer.readerIndex();
            rollingHash.setup(buffer, index);
            buffer.skipBytes(b);
            hashTable.add(rollingHash.getHash(), index);
        }
        buffer.readerIndex(start);
        return hashTable;
    }

    private static void findMatch(final ChannelBuffer oldBytes, final int frameStart, final int oldIndex, final ChannelBuffer newBytes, final int newIndex, final IntRef nextMatchStart, final IntRef nextMatchLength, final int b) {
        int start = 0;
        int end = b;
        for (int i = start; i < end; i++) {
            if (oldBytes.getByte(oldIndex+i) != newBytes.getByte(newIndex+i)) {
                nextMatchStart.elem = 0;
                nextMatchLength.elem = 0;
                return;
            }
        }
        final int oldZero = oldBytes.readerIndex();
        while(oldIndex+start > oldZero && newIndex+start > frameStart && oldBytes.getByte(oldIndex+start-1) == newBytes.getByte(newIndex+start-1)) start -=1;
        final int oldLength = oldBytes.writerIndex();
        final int newLength = newBytes.writerIndex();
        while(oldIndex+end < oldLength && newIndex+end < newLength && oldBytes.getByte(oldIndex+end) == newBytes.getByte(newIndex+end)) end+=1;
        nextMatchStart.elem = start;
        nextMatchLength.elem = end-start;
    }

    private static void copy(final ChannelBuffer newBytes, final ChannelBuffer out, final int from, final int to) {
        final int length = to-from;
        if (length > 0) {
            out.writeByte(0);
            VIntUtils.writeBase128Int(out, length);
            out.writeBytes(newBytes, from, length);
        }
    }

    public static void diff(final ChannelBuffer oldBytes, final ChannelBuffer newBytes, final ChannelBuffer out, final int b) {
        final IntRef nextMatchStart = new IntRef(0);
        final IntRef nextMatchLength = new IntRef(0);
        final HashCache hashTable = makeTable(oldBytes, b);
        int frameStart = newBytes.readerIndex();
        final int end = newBytes.writerIndex()-b;
        final RollingHash hash = new RollingHash(b);
outer:  while(true) {
            if (frameStart < end) {
                hash.setup(newBytes, frameStart);
                int index = frameStart;
                while (true) {
                    int bestMatchOldIndex = -1;
                    int bestMatchStart = 0;
                    int bestMatchLength = 0;
                    long it = hashTable.get(hash.getHash());
                    int oldIndex = (int)it;
                    while (oldIndex > 0) {
                        findMatch(oldBytes, frameStart, oldIndex, newBytes, index, nextMatchStart, nextMatchLength, b);
                        if (nextMatchLength.elem > bestMatchLength) {
                            bestMatchStart = nextMatchStart.elem;
                            bestMatchLength = nextMatchLength.elem;
                            bestMatchOldIndex = oldIndex;
                        }
                        it = hashTable.getNext(it);
                        oldIndex = (int)it;
                    }
                    if (bestMatchLength > 0) {
                        copy(newBytes, out, frameStart, index+bestMatchStart);
                        out.writeByte(1);
                        out.writeInt(bestMatchOldIndex+bestMatchStart);
                        out.writeInt(bestMatchLength);
                        frameStart = index+bestMatchStart+bestMatchLength;
                        continue outer;
                    }
                    if (index < end) {
                        hash.roll(newBytes.getByte(index+b));
                        index+=1;
                        continue;
                    }
                    break;
                }
            }

            copy(newBytes, out, frameStart, newBytes.writerIndex());
            return;
        }
    }

    public static void undiff(final ChannelBuffer oldBytes, final ChannelBuffer diff, final ChannelBuffer out) {
        while (diff.readableBytes() > 0) {
            final byte b = diff.readByte();
            if (b == 0) {
                final int length = VIntUtils.parseBase128Int(diff);
                out.writeBytes(diff, length);
            } else {
                final int start = diff.readInt();
                final int length = diff.readInt();
                out.writeBytes(oldBytes, start, length);
            }
        }
    }
    
    private static HashCache makeTable(final byte[] buffer, final int b) {
        final int hashes = buffer.length/b;
        final HashCache hashTable = new HashCache(hashes, hashes);
        final RollingHash rollingHash = new RollingHash(b);
        for (int index = 0; index < hashes*b; index+=b) {
            rollingHash.setup(buffer, index);
            hashTable.add(rollingHash.getHash(), index);
        }
        return hashTable;
    }

    private static void findMatch(final byte[] oldBytes, final int frameStart, final int oldIndex, final byte[] newBytes, final int newIndex, final IntRef nextMatchStart, final IntRef nextMatchLength, final int b) {
        int start = 0;
        int end = b;
        for (int i = start; i < end; i++) {
            if (oldBytes[oldIndex+i] != newBytes[newIndex+i]) {
                nextMatchStart.elem = 0;
                nextMatchLength.elem = 0;
                return;
            }
        }
        while(oldIndex+start > 0 && newIndex+start > frameStart && oldBytes[oldIndex+start-1] == newBytes[newIndex+start-1]) start -=1;
        while(oldIndex+end < oldBytes.length && newIndex+end < newBytes.length && oldBytes[oldIndex+end] == newBytes[newIndex+end]) end+=1;
        nextMatchStart.elem = start;
        nextMatchLength.elem = end-start;
    }

    private static int copy(final byte[] newBytes, final byte[] out, final int outPtr, final int from, final int to) {
        final int length = to-from;
        if (length > 0) {
            out[outPtr] = 0;
            final int vIntLength = VIntUtils.writeBase128Int(out, outPtr+1, length);
            System.arraycopy(newBytes, from, out, outPtr+1+vIntLength, length);
            return length+1+vIntLength;
        }
        return 0;
    }

    public static int diff(final byte[] oldBytes, final byte[] newBytes, final byte[] out, final int outOffset, final int b) {
        int outPtr = outOffset;
        final IntRef nextMatchStart = new IntRef(0);
        final IntRef nextMatchLength = new IntRef(0);
        final HashCache hashTable = makeTable(oldBytes, b);
        int frameStart = 0;
        final int end = newBytes.length-b;
        final RollingHash hash = new RollingHash(b);
outer:  while(true) {
            if (frameStart < end) {
                hash.setup(newBytes, frameStart);
                int index = frameStart;
                while (true) {
                    int bestMatchOldIndex = -1;
                    int bestMatchStart = 0;
                    int bestMatchLength = 0;
                    long it = hashTable.get(hash.getHash());
                    int oldIndex = (int)it;
                    while (oldIndex > 0) {
                        findMatch(oldBytes, frameStart, oldIndex, newBytes, index, nextMatchStart, nextMatchLength, b);
                        if (nextMatchLength.elem > bestMatchLength) {
                            bestMatchStart = nextMatchStart.elem;
                            bestMatchLength = nextMatchLength.elem;
                            bestMatchOldIndex = oldIndex;
                        }
                        it = hashTable.getNext(it);
                        oldIndex = (int)it;
                    }
                    if (bestMatchLength > 0) {
                        outPtr+=copy(newBytes, out, outPtr, frameStart, index+bestMatchStart);
                        out[outPtr] = 1;
                        outPtr++;
                        VIntUtils.writeInt(out, outPtr, bestMatchOldIndex+bestMatchStart);
                        outPtr+=4;
                        VIntUtils.writeInt(out, outPtr, bestMatchLength);
                        outPtr+=4;
                        frameStart = index+bestMatchStart+bestMatchLength;
                        continue outer;
                    }
                    if (index < end) {
                        hash.roll(newBytes[index+b]);
                        index+=1;
                        continue;
                    }
                    break;
                }
            }
            outPtr+=copy(newBytes, out, outPtr, frameStart, newBytes.length);
            return outPtr-outOffset;
        }
    }

    public static void undiff(final byte[] oldBytes, final byte[] diff, final int diffLength, final byte[] out, final int outOffset) {
        int outPtr = outOffset;
        int diffPtr = 0;
        final IntRef readBytes = new IntRef(0);
        while (diffPtr < diffLength) {
            final byte b = diff[diffPtr];
            diffPtr++;
            if (b == 0) {
                final int length = VIntUtils.parseBase128Int(diff, diffPtr, readBytes);
                diffPtr+=readBytes.elem;
                System.arraycopy(diff, diffPtr, out, outPtr, length);
                diffPtr+=length;
                outPtr+=length;
            } else {
                final int start = VIntUtils.parseInt(diff, diffPtr);
                diffPtr+=4;
                final int length = VIntUtils.parseInt(diff, diffPtr);
                diffPtr+=4;
                System.arraycopy(oldBytes, start, out, outPtr, length);
                outPtr+=length;
            }
        }
    }
}
