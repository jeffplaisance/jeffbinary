package com.jeffplaisance.jeffdiff

import com.google.common.base.Charsets
import java.io.{RandomAccessFile, InputStreamReader}
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferInputStream}


/**
 * @author jplaisance
 */
object BenchmarkJeffDiff {

    def main(args: Array[String]) {
        buffer
        array
    }

    def buffer {
        val oFile = new RandomAccessFile("/Users/j/Temp/enwik8", "r")
        val mFile = new RandomAccessFile("/Users/j/Temp/enwik8jeffff", "r")
        val oBuffer = ChannelBuffers.directBuffer(oFile.length.toInt)
        val mBuffer = ChannelBuffers.directBuffer(mFile.length.toInt)
        oBuffer.writeBytes(oFile.getChannel, oFile.length.toInt)
        mBuffer.writeBytes(mFile.getChannel, mFile.length.toInt)
        val diffBuffer = ChannelBuffers.directBuffer(mFile.length.toInt+6)
        JeffDiff.diff(oBuffer, mBuffer, diffBuffer, 16)
        println(diffBuffer.writerIndex)
        diffBuffer.clear
        var start = System.currentTimeMillis
        JeffDiff.diff(oBuffer, mBuffer, diffBuffer, 16)
        println("diff time: "+(System.currentTimeMillis-start))
        val undiffBuffer = ChannelBuffers.directBuffer(mFile.length.toInt)
        JeffDiff.undiff(oBuffer, diffBuffer, undiffBuffer)
        diffBuffer.readerIndex(0)
        undiffBuffer.clear
        start = System.currentTimeMillis
        JeffDiff.undiff(oBuffer, diffBuffer, undiffBuffer)
        println("undiff time: "+(System.currentTimeMillis-start))
        val mBufferReader = new InputStreamReader(new ChannelBufferInputStream(mBuffer), Charsets.UTF_8)
        val undiffReader = new InputStreamReader(new ChannelBufferInputStream(undiffBuffer), Charsets.UTF_8)
        var i = 0
        var j = 0
        while (true) {
            val a = mBufferReader.read
            val b = undiffReader.read

            if (a < 0 || b < 0) return
            if (a != b) {
                if (i < 1000) {
                    println("mismatch: "+a.toChar+" "+b.toChar)
                    i+=1
                } else {
                    println(j)
                    return
                }
            }
            j+=1
        }
        oFile.close
        mFile.close
    }

    def array {
        val oFile = new RandomAccessFile("/Users/j/Temp/enwik8", "r")
        val mFile = new RandomAccessFile("/Users/j/Temp/enwik8jeffff", "r")
        val oBuffer = ChannelBuffers.wrappedBuffer(new Array[Byte](oFile.length.toInt))
        val mBuffer = ChannelBuffers.wrappedBuffer(new Array[Byte](mFile.length.toInt))
        oBuffer.clear
        mBuffer.clear
        oBuffer.writeBytes(oFile.getChannel, oFile.length.toInt)
        mBuffer.writeBytes(mFile.getChannel, mFile.length.toInt)
        val diffBuffer = ChannelBuffers.wrappedBuffer(new Array[Byte](mFile.length.toInt))
        diffBuffer.clear
        val diffLength = JeffDiff.diff(oBuffer.array, mBuffer.array, diffBuffer.array, 0, 16)
        println(diffLength)
        diffBuffer.clear
        var start = System.currentTimeMillis
        JeffDiff.diff(oBuffer.array, mBuffer.array, diffBuffer.array, 0, 16)
        println("diff time: "+(System.currentTimeMillis-start))
        val undiffBuffer = ChannelBuffers.wrappedBuffer(new Array[Byte](mFile.length.toInt))
        undiffBuffer.clear
        JeffDiff.undiff(oBuffer.array, diffBuffer.array, diffLength, undiffBuffer.array, 0);
        diffBuffer.readerIndex(0)
        undiffBuffer.clear
        start = System.currentTimeMillis
        JeffDiff.undiff(oBuffer.array, diffBuffer.array, diffLength, undiffBuffer.array, 0);
        println("undiff time: "+(System.currentTimeMillis-start))
        val mBufferReader = new InputStreamReader(new ChannelBufferInputStream(mBuffer), Charsets.UTF_8)
        val undiffReader = new InputStreamReader(new ChannelBufferInputStream(undiffBuffer), Charsets.UTF_8)
        var i = 0
        var j = 0
        while (true) {
            val a = mBufferReader.read
            val b = undiffReader.read

            if (a < 0 || b < 0) return
            if (a != b) {
                if (i < 1000) {
                    println("mismatch: "+a.toChar+" "+b.toChar)
                    i+=1
                } else {
                    println(j)
                    return
                }
            }
            j+=1
        }
    }
}
