package maestro

import okio.Source

class NamedSource(val name: String, val source: Source, val extension: String, val path: String)

enum class MediaExt(val extName: String) {
    PNG("png"),
    JPEG("jpeg"),
    JPG("jpg"),
    GIF("gif"),
    MP4("mp4"),
    MP3("mp3")
}

fun String.isMediaFile(): Boolean {
    return this == MediaExt.MP3.extName ||
            this == MediaExt.PNG.extName ||
            this == MediaExt.JPEG.extName ||
            this == MediaExt.JPG.extName ||
            this == MediaExt.GIF.extName ||
            this == MediaExt.MP4.extName ||
            this == MediaExt.MP3.extName
}