package com.example.qrft

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import com.example.qrft.databinding.ActivityMainBinding
import java.io.File
import java.io.OutputStream

class Receiver(
    binding: ActivityMainBinding,
    private val context: Context,
    imageAnalysis: ImageAnalysis
) : Communicator(binding, context, imageAnalysis) {
    private lateinit var outputStream: OutputStream
    private var isFileCreated = false
    private var sequenceNumber = 0

    override fun handleScannedQRCode(contents: String) {
        Log.i("Contents", contents)
        val data = contents.drop(1)
        when (contents.take(1).toInt()) {
            FIRST_SEQUENCE_NUMBER -> createFile(data)
            LAST_SEQUENCE_NUMBER -> finishFileTransfer(data)
            sequenceNumber -> receiveFileChunk(data)
        }
    }

    private fun createFile(name: String) {
        if (!isFileCreated) {
            isFileCreated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createFileNew(name)
            } else {
                createFileOld(name)
            }

            if (isFileCreated) {
                generateQRCode(sequenceNumber.toString())
            }
        }
    }

    private fun createFileOld(name: String): Boolean {
        val downloadPath: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileName = getUnusedFileName(name, downloadPath.listFiles()!!.toList())
        outputStream = File(downloadPath, fileName).outputStream()
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createFileNew(name: String): Boolean {
        val contentResolver = context.contentResolver

        val fileList = mutableListOf<File>()
        contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads.DISPLAY_NAME),
            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf("$name%"),
            null
        )?.use {
            val nameColumn = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            while (it.moveToNext()) {
                fileList += File(it.getString(nameColumn))
            }
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let {
            outputStream = contentResolver.openOutputStream(it) ?: return false
        }
        return true
    }

    private fun getUnusedFileName(fullName: String, files: List<File>): String {
        val file = File(fullName)
        val fileName = file.nameWithoutExtension
        val fileExtension = file.extension
        val fileNameList = files.filter { it.extension == fileExtension }.map { it.nameWithoutExtension }
        return if (!fileNameList.contains(fileName)) {
            fullName
        } else {
            var duplicateFileNameNumber = 1
            while (fileNameList.contains("$fileName-$duplicateFileNameNumber")) {
                duplicateFileNameNumber++
            }
            "$fileName-$duplicateFileNameNumber.$fileExtension"
        }
    }

    private fun finishFileTransfer(data: String) {
        if (!isFileCreated) {
            Log.e(toString(), "File does not exist")
        } else {
            outputStream.write(Base64.decode(data, Base64.DEFAULT))
            generateQRCode(LAST_SEQUENCE_NUMBER.toString())
        }
        super.finishFileTransfer()
    }

    private fun receiveFileChunk(data: String) {
        if (!isFileCreated) {
            Log.e(toString(), "File does not exist")
        } else {
            outputStream.write(Base64.decode(data, Base64.DEFAULT))
            sequenceNumber = 1 - sequenceNumber
            generateQRCode(sequenceNumber.toString())
        }
    }
}