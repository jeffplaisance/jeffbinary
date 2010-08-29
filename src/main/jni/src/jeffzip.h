#include <stdint.h>

int32_t jeffzip_compress(uint8_t* dataPtr, int32_t dataLength, uint8_t* outPtr);

int32_t jeffzip_decompress(uint8_t* dataPtr, uint8_t* outPtr);