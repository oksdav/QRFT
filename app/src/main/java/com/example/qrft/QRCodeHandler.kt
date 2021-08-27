package com.example.qrft

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.qrft.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

abstract class QRCodeHandler(private val binding: ActivityMainBinding) : ImageAnalysis.Analyzer {
    companion object {
        private const val QRCODE_SIZE = 1000
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()

            BarcodeScanning.getClient(options).process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { handleScannedQRCode(it) }
                    }
                }
                .addOnFailureListener {
                    Log.e(this.toString(), "Analyze failed", it)
                    image.close()
                }
                .addOnCompleteListener {
                    Log.i(this.toString(), "Analyze completed")
                    image.close()
                }
        }
    }

    abstract fun handleScannedQRCode(contents: String)

    fun generateQRCode(contents: String) {
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
}