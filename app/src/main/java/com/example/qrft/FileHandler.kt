package com.example.qrft


import android.content.Context
import java.io.File


class FileHandler(
    context: Context,
    private val FILE_PATH_NAME: String = context.filesDir.path + "/",
){
    fun saveTextFile(fileName: String, text: String): Boolean {
        val fullFileName = FILE_PATH_NAME + fileName
        val file = File(fullFileName)
        if(!file.isDirectory) {
            file.writeText(text)
        }
        return file.isFile
    }

    fun readTextFile(fileName: String): String? {
        val fullFileName = FILE_PATH_NAME + fileName
        val file = File(fullFileName)
        return if(file.exists() && file.isFile) {
            file.readText()
        } else {
            null
        }
    }
}