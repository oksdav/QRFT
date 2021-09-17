package com.example.qrft

import android.content.Context
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import com.example.qrft.databinding.ActivityMainBinding
import java.io.File

class Receiver(
    binding: ActivityMainBinding,
    private val context: Context,
    private val imageAnalysis: ImageAnalysis
) : QRCodeHandler(binding) {
    companion object {
        private const val LAST_SEQUENCE_NUMBER = 2
    }

    private var isHandledTitle = false
    private var sequenceNumber = 0
    private lateinit var file: File

    override fun handleScannedQRCode(contents: String) {
        val (sequenceNumber, data) = extractSequenceNumber(contents)
        Toast.makeText(context, "Sequence number: $sequenceNumber | Data: $data", Toast.LENGTH_SHORT).show()
        if (!isHandledTitle) {
            handleTitle(contents)
        } else when (sequenceNumber) {
            LAST_SEQUENCE_NUMBER -> {
                handleChunk(data, LAST_SEQUENCE_NUMBER)
                Toast.makeText(context, "Finished reading file", Toast.LENGTH_LONG).show()
                imageAnalysis.clearAnalyzer()
            }
            this.sequenceNumber -> {
                handleChunk(data, 1 - sequenceNumber)
            }
        }
    }

    private fun extractSequenceNumber(contents: String): Pair<Int, String> {
        return Pair(contents.take(1).toInt(), contents.drop(1))
    }

    private fun handleTitle(fileTitle: String) {
        this.file = File(fileTitle)
        this.isHandledTitle = true
    }

    private fun handleChunk(data: String, nextSequenceNumber: Int) {
        saveTextChunk(file, data)
        this.sequenceNumber = nextSequenceNumber
        generateQRCode(nextSequenceNumber.toString())
    }

    private fun saveTextChunk(file: File, textChunk: String) {
        if(!file.isFile && !file.isDirectory) {
            file.createNewFile()
        }
        file.outputStream().write(textChunk.toByteArray())
    }
}