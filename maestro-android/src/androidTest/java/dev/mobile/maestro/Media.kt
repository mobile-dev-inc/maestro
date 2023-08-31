package dev.mobile.maestro

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.OutputStream

object MediaStorage {

    fun getOutputStream(mediaName: String, mediaExt: String): OutputStream? {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val ext = Service.FileType.values().first { it.ext == mediaExt }
        val contentValues = ContentValues()
        contentValues.apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, mediaName)
            put(MediaStore.MediaColumns.MIME_TYPE, ext.mimeType)
        }
        val contentResolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val outputStream = contentResolver.insert(uri, contentValues)?.let {
            contentResolver.openOutputStream(it)
        }
        return outputStream
    }
}