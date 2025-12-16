package com.caloracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for image processing and conversion.
 * Handles base64 encoding, compression, and rotation for Claude API.
 */
object ImageUtils {

    /**
     * Maximum image dimension (width or height) before compression.
     * Reduces API payload size while maintaining image quality.
     */
    private const val MAX_DIMENSION = 1024

    /**
     * JPEG compression quality (0-100).
     */
    private const val JPEG_QUALITY = 85

    /**
     * Convert an image file to base64 string.
     * Automatically compresses and rotates the image as needed.
     *
     * @param context Android context
     * @param imageUri URI of the image file
     * @return Base64-encoded image string, or null if conversion fails
     */
    fun convertImageToBase64(context: Context, imageUri: Uri): String? {
        return try {
            // Read image from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Get rotation from EXIF data
            val rotation = getRotationFromExif(context, imageUri)

            // Rotate if needed
            val rotatedBitmap = if (rotation != 0) {
                rotateBitmap(bitmap, rotation)
            } else {
                bitmap
            }

            // Compress image
            val compressedBitmap = compressBitmap(rotatedBitmap)

            // Convert to base64
            bitmapToBase64(compressedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convert a bitmap to base64 string.
     *
     * @param bitmap The bitmap to convert
     * @param quality JPEG quality (0-100)
     * @return Base64-encoded string
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = JPEG_QUALITY): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Compress a bitmap to reduce size while maintaining quality.
     * Scales down if dimensions exceed MAX_DIMENSION.
     *
     * @param bitmap The bitmap to compress
     * @return Compressed bitmap
     */
    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate if scaling is needed
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return bitmap
        }

        // Calculate scale factor
        val scaleFactor = if (width > height) {
            MAX_DIMENSION.toFloat() / width
        } else {
            MAX_DIMENSION.toFloat() / height
        }

        // Create scaled bitmap
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Rotate a bitmap by the specified degrees.
     *
     * @param bitmap The bitmap to rotate
     * @param degrees Rotation degrees
     * @return Rotated bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Get rotation from EXIF data.
     * Fixes orientation issues from camera photos.
     *
     * @param context Android context
     * @param imageUri URI of the image
     * @return Rotation degrees (0, 90, 180, or 270)
     */
    private fun getRotationFromExif(context: Context, imageUri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            when (exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Save bitmap to file.
     *
     * @param bitmap The bitmap to save
     * @param file The destination file
     * @return True if successful, false otherwise
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            outputStream.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get media type from file extension.
     *
     * @param fileName The file name
     * @return Media type string (e.g., "image/jpeg")
     */
    fun getMediaType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg" // Default to JPEG
        }
    }
}
