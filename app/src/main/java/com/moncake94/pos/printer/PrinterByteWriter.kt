package com.moncake94.pos.printer

import java.io.OutputStream

private const val PRINT_CHUNK_SIZE = 128
private const val PRINT_CHUNK_DELAY_MS = 20L
private const val PRINT_FINISH_DELAY_MS = 500L

fun OutputStream.writePrinterBytes(bytes: ByteArray) {
    bytes.asIterableChunks(PRINT_CHUNK_SIZE).forEach { chunk ->
        write(chunk)
        flush()
        Thread.sleep(PRINT_CHUNK_DELAY_MS)
    }
    flush()
    Thread.sleep(PRINT_FINISH_DELAY_MS)
}

fun ByteArray.asIterableChunks(size: Int): Iterable<ByteArray> = Iterable {
    object : Iterator<ByteArray> {
        private var offset = 0

        override fun hasNext(): Boolean = offset < this@asIterableChunks.size

        override fun next(): ByteArray {
            val end = (offset + size).coerceAtMost(this@asIterableChunks.size)
            return copyOfRange(offset, end).also { offset = end }
        }
    }
}
