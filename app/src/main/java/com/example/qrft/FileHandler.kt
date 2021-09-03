package com.example.qrft


import android.content.Context
import android.widget.Toast
import java.io.*


class FileHandler (
    private val context: Context
){
    fun saveTextFile(title: String, text: String) {
        try {
            val fileOutputStream: FileOutputStream =
                context.openFileOutput(title, Context.MODE_PRIVATE)
            val outputWriter = OutputStreamWriter(fileOutputStream)
            outputWriter.write(text)
            outputWriter.close()
            Toast.makeText(context, "File saved successfully!", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            Toast.makeText(context, "Error, could not save file!", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    fun readTextFile(title: String): String? {
        try {
            val fileInputStream: FileInputStream = context.openFileInput(title)
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder: StringBuilder = StringBuilder()
            var text: String?
            while(run {
                    text = bufferedReader.readLine()
                    text
                } != null) {
                stringBuilder.append(text)
            }
            return stringBuilder.toString()
        }
        catch (e: Exception) {
            Toast.makeText(context, "Error, could not read file!", Toast.LENGTH_SHORT).show()
            e.printStackTrace()

        }
        return null
    }
}