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

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author jplaisance
 */

public final class RollingHash {
    private final byte[] buffer;
    private final int blockSize;
    private final int rotate;
    private int index = 0;
    private int hash = 0;
    private static final int[] hashF = new int[]{
                         2009259630, 1017151718, -214865856, -867652023, -1203634412,
                         1576651658, 1468553019, 115080097, -422130441, 641841944,
                         1065711022, 1377398376, 514867700, -1391691509, -1370269453,
                         310529824, -1498768137, 1917798874, -191331803, 735176465,
                         -1373414483, -2132175198, -1633038093, 1930615952, 396794750,
                         2021813506, 1190186935, -1493725308, 1532827066, 1591974214,
                         -463328058, -88262106, -438228207, -1880506682, -573442807,
                         1444534791, 1469362760, 1924338320, 1907909580, -969857705,
                         -1007124950, 690789444, -405578795, -1467150237, -752759109,
                         -961011393, -465707592, 232433503, 813244851, -1448927748,
                         56102761, -420200347, 1957762602, -339119690, 1752148243,
                         1070483901, 479942543, -146497456, -138290393, 426522718,
                         -1100168226, -1095577991, -1133706826, 576150197, -1704902382,
                         -475079669, 1031387395, 1684640676, -1648167465, -607829836,
                         -678942892, 190464461, -1352672893, 1791463127, 1009225736,
                         966514447, -1917595833, 991609012, -106200082, -855841712,
                         146587809, 129676229, -139246893, 1382522237, 114387732,
                         1651471979, -1966267812, -733537842, -1809132261, 962022345,
                         -1645670890, -909519794, 1935384154, -1817015824, 1072329931,
                         447557786, -252731625, -1391675844, 131806999, -743045144,
                         -2128508185, 1638958077, 1883663162, -139034038, 1127975596,
                         1846188376, -883226066, -2143174604, 1980957118, 511262319,
                         1659099644, -1808656325, 861860455, -465216832, -366794422,
                         -1322324705, 531346837, -1453103926, -1586089928, -1129619445,
                         -911869494, 1700866037, -2054346131, -391683302, 279067990,
                         1020232883, 188907116, 353431581, 1726298056, -1748660511,
                         1055688235, -532977765, 595912354, -32723341, 2007682278,
                         1002396696, 1359032651, -295983536, 367580720, -411742822,
                         -1540466615, 1006675323, 895337034, 1046616424, 704291759,
                         297322098, -1485741129, -510385643, 356254931, 2071306458,
                         -1738237425, -547589188, 793354052, 565694811, -1457256941,
                         1514104972, 1106275578, -1439720503, 378104577, -1901293178,
                         1318154703, 1489177476, 1581338922, -1857689723, 808137262,
                         338376224, 651148737, -1518696322, -201891575, -1068465081,
                         -1937239052, -1917221623, -1855915760, 11019486, 905540416,
                         1740997152, -173656801, 1414729318, 680666881, 1090138917,
                         -2067981321, 786510500, -2024670392, -1948192727, 659067696,
                         698468484, -1970500880, -2105414550, -673763986, -996945862,
                         -969286925, -1951149794, -276551726, -375900758, 2103760633,
                         897826926, 1990609587, -1564920566, 1647084337, -806615868,
                         -1329618546, 997254596, -473565340, 967831625, 1721515578,
                         2131955616, -1942325433, -644370263, -172131489, -2112049630,
                         -8504304, -525395175, 2081583657, -1821522384, -768490677,
                         -355027814, 433428279, -1416072712, -1591167251, -2111407490,
                         -193809703, 767182608, -521223511, 1402351403, -1814583173,
                         -1392280866, -1489769854, 1884761886, 1120256347, -295605959,
                         -326158687, 1938297395, 568356482, -1393008304, 1017104488,
                         -5595870, -1483622927, 208854218, -1702777658, -1228355764,
                         -743719143, -873822930, 957153228, -1950492488, -1607322992,
                         -1986942075, 246101376, 1323623511, 1479368962, -259385794,
                         -587752463, 1690182287, -720083611, -641709744, -367170933,
                         2006746635,
    };

    public RollingHash(final int blockSize) {
        this.blockSize = blockSize;
        rotate = blockSize%32;
        this.buffer = new byte[blockSize];
    }

    public void setup(final ChannelBuffer buffer, final int start) {
        index = 0;
        hash = 0;
        if (buffer.writerIndex()-start < blockSize) throw new IndexOutOfBoundsException();
        for (int i = 0; i < blockSize; i++) {
            final int b = buffer.getUnsignedByte(start+i);
            this.buffer[i] = (byte)b;
            hash = Integer.rotateLeft(hash, 1) ^ hashF[b];
        }
    }

    public void setup(final byte[] buffer, final int start) {
        index = 0;
        hash = 0;
        if (buffer.length-start < blockSize) throw new IndexOutOfBoundsException();
        for (int i = 0; i < blockSize; i++) {
            final int b = buffer[start+i]&0xFF;
            this.buffer[i] = (byte)b;
            hash = Integer.rotateLeft(hash, 1) ^ hashF[b];
        }
    }

    public int getHash() {
        return hash;
    }

    public int roll(final byte b) {
        final int s = Integer.rotateLeft(hashF[buffer[index]&0xFF], rotate);
        hash = Integer.rotateLeft(hash, 1) ^ s ^ hashF[b&0xFF];
        buffer[index] = b;
        index = (index+1)%buffer.length;
        return hash;
    }
}
