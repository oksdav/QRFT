package com.example.qrft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.qrft.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageAnalysis: ImageAnalysis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!allPermissionsGranted()) {
            requestPermission.launch(REQUIRED_PERMISSIONS)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageAnalysis = ImageAnalysis.Builder().build()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        binding.receiveFile.visibility = View.GONE
        binding.sendFile.visibility = View.GONE

        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

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
                Log.e(toString(), "Use case binding failed", ex)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun onReceive(@Suppress("UNUSED_PARAMETER") view: View) {
        if (allPermissionsGranted()) {
            binding.qrcode.setImageDrawable(null)
            startAnalyzer(Receiver(binding, baseContext, imageAnalysis))
        } else {
            requestPermission.launch(REQUIRED_PERMISSIONS)
        }
    }

    fun onSend(@Suppress("UNUSED_PARAMETER") view: View) {
        if (allPermissionsGranted()) {
            binding.qrcode.setImageDrawable(null)
            getContent.launch("*/*")
        } else {
            requestPermission.launch(REQUIRED_PERMISSIONS)
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) {
        startAnalyzer(Sender(binding, baseContext, imageAnalysis, it))
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.any { permission -> !permission.value }) {
            finish()
        }
    }
}