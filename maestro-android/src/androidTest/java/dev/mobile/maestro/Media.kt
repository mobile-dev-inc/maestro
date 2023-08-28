package dev.mobile.maestro

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.OutputStream

object MediaStorage {

    fun getOutputStream(mediaName: String): OutputStream? {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues()
        contentValues.apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, mediaName)
            put(MediaStore.MediaColumns.MIME_TYPE, Service.FileType.PNG.mimeType)
        }
        val contentResolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val outputStream = contentResolver.insert(uri, contentValues)?.let {
            contentResolver.openOutputStream(it)
        }
        return outputStream
    }
}