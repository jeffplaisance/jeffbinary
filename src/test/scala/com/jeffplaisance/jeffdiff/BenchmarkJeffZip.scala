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

package com.jeffplaisance.jeffdiff

import org.jboss.netty.buffer.ChannelBuffers
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode
import java.io.{File, RandomAccessFile}

/**
 * @author jplaisance
 */
object BenchmarkJeffZip {

    def main(args: Array[String]) {
        testNative
        test
        val temp = new File(args(0))
        for (file <- temp.listFiles.filter(x => x.length < 1024*1024*128 && !x.isDirectory)) {
            println(file.getAbsolutePath)
            println("--------------------------------------------------------")
            println("native")
            native(file.getAbsolutePath)
            println("jvm")
            jvm(file.getAbsolutePath)
            checkcompat(file.getAbsolutePath)
            checkcompat2(file.getAbsolutePath)
            println
        }
    }

    def testNative = {
        val str = "Lossless data compression is a class of data compression algorithms that allows the exact original data to be reconstructed from the compressed data. The term lossless is in contrast to lossy data compression, which only allows an approximation of the original data to be reconstructed, in exchange for better compression rates."
        val array = str.getBytes
        val buffer = ByteBuffer.allocateDirect(array.length)
        buffer.put(array)
        val out = ByteBuffer.allocateDirect(array.length+1024)
        JeffZip.compress(buffer, 0, array.length, out, 0)
        val decomp = ByteBuffer.allocateDirect(array.length)
        JeffZip.decompress(out, 0, decomp, 0)
        val decompArray = new Array[Byte](array.length)
        decomp.get(decompArray)
        println(new String(decompArray, 0, decompArray.length))
    }

    def test = {
        val str = "Lossless data compression is a class of data compression algorithms that allows the exact original data to be reconstructed from the compressed data. The term lossless is in contrast to lossy data compression, which only allows an approximation of the original data to be reconstructed, in exchange for better compression rates."
        val buffer = str.getBytes
        val out = new Array[Byte](buffer.length+1024)
        JeffZip.compress(buffer,0, buffer.length, out, 0)
        val decomp = new Array[Byte](buffer.length)
        JeffZip.decompress(out, 0, decomp, 0)
        println(new String(decomp, 0, decomp.length))
    }

    def native(fileName:String):Unit = {
        val file = new RandomAccessFile(fileName, "r")
        val buffer = ByteBuffer.allocateDirect(file.length.toInt)
        val fileChannel = file.getChannel
        val mappedFileBuffer = fileChannel.map(MapMode.READ_ONLY, 0, file.length.toInt)
        val fileLength = file.length.toInt
        fileChannel.close
        file.close
        buffer.put(mappedFileBuffer)
        println(fileLength)
        val compressDirect = ByteBuffer.allocateDirect(fileLength+1024)
        JeffZip.compress(buffer, 0, fileLength, compressDirect, 0)
        var start = System.currentTimeMillis
        val compressLength = JeffZip.compress(buffer, 0, fileLength, compressDirect, 0)
        println("compress time: "+(System.currentTimeMillis-start))
        println(compressLength)
        val decompressBuffer = ByteBuffer.allocateDirect(fileLength)
        JeffZip.decompress(compressDirect, 0, decompressBuffer, 0)
        start = System.currentTimeMillis
        JeffZip.decompress(compressDirect, 0, decompressBuffer, 0)
        println("decompress time: "+(System.currentTimeMillis-start))
        for (i <- 0 until fileLength) {
            if (buffer.get(i) != decompressBuffer.get(i)) {
                throw new RuntimeException("ruh roh")
            }
        }
    }

    def jvm(fileName:String) = {
        val file = new RandomAccessFile(fileName, "r")
        val buffer = ChannelBuffers.dynamicBuffer(file.length.toInt)
        buffer.writeBytes(file.getChannel, file.length.toInt)
        val fileLength = file.length.toInt
        file.close
        val compressArray = new Array[Byte](fileLength+1024)
        JeffZip.compress(buffer.array, 0, buffer.writerIndex, compressArray, 0)
        var start = System.currentTimeMillis
        val compressLength = JeffZip.compress(buffer.array, 0, buffer.writerIndex, compressArray, 0)
        println("compress time: "+(System.currentTimeMillis-start))
        println(compressLength)
        val decompressBuffer = new Array[Byte](fileLength)
        JeffZip.decompress(compressArray, 0, decompressBuffer, 0)
        start = System.currentTimeMillis
        JeffZip.decompress(compressArray, 0, decompressBuffer, 0)
        println("decompress time: "+(System.currentTimeMillis-start))
        for (i <- 0 until buffer.writerIndex) {
            if (buffer.getByte(i) != decompressBuffer(i)) throw new RuntimeException("ruh roh")
        }
    }

    def checkcompat(fileName:String):Unit = {
        val file = new RandomAccessFile(fileName, "r")
        val buffer = ChannelBuffers.dynamicBuffer(file.length.toInt)
        buffer.writeBytes(file.getChannel, file.length.toInt)
        val fileLength = file.length.toInt
        file.close
        val compressArray = new Array[Byte](fileLength+1024)
        JeffZip.compress(buffer.array, 0, buffer.writerIndex, compressArray, 0)
        val compressLength = JeffZip.compress(buffer.array, 0, buffer.writerIndex, compressArray, 0)
        val compressBuffer = ByteBuffer.allocateDirect(fileLength+1024);
        compressBuffer.put(compressArray);
        val decompressBuffer = ByteBuffer.allocateDirect(fileLength)
        JeffZip.decompress(compressBuffer, 0, decompressBuffer, 0)
        for (i <- 0 until buffer.writerIndex) {
            if (buffer.getByte(i) != decompressBuffer.get(i)) {
                throw new RuntimeException("ruh roh")
            }
        }
        println("native decompressor can decompress jvm compressor output")
    }

    def checkcompat2(fileName:String):Unit = {
        val file = new RandomAccessFile(fileName, "r")
        val buffer = ByteBuffer.allocateDirect(file.length.toInt)
        val fileChannel = file.getChannel
        val mappedFileBuffer = fileChannel.map(MapMode.READ_ONLY, 0, file.length.toInt)
        val fileLength = file.length.toInt
        fileChannel.close
        file.close
        buffer.put(mappedFileBuffer)
        val compressDirect = ByteBuffer.allocateDirect(fileLength+1024)
        JeffZip.compress(buffer, 0, fileLength, compressDirect, 0)

        val compressArray = new Array[Byte](fileLength+1024)
        compressDirect.get(compressArray);
        val decompressBuffer = new Array[Byte](fileLength)
        JeffZip.decompress(compressArray, 0, decompressBuffer, 0)
        for (i <- 0 until buffer.capacity) {
            if (buffer.get(i) != decompressBuffer(i)) {
                throw new RuntimeException("ruh roh")
            }
        }
        println("jvm decompressor can decompress native compressor output")
    }
}
