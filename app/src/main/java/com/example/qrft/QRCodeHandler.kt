package com.example.qrft

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.Toast
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

abstract class QRCodeHandler(
    private val binding: ActivityMainBinding,
    private val context: Context,
    private val imageAnalysis: ImageAnalysis
) : ImageAnalysis.Analyzer {
    companion object {
        const val FIRST_SEQUENCE_NUMBER = 2
        const val LAST_SEQUENCE_NUMBER = 3
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
                        barcode.rawValue?.let {
                            try {
                                handleScannedQRCode(it)
                            } catch (ex: Exception) {
                                Log.e(toString(), "Error while handling scanned QR code", ex)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(toString(), "Analyze failed", it)
                    image.close()
                }
                .addOnCompleteListener {
                    Log.d(toString(), "Analyze completed")
                    image.close()
                }
        }
    }

    protected abstract fun handleScannedQRCode(contents: String)

    protected fun generateQRCode(contents: String) {
        val qrCodeSize = context.resources.displayMetrics.widthPixels
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L
        )
        val bitMatrix = QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize, hints)
        val bitmap = Bitmap.createBitmap(qrCodeSize, qrCodeSize, Bitmap.Config.RGB_565)
        for (y in 0 until qrCodeSize) {
            for (x in 0 until qrCodeSize) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        binding.qrcode.setImageBitmap(bitmap)
    }

    protected fun finishFileTransfer() {
        imageAnalysis.clearAnalyzer()
        binding.receiveFile.visibility = View.VISIBLE
        binding.sendFile.visibility = View.VISIBLE
        Toast.makeText(context, "Finished transferring file", Toast.LENGTH_LONG).show()
    }
}