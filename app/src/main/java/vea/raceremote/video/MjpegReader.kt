package vea.raceremote.video

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.util.Locale

/** Bounded subset used by ESP32 CameraWebServer: multipart JPEG with Content-Length. */
class MjpegReader(input: InputStream, contentType: String) {
    private val input = input.buffered()
    private val delimiter: String

    init {
        if (!contentType.substringBefore(';').trim().equals("multipart/x-mixed-replace", true))
            throw IOException("Expected multipart MJPEG")
        val boundary = contentType.split(';').drop(1).map { it.trim() }
            .firstOrNull { it.substringBefore('=').equals("boundary", true) }
            ?.substringAfter('=', "")?.trim()?.removeSurrounding("\"")
            ?: throw IOException("Missing boundary")
        if (boundary.isEmpty() || boundary.length > 70 || boundary.any { it.code !in 33..126 })
            throw IOException("Invalid boundary")
        delimiter = "--$boundary"
    }

    fun nextFrame(): ByteArray? {
        var line = readLine() ?: return null
        var emptyLines = 0
        while (line.isEmpty() && emptyLines++ < 2) line = readLine() ?: throw EOFException()
        if (line == "$delimiter--") return null
        if (line != delimiter) throw IOException("Invalid frame boundary")
        val headers = mutableMapOf<String, String>()
        var headerBytes = 0
        while (true) {
            line = readLine() ?: throw EOFException("Incomplete headers")
            headerBytes += line.length + 2
            if (headerBytes > 16_384) throw IOException("Headers too large")
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon < 1) throw IOException("Invalid header")
            val name = line.substring(0, colon).trim().lowercase(Locale.ROOT)
            if (headers.put(name, line.substring(colon + 1).trim()) != null)
                throw IOException("Duplicate header")
        }
        if (!headers["content-type"].equals("image/jpeg", true)) throw IOException("Expected JPEG part")
        val length = headers["content-length"]?.toIntOrNull() ?: throw IOException("Missing JPEG length")
        if (length !in 4..524_288) throw IOException("JPEG exceeds frame size limit")
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(bytes, offset, length - offset)
            if (count < 0) throw EOFException("Incomplete JPEG")
            offset += count
        }
        if (bytes[0] != 0xff.toByte() || bytes[1] != 0xd8.toByte() ||
            bytes[length - 2] != 0xff.toByte() || bytes[length - 1] != 0xd9.toByte())
            throw IOException("Invalid JPEG markers")
        return bytes
    }

    private fun readLine(): String? {
        val line = StringBuilder()
        while (line.length <= 4096) {
            val byte = input.read()
            if (byte < 0) {
                if (line.isEmpty()) return null
                throw EOFException("Incomplete line")
            }
            if (byte == 10) return line.toString().removeSuffix("\r")
            line.append(byte.toChar())
        }
        throw IOException("Line too long")
    }
}
