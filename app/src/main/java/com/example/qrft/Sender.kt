package com.example.qrft

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.camera.core.ImageAnalysis
import com.example.qrft.databinding.ActivityMainBinding
import java.io.InputStream

class Sender(
    binding: ActivityMainBinding,
    context: Context,
    imageAnalysis: ImageAnalysis,
    uri: Uri
) : Communicator(binding, imageAnalysis) {
    companion object {
        private const val FILE_CHUNK_SIZE = 320
    }

    private var inputStream: InputStream
    private var sequenceNumber = 1

    init {
        val contentResolver = context.contentResolver

        inputStream = contentResolver.openInputStream(uri)!!

        contentResolver.query(uri, null, null, null, null)?.use {
            it.moveToFirst()
            val fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            generateQRCode(FIRST_SEQUENCE_NUMBER.toString() + fileName)
        }
    }

    override fun handleScannedQRCode(contents: String) {
        when (contents.toInt()) {
            LAST_SEQUENCE_NUMBER -> finishFileTransfer()
            (1 - sequenceNumber) -> sendFileChunk()
        }
    }

    private fun sendFileChunk() {
        val (fileChunk, endOfFile) = readFileChunk()

        sequenceNumber = if (endOfFile) {
            LAST_SEQUENCE_NUMBER
        } else {
            1 - sequenceNumber
        }

        generateQRCode(sequenceNumber.toString() + fileChunk)
    }

    private fun readFileChunk(): Pair<String, Boolean> {
        val fileChunk = ByteArray(FILE_CHUNK_SIZE)
        val endOfFile = inputStream.read(fileChunk, 0, FILE_CHUNK_SIZE) == -1
        return Pair(Base64.encodeToString(fileChunk, Base64.DEFAULT), endOfFile)
    }
}