#include <stdlib.h>
#include <string.h>
#include "jeffzip.h"

static uint32_t parseBase128Int(int8_t* in, int32_t* readBytes) {
    uint32_t result;
    if (in[0] < 0) {
        result = ~in[0];
        if (in[1] < 0) {
            result |= (~in[1])<<7;
            if (in[2] < 0) {
                result |= (~in[2])<<14;
                if (in[3] < 0) {
                    result |= (~in[3])<<21;
                    result |= (in[4])<<28;
                    *readBytes = 5;
                    return result;
                } else {
                    result |= in[3]<<21;
                    *readBytes = 4;
                    return result;
                }
            } else {
                result |= in[2]<<14;
                *readBytes = 3;
                return result;
            }
        } else {
            result |= in[1]<<7;
            *readBytes = 2;
            return result;
        }
    } else {
        result = in[0];
        *readBytes = 1;
        return result;
    }
}

static int32_t writeBase128Int(int8_t* out, uint32_t i) {
    if (i > 0x7F) {
        out[0] = ~(i&0x7F);
        if (i > 0x3FFF) {
            out[1] = ~((i>>7)&0x7F);
            if (i > 0x1FFFFF) {
                out[2] = ~((i>>14)&0x7F);
                if (i > 0xFFFFFFF) {
                    out[3] = ~((i>>21)&0x7F);
                    out[4] = i>>28;
                    return 5;
                } else {
                    out[3] = i>>21;
                    return 4;
                }
            } else {
                out[2] = i>>14;
                return 3;
            }
        } else {
            out[1] = i>>7;
            return 2;
        }
    } else {
        out[0] = i;
        return 1;
    }
}

static int32_t parseVSInt(int8_t* in, int32_t* readBytes) {
    uint32_t i = parseBase128Int(in, readBytes);
    //equivalent to:
    //if (i % 2 == 1) return -(i/2+1); else return i/2;
    return (i >> 1) ^ (-(i & 1));
}

static int32_t writeVSInt(int8_t* out, int32_t i) {
    //equivalent to:
    //if (i < 0) return writeBase128Int(out, -i*2-1); else return writeBase128Int(out, i*2);
    return writeBase128Int(out, (i << 1) ^ (i >> 31));
}

#define BLOCK_SIZE 6
#define ROTATE (BLOCK_SIZE&0x1F)
const int32_t hashF[] = {2009259630, 1017151718, -214865856, -867652023, -1203634412,
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

typedef struct {
    uint8_t buffer[BLOCK_SIZE];
    int32_t index;
    uint32_t hash;
} RollingHash;

static void RollingHash_setup(RollingHash* this, uint8_t* data) {
    int32_t i;
    this->index = 0;
    this->hash = 0;
    memcpy(this->buffer, data, sizeof(this->buffer));
    for (i = 0; i < BLOCK_SIZE; i++) {
        this->hash = ((this->hash<<1)|(this->hash>>31)) ^ hashF[data[i]];
    }
}

static uint32_t RollingHash_roll(RollingHash* this, uint8_t b) {
    uint32_t h = hashF[this->buffer[this->index]];
    uint32_t s = ((h<<ROTATE)|(h>>(32-ROTATE)));
    this->hash = ((this->hash<<1)|(this->hash>>31)) ^ s ^ hashF[b];
    this->buffer[this->index] = b;
    this->index = (this->index+1)%sizeof(this->buffer);
    return this->hash;
}

#define HASH_SIZE 32768
#define HASH_MASK (HASH_SIZE-1)

static void findMatch(uint8_t* data, int32_t dataLength, int32_t frameStart, int32_t oldIndex, int32_t newIndex, int32_t* nextMatchStart, int32_t* nextMatchLength) {
    int32_t start = 0;
    int32_t end = BLOCK_SIZE;
    if (memcmp(data+oldIndex, data+newIndex, BLOCK_SIZE)) {
        *nextMatchStart = 0;
        *nextMatchLength = 0;
        return;
    }
    while(oldIndex+start > 0 && newIndex+start > frameStart && data[oldIndex+start-1] == data[newIndex+start-1]) start--;
    while(newIndex+end < dataLength && data[oldIndex+end] == data[newIndex+end]) end++;
    *nextMatchStart = start;
    *nextMatchLength = end-start;
}

static int32_t copy(uint8_t* data, uint8_t* out, int32_t from, int32_t to) {
    int32_t length = to-from;
    int32_t written = 0;
    if (length > 0) {
        written+=writeVSInt(out, length);
        memcpy(out+written, data+from, length);
        written+=length;
    }
    return written;
}

typedef struct {
    uint32_t hash;
    int32_t index;
} Entry;

int32_t jeffzip_compress(uint8_t* dataPtr, int32_t dataLength, uint8_t* outPtr) {
    const uint8_t* outStart = outPtr;
    outPtr+=writeBase128Int(outPtr, dataLength);

    Entry* hashTable = malloc(HASH_SIZE*sizeof(Entry));
    memset(hashTable, 0xFF, HASH_SIZE*sizeof(Entry));
    RollingHash hash;
    const int32_t end = dataLength-BLOCK_SIZE;
    int32_t frameIndex = 0;
frame:
    if (frameIndex < end) {
        int32_t index = frameIndex;
        RollingHash_setup(&hash, dataPtr+frameIndex);
loop:   {
            int32_t hashIndex = hash.hash&HASH_MASK;
            Entry old = hashTable[hashIndex];
            Entry newEntry = {hash.hash, index};
            hashTable[hashIndex] = newEntry;
            if (old.index >= 0 && old.hash == hash.hash) {
                int32_t matchStart;
                int32_t matchLength;
                findMatch(dataPtr, dataLength, frameIndex, old.index, index, &matchStart, &matchLength);
                if (matchLength > 0) {
                    int32_t backtrack = index-old.index;
                    outPtr+=copy(dataPtr, outPtr, frameIndex, index+matchStart);
                    outPtr+=writeVSInt(outPtr, -(((matchLength-BLOCK_SIZE)<<3)|(backtrack&0x7)));
                    outPtr+=writeBase128Int(outPtr, backtrack>>3);
                    frameIndex = index+matchStart+matchLength;
                    goto frame;
                }
            }
            if (index < end) {
                RollingHash_roll(&hash, dataPtr[index+BLOCK_SIZE]);
                index++;
                goto loop;
            }
        }
    }
    free(hashTable);
    outPtr+=copy(dataPtr, outPtr, frameIndex, dataLength);
    return outPtr-outStart;
}

int32_t jeffzip_decompress(uint8_t* dataPtr, uint8_t* outPtr) {
    int32_t readBytes;
    int32_t uncompressedLength = parseBase128Int(dataPtr, &readBytes);
    dataPtr+= readBytes;
    const uint8_t* end = outPtr+uncompressedLength;
    while (outPtr < end) {
        int32_t first = parseVSInt(dataPtr, &readBytes);
        dataPtr+=readBytes;
        if (first <= 0) {
            int32_t second = parseBase128Int(dataPtr, &readBytes);
            dataPtr+=readBytes;
            int32_t posFirst = -first;
            int32_t length = (posFirst>>3)+BLOCK_SIZE;
            int32_t backtrack = (second<<3)|(posFirst&0x7);
            uint8_t* index = outPtr-backtrack;
            if (index+length > outPtr) {
                int32_t i;
                for (i = 0; i < length; i++) {
                    outPtr[i] = index[i];
                }
            } else {
                memcpy(outPtr, index, length);
            }
            outPtr+=length;
        } else {
            memcpy(outPtr, dataPtr, first);
            dataPtr+=first;
            outPtr+=first;
        }
    }
    return uncompressedLength;
}
