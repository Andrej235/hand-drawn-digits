package com.example.digitrecognizer

import android.content.Context
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.zip.ZipInputStream

fun readNpz(context: Context, filename: String): Map<String, Array<FloatArray>> {
    val arrays = mutableMapOf<String, Array<FloatArray>>()
    context.assets.open(filename).use { stream ->
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val arrayName = entry.name.removeSuffix(".npy")
                    val arrayData = parseNpy(zip)
                    arrays[arrayName] = arrayData
                }
                entry = zip.nextEntry
            }
        }
    }
    return arrays
}

fun parseNpy(stream: InputStream): Array<FloatArray> {
    // Read magic bytes (first 6 bytes)
    val magic = ByteArray(6)
    stream.read(magic)

    // Verify NPY format (first byte 0x93, then "NUMPY")
    require(
        magic[0] == 0x93.toByte() && magic[1].toInt().toChar() == 'N' && magic[2].toInt()
            .toChar() == 'U' && magic[3].toInt().toChar() == 'M' && magic[4].toInt()
            .toChar() == 'P' && magic[5].toInt().toChar() == 'Y'
    ) {
        "Invalid NPY magic number"
    }

    val versionMajor = stream.read()
    val versionMinor = stream.read()

    // Read header length (little-endian short)
    val headerLength = when (versionMajor) {
        1 -> stream.readShortLittleEndian()
        2 -> stream.readIntLittleEndian().toShort()  // Actually 4 bytes for v2
        else -> throw IOException("Unsupported NPY version")
    }

    // Read header
    val headerBytes = ByteArray(headerLength.toInt())
    stream.read(headerBytes)
    val header = String(headerBytes, Charsets.UTF_8)

    // Improved shape parsing
    val shapeRegex = "'shape':\\s*\\(\\s*(\\d+\\s*,?\\s*)+\\s*\\)".toRegex()
    val shapeMatch = shapeRegex.find(header) ?: throw IOException("Invalid shape in NPY header")

    // Extract all numbers from shape tuple
    val shape = shapeMatch.value.replace("'shape':", "").replace("[()\\s]".toRegex(), "").split(",")
        .filter { it.isNotEmpty() }.map { it.toInt() }

    // Handle different dimensionalities
    val (rows, cols) = when (shape.size) {
        1 -> Pair(1, shape[0])  // Treat 1D array as 2D with 1 row
        2 -> Pair(shape[0], shape[1])
        else -> throw IOException("Unsupported array dimensions: ${shape.size}D")
    }

    // Parse dtype (updated to handle your '<i8' example)
    val descrRegex = "'descr':\\s*'([^']*)'".toRegex()
    val descr =
        descrRegex.find(header)?.groupValues?.get(1) ?: throw IOException("Missing dtype in header")

    // Configure based on data type
    val (elementSize, bufferHandler) = when (descr) {
        "<i8" -> Pair(8, { buffer: ByteBuffer -> buffer.asLongBuffer() })
        "<f4" -> Pair(4, { buffer: ByteBuffer -> buffer.asFloatBuffer() })
        "<f8" -> Pair(8, { buffer: ByteBuffer -> buffer.asDoubleBuffer() })
        else -> throw IOException("Unsupported dtype: $descr")
    }

    // Read data with guaranteed full buffer population
    val buffer = ByteArray(rows * cols * elementSize)
    var totalRead = 0
    while (totalRead < buffer.size) {
        val bytesRead = stream.read(buffer, totalRead, buffer.size - totalRead)
        if (bytesRead == -1) throw EOFException("Unexpected end of stream")
        totalRead += bytesRead
    }

    val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
    val typedBuffer = bufferHandler(byteBuffer)

    // Create array with proper row-major ordering
    return Array(rows) { row ->
        FloatArray(cols) { col ->
            // Calculate position based on actual buffer layout
            val pos = when (descr) {
                "<i8" -> (typedBuffer as LongBuffer)[row * cols + col].toFloat()
                "<f4" -> (typedBuffer as FloatBuffer)[row * cols + col]
                "<f8" -> (typedBuffer as DoubleBuffer)[row * cols + col].toFloat()
                else -> throw IllegalStateException()
            }
            pos
        }
    }
}

// Helper extensions for reading binary data
fun InputStream.readShortLittleEndian(): Short {
    val bytes = ByteArray(2)
    read(bytes)
    return (bytes[0].toInt() and 0xFF or (bytes[1].toInt() and 0xFF shl 8)).toShort()
}

fun InputStream.readIntLittleEndian(): Int {
    val bytes = ByteArray(4)
    read(bytes)
    return (bytes[0].toInt() and 0xFF) or (bytes[1].toInt() and 0xFF shl 8) or (bytes[2].toInt() and 0xFF shl 16) or (bytes[3].toInt() and 0xFF shl 24)
}
