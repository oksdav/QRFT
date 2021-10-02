package com.example.qrft

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import com.example.qrft.databinding.ActivityMainBinding
import java.io.File
import java.io.OutputStream

class Receiver(
    binding: ActivityMainBinding,
    private val context: Context,
    private val imageAnalysis: ImageAnalysis,
) : QRCodeHandler(binding) {
    companion object {
        private const val FIRST_SEQUENCE_NUMBER = 2
        private const val LAST_SEQUENCE_NUMBER = 3
    }

    private lateinit var outputStream: OutputStream
    private var isFileCreated = false
    private var sequenceNumber = 0

    override fun handleScannedQRCode(contents: String) {
        val (sequenceNumber, data) = extractSequenceNumber(contents)
        Toast.makeText(
            context,
            "Sequence number: $sequenceNumber | Data: $data",
            Toast.LENGTH_SHORT
        ).show()
        when (sequenceNumber) {
            FIRST_SEQUENCE_NUMBER -> {
                if (!isFileCreated) {
                    createFile(data)
                }
            }
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

    private fun createFile(name: String) {
        val downloadPath: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val filename = File(name)

        var file = File(downloadPath, name)
        isFileCreated = file.createNewFile()
        var fileDuplicateNameNumber = 1

        while (!isFileCreated) {
            file = File(downloadPath, "${filename.nameWithoutExtension}-$fileDuplicateNameNumber.${filename.extension}")
            isFileCreated = file.createNewFile()
            fileDuplicateNameNumber++
        }

        outputStream = file.outputStream()
        generateQRCode(sequenceNumber.toString())
    }

    private fun handleChunk(data: String, nextSequenceNumber: Int) {
        outputStream.write(data.toByteArray())
        sequenceNumber = nextSequenceNumber
        generateQRCode(sequenceNumber.toString())
    }
}