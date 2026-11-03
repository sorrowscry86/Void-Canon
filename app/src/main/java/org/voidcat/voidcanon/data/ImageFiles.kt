package org.voidcat.voidcanon.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.util.UUID

object ImageFiles {
    const val MAX_FILE_BYTES = 16L * 1024 * 1024
    const val MAX_PIXELS = 24_000_000L

    /** Normalize orientation, dimensions, and format; never retain a fragile picker URI. */
    fun import(input: InputStream, scratch: File, images: File): Pair<File, Boolean> {
        val source = File(scratch, "source-${UUID.randomUUID()}")
        try {
            input.use { stream -> source.outputStream().use { out -> copyLimited(stream, out, MAX_FILE_BYTES) } }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Choose a supported image file." }
            require(bounds.outWidth.toLong() * bounds.outHeight <= MAX_PIXELS) { "Image is too large (maximum 24 megapixels)." }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
            val decoded = requireNotNull(BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })) {
                "The image could not be decoded."
            }
            val orientation = runCatching { ExifInterface(source).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
            val matrix = Matrix().apply {
                when (orientation) {
                    2 -> setScale(-1f, 1f)
                    3 -> setRotate(180f)
                    4 -> setScale(1f, -1f)
                    5 -> { setRotate(90f); postScale(-1f, 1f) }
                    6 -> setRotate(90f)
                    7 -> { setRotate(270f); postScale(-1f, 1f) }
                    8 -> setRotate(270f)
                }
            }
            val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val transparent = hasTransparentPixels(bitmap)
            val target = File(images, "${UUID.randomUUID()}.png")
            try {
                target.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Could not store image." } }
            } catch (e: Exception) { target.delete(); throw e }
            finally { if (bitmap !== decoded) bitmap.recycle(); decoded.recycle() }
            return target to transparent
        } finally { source.delete() }
    }

    fun validate(file: File): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        require(options.outWidth in 1..4096 && options.outHeight in 1..4096 &&
            options.outWidth.toLong() * options.outHeight <= MAX_PIXELS) { "Backup contains an invalid or oversized image." }
        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path)) { "Backup contains an unreadable image." }
        return try { hasTransparentPixels(bitmap) } finally { bitmap.recycle() }
    }

    private fun hasTransparentPixels(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            if (row.any { (it ushr 24) < 250 }) return true
        }
        return false
    }
}
