package com.example.qrft

import android.graphics.Bitmap
import android.graphics.Color
import com.example.qrft.databinding.ActivityMainBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QRCodeGenerator(private val binding: ActivityMainBinding) {
    companion object {
        private const val QRCODE_SIZE = 1000
    }

    fun generate(contents: String) {
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