package com.example.qrft

import com.example.qrft.databinding.ActivityMainBinding
import java.io.File

class Sender(
    binding: ActivityMainBinding
) : QRCodeHandler(binding) {
    companion object {
        private const val TITLE_SEQUENCE_NUMBER = 3
        private const val LAST_SEQUENCE_NUMBER = 2
        private const val QRCODE_SIZE = 40
    }

    private var sequenceNumber = 1
    private var currChunkNumber = 0
    private lateinit var file : File


    fun send(file: File) {
        this.file = file
        this.generateQRCode(TITLE_SEQUENCE_NUMBER.toString() + file.name)
    }

    override fun handleScannedQRCode(contents: String) {
        if (sequenceNumber.toString() != contents) {
            sendNextChunk()
        }
    }

    private fun sendNextChunk() {
        val offset = currChunkNumber * QRCODE_SIZE
        setSequenceNumber(offset)
        this.generateQRCode(sequenceNumber.toString() + readTextChunk(file, offset))

        currChunkNumber ++
    }

    private fun setSequenceNumber(offset: Int) {
        sequenceNumber = if (file.length() <= offset + QRCODE_SIZE) {
            LAST_SEQUENCE_NUMBER
        } else {
            1 - sequenceNumber
        }
    }

    private fun readTextChunk(file: File, offset: Int): String {
        val retChunk: CharArray = charArrayOf()
        if(file.exists() && file.isFile) {
            file.bufferedReader().read(retChunk, offset, QRCODE_SIZE)
        }
        return retChunk.toString()
    }
}


