package com.example.qrft

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.qrft.databinding.ActivityMainBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val QRCODE_SIZE = 1000
        private const val LAST_SEQUENCE_NUMBER = 2
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    private var sequenceNumber = 0
    private var dummyFile = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val imageAnalyzer = ImageAnalysis.Builder().build()
            imageAnalyzer.setAnalyzer(cameraExecutor, QRCodeAnalyzer { handleScannedQRCode(it) })

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, imageAnalyzer)
            } catch (ex: Exception) {
                Log.e(this.toString(), "Use case binding failed", ex)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun generateQRCode(contents: String) {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        )
        val bitMatrix = QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE, hints)
        val bitmap = Bitmap.createBitmap(QRCODE_SIZE, QRCODE_SIZE, Bitmap.Config.RGB_565)
        for (y in 0 until QRCODE_SIZE) {
            for (x in 0 until QRCODE_SIZE) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        binding.qrcode.setImageBitmap(bitmap)
    }

    private fun handleScannedQRCode(contents: String) {
        val (sequenceNumber, data) = extractSequenceNumber(contents)
        Toast.makeText(this, "Sequence number: $sequenceNumber | Data: $data", Toast.LENGTH_SHORT).show()

        when (sequenceNumber) {
            LAST_SEQUENCE_NUMBER -> {
                handleChunk(data, LAST_SEQUENCE_NUMBER)
                Toast.makeText(baseContext, dummyFile, Toast.LENGTH_LONG).show()
                cameraExecutor.shutdown()
            }
            this.sequenceNumber -> {
                handleChunk(data, 1 - sequenceNumber)
            }
        }
    }

    private fun extractSequenceNumber(contents: String): Pair<Int, String> {
        return Pair(contents.take(1).toInt(), contents.drop(1))
    }

    private fun handleChunk(data: String, nextSequenceNumber: Int) {
        saveToFile(data)
        this.sequenceNumber = nextSequenceNumber
        generateQRCode(nextSequenceNumber.toString())
    }

    private fun saveToFile(data: String) {
        //TODO
        dummyFile += data
    }
}