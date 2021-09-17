package com.example.qrft


import android.content.Context
import java.io.File


class FileHandler(
    context: Context,
    private val FILE_PATH_NAME: String = context.filesDir.path + "/"
){
    fun saveTextFile(fileName: String, text: String) {
        val fullFileName = FILE_PATH_NAME + fileName
        File(fullFileName).writeText(text)
    }

    fun readTextFile(fileName: String): String? {
        val fullFileName = FILE_PATH_NAME + fileName
        return if(File(fullFileName).exists()) {
            File(fullFileName).readText()
        } else {
            null
        }
    }
}