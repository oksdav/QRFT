package com.example.qrft

import android.content.Context
import com.example.qrft.databinding.ActivityMainBinding

class Sender(
    binding: ActivityMainBinding,
    context: Context
) : QRCodeHandler(binding) {
    companion object {
        private const val LAST_SEQUENCE_NUMBER = 2
        private const val QRCODE_SIZE_IN_BITS = 40
    }

    private var sequenceNumber = 1
    private var currChunkNumber = 0
    private lateinit var chunks : Array<String>
    private val fileHandler = FileHandler(context)
    private lateinit var fileData : String


    fun send(fileName: String) {
        fileData = fileHandler.readTextFile(fileName).toString()
        chunks = splitDataToChunks(fileData)

        // Sends title
        this.generateQRCode(fileName)
    }

    private fun sendNextChunk() {
        this.generateQRCode(chunks[currChunkNumber])
        currChunkNumber ++
    }

    private fun splitDataToChunks(data: String): Array<String> {
        val chunks = emptyArray<String>()
        var i = 0
        while (i < data.length) {
            setSequenceNumber(data, i)
            val nextChunk = data.slice(IntRange(i,
                    (i + QRCODE_SIZE_IN_BITS).coerceAtMost(data.length - 1)))

            chunks.plus(nextChunk)
            i += QRCODE_SIZE_IN_BITS
        }
        return chunks
    }

    private fun setSequenceNumber(data: String, index: Int) {
        sequenceNumber = if (index + QRCODE_SIZE_IN_BITS > data.length) {
            LAST_SEQUENCE_NUMBER
        } else {
            1 - sequenceNumber
        }
    }

    override fun handleScannedQRCode(contents: String) {
        if (sequenceNumber.toString() == contents) {
            sendNextChunk()
        }
    }
}


