package com.example.qrft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.qrft.databinding.ActivityMainBinding
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var fileHandler: FileHandler
    private lateinit var titleText: EditText
    private lateinit var editText: EditText
    private lateinit var chosenFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fileHandler = FileHandler(baseContext)
        editText = findViewById(R.id.editText)
        titleText = findViewById(R.id.titleText)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(imageAnalysis)
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera(imageAnalysis: ImageAnalysis) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, imageAnalysis)
            } catch (ex: Exception) {
                Log.e(this.toString(), "Use case binding failed", ex)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun saveTextFile(@Suppress("UNUSED_PARAMETER") view: View) {
        if(fileHandler.saveTextFile(titleText.text.toString() ,editText.text.toString())) {
            Toast.makeText(baseContext, "File saved successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(baseContext, "File could not be saved!", Toast.LENGTH_SHORT).show()
        }
    }

    fun readTextFile(@Suppress("UNUSED_PARAMETER") view: View) {
        val readText = fileHandler.readTextFile(titleText.text.toString())
        if(readText.equals(null)) {
            Toast.makeText(baseContext, "Error, could not read file!", Toast.LENGTH_SHORT).show()
        } else {
            editText.setText(readText)
        }
    }

    fun onSend(@Suppress("UNUSED_PARAMETER") view: View) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageAnalysis = ImageAnalysis.Builder().build()

        val sender = Sender(binding, baseContext)
        imageAnalysis.setAnalyzer(cameraExecutor, sender)

        if (allPermissionsGranted()) {
            if(this::chosenFile.isInitialized && chosenFile.isFile) {
                sender.send(chosenFile.name)
                startCamera(imageAnalysis)
            } else {
                Toast.makeText(baseContext, "Please choose a file to send", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }


        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun onReceive(@Suppress("UNUSED_PARAMETER") view: View) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageAnalysis = ImageAnalysis.Builder().build()
        val receiver = Receiver(binding, baseContext, imageAnalysis)
        imageAnalysis.setAnalyzer(cameraExecutor, receiver)

        if (allPermissionsGranted()) {
            startCamera(imageAnalysis)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun onChoose(view: View) {
        chooseFile(view)
    }

     private fun chooseFile(@Suppress("UNUSED_PARAMETER") view: View) {
        val fileChooser = FileChooser(this@MainActivity)
        fileChooser.setFileListener { file ->
            chosenFile = file
            titleText.setText(chosenFile.name.toString())
        }
        fileChooser.showDialog()
    }
}