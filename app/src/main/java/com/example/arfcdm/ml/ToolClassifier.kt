package com.example.arfcdm.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ClassificationResult(
    val label: String,
    val confidence: Float
)

class ToolClassifier(context: Context) {

    private var interpreter: Interpreter? = null
    private val labels = listOf(
        "Martelo",
        "Alicate",
        "Chave de Boca",
        "Chave de Fenda",
        "Chave Phillips",
        "Desconhecido"
    )

    companion object {
        private const val INPUT_SIZE = 224
        private const val PIXEL_SIZE = 3
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }

    init {
        interpreter = null
    }

    fun classify(bitmap: Bitmap): ClassificationResult {
        return simulateClassification(bitmap)
    }

    private fun simulateClassification(bitmap: Bitmap): ClassificationResult {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L

        pixels.forEach { pixel ->
            redSum += (pixel shr 16 and 0xFF)
            greenSum += (pixel shr 8 and 0xFF)
            blueSum += (pixel and 0xFF)
        }

        val totalPixels = pixels.size
        val avgRed = redSum / totalPixels
        val avgGreen = greenSum / totalPixels
        val avgBlue = blueSum / totalPixels

        val tool = when {
            avgRed > avgGreen && avgRed > avgBlue -> "Martelo"
            avgBlue > avgRed && avgBlue > avgGreen -> "Alicate"
            avgGreen > avgRed -> "Chave de Boca"
            else -> labels.random()
        }

        return ClassificationResult(
            label = tool,
            confidence = (0.6f + Math.random().toFloat() * 0.3f)
        )
    }

    fun close() {
        interpreter?.close()
    }
}