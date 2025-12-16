package com.caloracker.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.caloracker.BuildConfig
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

/**
 * Service for on-device food detection using TensorFlow Lite.
 * Processes images locally without requiring internet connection.
 */
class FoodDetectionService(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var labels: List<String> = emptyList()

    // Model configuration
    private var inputImageWidth = 224
    private var inputImageHeight = 224
    private val pixelSize = 3 // RGB
    private val imageMean = 127.5f
    private val imageStd = 127.5f

    // Results configuration
    private val maxResults = 5
    private val confidenceThreshold = 0.1f

    companion object {
        private const val TAG = "FoodDetectionService"
    }

    /**
     * Data class representing a food prediction result
     */
    data class FoodPrediction(
        val label: String,
        val confidence: Float,
        val displayName: String = label.replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.capitalize() }
    )

    /**
     * Initialize the TensorFlow Lite interpreter and load labels
     */
    fun initialize() {
        try {
            // Load labels from assets
            labels = loadLabels()
            Log.d(TAG, "Loaded ${labels.size} food labels")

            // Load TensorFlow Lite model
            val modelBuffer = loadModelFile()

            // Configure interpreter options
            val options = Interpreter.Options()

            // Try to use GPU delegate if available
            val compatibilityList = CompatibilityList()
            if (compatibilityList.isDelegateSupportedOnThisDevice) {
                gpuDelegate = GpuDelegate(compatibilityList.bestOptionsForThisDevice)
                options.addDelegate(gpuDelegate)
                Log.d(TAG, "GPU delegate enabled for faster inference")
            } else {
                // Use multiple threads on CPU
                options.setNumThreads(4)
                Log.d(TAG, "Using CPU with 4 threads")
            }

            // Create interpreter
            interpreter = Interpreter(modelBuffer, options)

            // Get input shape to determine image size
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            if (inputShape != null && inputShape.size >= 3) {
                inputImageHeight = inputShape[1]
                inputImageWidth = inputShape[2]
                Log.d(TAG, "Model input size: ${inputImageWidth}x${inputImageHeight}")
            }

            Log.d(TAG, "TensorFlow Lite model initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TensorFlow Lite model", e)
            throw e
        }
    }

    /**
     * Classify a food image and return top predictions
     */
    fun classifyImage(bitmap: Bitmap): List<FoodPrediction> {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized. Call initialize() first.")
            return emptyList()
        }

        try {
            // Preprocess image
            val inputBuffer = preprocessImage(bitmap)

            // Run inference
            val outputArray = Array(1) { FloatArray(labels.size) }
            interpreter?.run(inputBuffer, outputArray)

            // Process results
            val predictions = processOutput(outputArray[0])

            Log.d(TAG, "Classification completed. Top prediction: ${predictions.firstOrNull()?.label}")
            return predictions
        } catch (e: Exception) {
            Log.e(TAG, "Error during image classification", e)
            return emptyList()
        }
    }

    /**
     * Preprocess bitmap for TensorFlow Lite model input
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize bitmap to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            inputImageWidth,
            inputImageHeight,
            true
        )

        // Allocate ByteBuffer for model input
        val byteBuffer = ByteBuffer.allocateDirect(
            4 * inputImageHeight * inputImageWidth * pixelSize
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        // Convert bitmap to float array and normalize
        val intValues = IntArray(inputImageWidth * inputImageHeight)
        resizedBitmap.getPixels(
            intValues,
            0,
            resizedBitmap.width,
            0,
            0,
            resizedBitmap.width,
            resizedBitmap.height
        )

        var pixel = 0
        for (i in 0 until inputImageHeight) {
            for (j in 0 until inputImageWidth) {
                val value = intValues[pixel++]

                // Extract RGB values and normalize to [-1, 1]
                byteBuffer.putFloat(((value shr 16 and 0xFF) - imageMean) / imageStd)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - imageMean) / imageStd)
                byteBuffer.putFloat(((value and 0xFF) - imageMean) / imageStd)
            }
        }

        return byteBuffer
    }

    /**
     * Process model output and return top predictions
     */
    private fun processOutput(output: FloatArray): List<FoodPrediction> {
        // Create list of predictions with labels
        val predictions = output.mapIndexed { index, confidence ->
            FoodPrediction(
                label = labels.getOrElse(index) { "unknown_$index" },
                confidence = confidence
            )
        }

        // Sort by confidence and filter by threshold
        return predictions
            .filter { it.confidence >= confidenceThreshold }
            .sortedByDescending { it.confidence }
            .take(maxResults)
    }

    /**
     * Load TensorFlow Lite model from assets
     */
    private fun loadModelFile(): MappedByteBuffer {
        val modelFileName = BuildConfig.TFLITE_MODEL_FILE
        val assetFileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Load food labels from assets
     */
    private fun loadLabels(): List<String> {
        val labelsFileName = BuildConfig.TFLITE_LABELS_FILE
        return context.assets.open(labelsFileName).bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
        Log.d(TAG, "TensorFlow Lite resources released")
    }
}
