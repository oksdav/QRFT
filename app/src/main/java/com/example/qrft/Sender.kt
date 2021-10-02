package com.example.qrft

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.camera.core.ImageAnalysis
import com.example.qrft.databinding.ActivityMainBinding
import java.io.BufferedReader

class Sender(
    binding: ActivityMainBinding,
    private val context: Context,
    private val imageAnalysis: ImageAnalysis,
    private val uri: Uri
) : QRCodeHandler(binding) {
    companion object {
        private const val FIRST_SEQUENCE_NUMBER = 2
        private const val LAST_SEQUENCE_NUMBER = 3
        private const val QRCODE_SIZE = 40
    }

    private lateinit var bufferedReader: BufferedReader
    private var fileSize: Long = 0
    private var sequenceNumber = 1
    private var currChunkNumber = 0

    @SuppressLint("Range")
    fun send() {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            bufferedReader = inputStream.bufferedReader()

            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
                cursor.close()
                this.generateQRCode(FIRST_SEQUENCE_NUMBER.toString() + filename)
            }
        }
    }

    override fun handleScannedQRCode(contents: String) {
        if (sequenceNumber.toString() != contents) {
            sendNextChunk()
        }
    }

    private fun sendNextChunk() {
        val offset = currChunkNumber * QRCODE_SIZE
        setSequenceNumber(offset)
        this.generateQRCode(sequenceNumber.toString() + readTextChunk(offset))
        currChunkNumber++
    }

    private fun setSequenceNumber(offset: Int) {
        sequenceNumber = if (fileSize <= offset + QRCODE_SIZE) {
            imageAnalysis.clearAnalyzer()
            LAST_SEQUENCE_NUMBER
        } else {
            1 - sequenceNumber
        }
    }

    private fun readTextChunk(offset: Int): String {
        val fileChunk = CharArray(QRCODE_SIZE)
        bufferedReader.read(fileChunk, offset, QRCODE_SIZE)
        return String(fileChunk).trimEnd('\u0000')
    }
}


