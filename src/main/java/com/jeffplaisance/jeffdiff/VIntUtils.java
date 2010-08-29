package com.jeffplaisance.jeffdiff;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author jplaisance
 */
public final class VIntUtils {
    public static int parseVSInt(final byte[] in, final int offset, final IntRef readBytes) {
        final int i = parseBase128Int(in, offset, readBytes);
        //equivalent to:
        //if (i&1 == 1) return -(i/2+1); else return i/2;
        return (i >>> 1) ^ (-(i & 1));
    }

    public static int writeVSInt(final byte[] out, final int offset, final int i) {
        //equivalent to:
        //if (i < 0) return writeBase128Int(out, offset, -i*2-1); else return writeBase128Int(out, offset, i*2);
        return writeBase128Int(out, offset, (i << 1) ^ (i >> 31));
    }

    public static int parseBase128Int(final byte[] in, final int offset, final IntRef readBytes) {
        int result;
        if (in[offset] < 0) {
            result = ~in[offset];
            if (in[offset+1] < 0) {
                result |= (~in[offset+1])<<7;
                if (in[offset+2] < 0) {
                    result |= (~in[offset+2])<<14;
                    if (in[offset+3] < 0) {
                        result |= (~in[offset+3])<<21;
                        result |= (in[offset+4])<<28;
                        readBytes.elem = 5;
                        return result;
                    } else {
                        result |= in[offset+3]<<21;
                        readBytes.elem = 4;
                        return result;
                    }
                } else {
                    result |= in[offset+2]<<14;
                    readBytes.elem = 3;
                    return result;
                }
            } else {
                result |= in[offset+1]<<7;
                readBytes.elem = 2;
                return result;
            }
        } else {
            result = in[offset];
            readBytes.elem = 1;
            return result;
        }
    }

    public static int writeBase128Int(final byte[] out,final int offset, final int i) {
        if (i > 0x7F) {
            out[offset] = (byte)~(i&0x7F);
            if (i > 0x3FFF) {
                out[offset+1] = (byte)~((i>>>7)&0x7F);
                if (i > 0x1FFFFF) {
                    out[offset+2] = (byte)~((i>>>14)&0x7F);
                    if (i > 0xFFFFFFF) {
                        out[offset+3] = (byte)~((i>>>21)&0x7F);
                        out[offset+4] = (byte)(i>>>28);
                        return 5;
                    } else {
                        out[offset+3] = (byte)(i>>>21);
                        return 4;
                    }
                } else {
                    out[offset+2] = (byte)(i>>>14);
                    return 3;
                }
            } else {
                out[offset+1] = (byte)(i>>>7);
                return 2;
            }
        } else {
            out[offset] = (byte)i;
            return 1;
        }
    }

    public static int parseInt(final byte[] in, final int offset) {
        return ((in[offset]&0xFF)<<24)|((in[offset+1]&0xFF)<<16)|((in[offset+2]&0xFF)<<8)|(in[offset+3]&0xFF);
    }

    public static void writeInt(final byte[] out, final int offset, final int i) {
        out[offset] = (byte)(i>>>24);
        out[offset+1] = (byte)(i>>>16);
        out[offset+2] = (byte)(i>>>8);
        out[offset+3] = (byte)(i);
    }
    
    public static int parseVSInt(final ChannelBuffer in) {
        final int i = parseBase128Int(in);
        //equivalent to:
        //if (i&1 == 1) return -(i/2+1); else return i/2;
        return (i >>> 1) ^ (-(i & 1));
    }

    public static int writeVSInt(final ChannelBuffer out, final int i) {
        //equivalent to:
        //if (i < 0) return writeBase128Int(out, offset, -i*2-1); else return writeBase128Int(out, offset, i*2);
        return writeBase128Int(out, (i << 1) ^ (i >> 31));
    }

    public static int parseBase128Int(final ChannelBuffer in) {
        int result;
        byte b;
        if ((b = in.readByte()) < 0) {
            result = ~b;
            if ((b = in.readByte()) < 0) {
                result |= (~b)<<7;
                if ((b = in.readByte()) < 0) {
                    result |= (~b)<<14;
                    if ((b = in.readByte()) < 0) {
                        result |= (~b)<<21;
                        result |= in.readByte()<<28;
                        return result;
                    } else {
                        result |= b<<21;
                        return result;
                    }
                } else {
                    result |= b<<14;
                    return result;
                }
            } else {
                result |= b<<7;
                return result;
            }
        } else {
            result = b;
            return result;
        }
    }

    public static int writeBase128Int(final ChannelBuffer out, final int i) {
        if (i > 0x7F) {
            out.writeByte(~(i&0x7F));
            if (i > 0x3FFF) {
                out.writeByte(~((i>>>7)&0x7F));
                if (i > 0x1FFFFF) {
                    out.writeByte(~((i>>>14)&0x7F));
                    if (i > 0xFFFFFFF) {
                        out.writeByte(~((i>>>21)&0x7F));
                        out.writeByte(i>>>28);
                        return 5;
                    } else {
                        out.writeByte(i>>>21);
                        return 4;
                    }
                } else {
                    out.writeByte(i>>>14);
                    return 3;
                }
            } else {
                out.writeByte(i>>>7);
                return 2;
            }
        } else {
            out.writeByte(i);
            return 1;
        }
    }
}
