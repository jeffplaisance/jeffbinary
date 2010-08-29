#include<stdlib.h>
#include<stdio.h>
#include<sys/time.h>
#include "jeffzip.h"

int main() {
    int counter;
    FILE *file;
    file=fopen("/Users/j/Temp/enwik8","rb");
    if (!file) {
        printf("Unable to open file!");
        return 1;
    }
    fseek(file, 0L, SEEK_END);
    int64_t size = ftell(file);
    fseek(file, 0L, SEEK_SET);
    uint8_t* filebuffer = malloc(size);
    fread(filebuffer, 1, size, file);
    fclose(file);
    uint8_t* compressedbuffer = malloc(size);
    uint8_t* decompressedbuffer = malloc(size);
    struct timeval start,stop,result;
    int i;
    for (i = 0; i < 10; i++) {
	    gettimeofday(&start, NULL);
        jeffzip_compress(filebuffer, size, compressedbuffer);
        gettimeofday(&stop, NULL);
        timersub(&stop,&start,&result);
        printf("jeffzip compress time: %f\n", result.tv_sec+result.tv_usec/1000000.0);
        gettimeofday(&start, NULL);
        jeffzip_decompress(compressedbuffer, decompressedbuffer);
    }
    gettimeofday(&stop, NULL);
    timersub(&stop,&start,&result);
    printf("jeffzip decompress time: %f\n", result.tv_sec+result.tv_usec/1000000.0);

    return 0;
}
