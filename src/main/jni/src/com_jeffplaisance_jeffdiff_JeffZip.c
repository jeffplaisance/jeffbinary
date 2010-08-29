#include "com_jeffplaisance_jeffdiff_JeffZip.h"
#include "jeffzip.h"

JNIEXPORT jint JNICALL Java_com_jeffplaisance_jeffdiff_JeffZip_compress
        (JNIEnv* env, jclass class, jobject data, jlong dataOffset, jint dataLength, jobject out, jlong outOffset) {
    uint8_t* dataPtr = (*env)->GetDirectBufferAddress(env, data)+dataOffset;
    uint8_t* outPtr = (*env)->GetDirectBufferAddress(env, out)+outOffset;
    return jeffzip_compress(dataPtr, dataLength, outPtr);
}

JNIEXPORT jint JNICALL Java_com_jeffplaisance_jeffdiff_JeffZip_decompress
        (JNIEnv* env, jclass class, jobject data, jlong dataOffset, jobject out, jlong outOffset) {
    uint8_t* outPtr = (*env)->GetDirectBufferAddress(env, out)+outOffset;
    uint8_t* dataPtr = (*env)->GetDirectBufferAddress(env, data)+dataOffset;
    return jeffzip_decompress(dataPtr, outPtr);
}
