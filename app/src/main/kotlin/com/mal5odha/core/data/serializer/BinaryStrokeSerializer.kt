package com.mal5odha.core.data.serializer

import com.mal5odha.core.data.models.Ml5Point
import com.mal5odha.core.data.models.Ml5Stroke
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * High-performance binary serializer for stroke vector data.
 * Provides order-of-magnitude faster deserialization and significantly smaller file sizes
 * compared to verbose JSON formatting.
 */
object BinaryStrokeSerializer {

    private const val MAGIC_HEADER: Int = 0x4D4C3553 // "ML5S" (Mal5odha Stroke)
    private const val FORMAT_VERSION: Byte = 1

    /**
     * Serializes a list of [Ml5Stroke] into a binary stream.
     */
    fun serialize(strokes: List<Ml5Stroke>, outputStream: OutputStream) {
        val dataOut = DataOutputStream(outputStream)
        dataOut.writeInt(MAGIC_HEADER)
        dataOut.writeByte(FORMAT_VERSION.toInt())
        dataOut.writeInt(strokes.size)

        for (stroke in strokes) {
            dataOut.writeUTF(stroke.id)
            dataOut.writeInt(stroke.color)
            dataOut.writeFloat(stroke.width)
            dataOut.writeUTF(stroke.tool)

            dataOut.writeInt(stroke.points.size)
            for (p in stroke.points) {
                dataOut.writeFloat(p.x)
                dataOut.writeFloat(p.y)
                dataOut.writeFloat(p.pressure)
                dataOut.writeLong(p.timestamp)
            }
        }
        dataOut.flush()
    }

    /**
     * Serializes strokes to a byte array.
     */
    fun toByteArray(strokes: List<Ml5Stroke>): ByteArray {
        val byteOut = ByteArrayOutputStream()
        serialize(strokes, byteOut)
        return byteOut.toByteArray()
    }

    /**
     * Deserializes a binary stream into a list of [Ml5Stroke].
     */
    fun deserialize(inputStream: InputStream): List<Ml5Stroke> {
        val dataIn = DataInputStream(inputStream)
        val magic = dataIn.readInt()
        if (magic != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid binary stroke stream: bad magic header $magic")
        }

        val version = dataIn.readByte()
        if (version.toInt() != FORMAT_VERSION.toInt()) {
            throw IllegalArgumentException("Unsupported stroke format version: $version")
        }

        val strokeCount = dataIn.readInt()
        val strokes = ArrayList<Ml5Stroke>(strokeCount)

        for (i in 0 until strokeCount) {
            val id = dataIn.readUTF()
            val color = dataIn.readInt()
            val width = dataIn.readFloat()
            val tool = dataIn.readUTF()

            val pointCount = dataIn.readInt()
            val points = ArrayList<Ml5Point>(pointCount)

            for (j in 0 until pointCount) {
                val x = dataIn.readFloat()
                val y = dataIn.readFloat()
                val pressure = dataIn.readFloat()
                val timestamp = dataIn.readLong()
                points.add(Ml5Point(x, y, pressure, timestamp))
            }

            strokes.add(Ml5Stroke(id, color, width, tool, points))
        }

        return strokes
    }

    /**
     * Deserializes from a byte array.
     */
    fun fromByteArray(bytes: ByteArray): List<Ml5Stroke> {
        return deserialize(ByteArrayInputStream(bytes))
    }
}
