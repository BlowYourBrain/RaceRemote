package vea.raceremote.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import java.util.concurrent.Executors
import okhttp3.OkHttpClient

/** At most one waiting JPEG and one latest bitmap. Never queues UI frame callbacks. */
class MjpegVideoView(context: Context) : View(context) {
    private data class Encoded(val bytes: ByteArray, val generation: Long, val receivedMs: Long)
    private data class Decoded(val bitmap: Bitmap, val sequence: Long, val receivedMs: Long)
    private val gate = Any()
    private val decoder = Executors.newSingleThreadExecutor()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val background = Paint().apply { color = Color.BLACK }
    private var pending: Encoded? = null
    private var decoding = false
    private var generation = 0L
    private var stream: MjpegStream? = null
    private var latest: Decoded? = null
    private var sequence = 0L
    private var drawnSequence = -1L
    private var drawnFrames = 0
    private var receivedFrames = 0
    private var decodedFrames = 0
    private var statsAt = 0L
    private var startedAt = 0L
    private var lastReceivedAt = 0L
    private var disposed = false
    var onStatus: (String, Boolean) -> Unit = { _, _ -> }

    private val heartbeat = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            synchronized(gate) {
                if (stream == null) return
                val deadline = if (lastReceivedAt == 0L) startedAt + 4000 else lastReceivedAt + 1500
                if (now >= deadline) {
                    stop("Нет свежего видео"); return
                }
                if (now - statsAt >= 1000) {
                    val seconds = (now - statsAt) / 1000.0
                    val message = "Получено %.1f · отрисовано %.1f кадр/с".format(receivedFrames / seconds, drawnFrames / seconds)
                    onStatus(message, true)
                    android.util.Log.d("RaceRemoteVideo", "fps received=${receivedFrames / seconds} decoded=${decodedFrames / seconds} drawn=${drawnFrames / seconds}")
                    drawnFrames = 0; receivedFrames = 0; decodedFrames = 0; statsAt = now
                }
            }
            postDelayed(this, 250)
        }
    }

    fun start(url: String, http: OkHttpClient) {
        stop()
        synchronized(gate) {
            if (disposed) return
            val current = generation
            startedAt = SystemClock.elapsedRealtime(); statsAt = startedAt; lastReceivedAt = 0
            drawnFrames = 0; receivedFrames = 0; decodedFrames = 0; drawnSequence = -1
            stream = MjpegStream(onFrame = { bytes ->
                synchronized(gate) {
                    if (current == generation && !disposed) {
                        lastReceivedAt = SystemClock.elapsedRealtime(); receivedFrames++
                        pending = Encoded(bytes, current, lastReceivedAt)
                        if (!decoding) { decoding = true; decoder.execute { decodeLatest() } }
                    }
                }
            }, onEnd = { message -> post {
                synchronized(gate) { if (current == generation) stop(message) }
            } })
            onStatus("Подключение видео…", true)
            stream?.start(url, http)
            postInvalidateOnAnimation()
            postDelayed(heartbeat, 250)
        }
    }

    private fun decodeLatest() {
        while (true) {
            val encoded = synchronized(gate) {
                val next = pending
                pending = null
                if (next == null) decoding = false
                next
            } ?: return
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(encoded.bytes, 0, encoded.bytes.size, options)
            val valid = options.outWidth in 1..1920 && options.outHeight in 1..1080 &&
                options.outWidth.toLong() * options.outHeight <= 2_073_600
            val bitmap = if (valid) BitmapFactory.decodeByteArray(encoded.bytes, 0, encoded.bytes.size) else null
            synchronized(gate) {
                if (encoded.generation == generation && !disposed) {
                    if (bitmap == null) {
                        post { synchronized(gate) {
                            if (encoded.generation == generation) stop("Не удалось декодировать кадр")
                        } }
                    } else {
                        latest = Decoded(bitmap, ++sequence, encoded.receivedMs)
                        decodedFrames++
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)
        synchronized(gate) {
            // Sample the latest decoded image at the view's frame cadence.
            // Repeated draws do not increment the fresh-frame counter.
            if (stream != null) postInvalidateOnAnimation()
            val frame = latest ?: return
            if (SystemClock.elapsedRealtime() - frame.receivedMs >= 1500) return
            val scale = minOf(width.toFloat() / frame.bitmap.width, height.toFloat() / frame.bitmap.height)
            val w = frame.bitmap.width * scale; val h = frame.bitmap.height * scale
            canvas.drawBitmap(frame.bitmap, null, RectF((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2), paint)
            if (frame.sequence != drawnSequence) { drawnSequence = frame.sequence; drawnFrames++ }
        }
    }

    fun stop(message: String = "Видео выключено") {
        synchronized(gate) {
            generation++; stream?.close(); stream = null; pending = null; latest = null
            removeCallbacks(heartbeat); invalidate(); onStatus(message, false)
            android.util.Log.d("RaceRemoteVideo", message)
        }
    }

    fun close() { stop(); synchronized(gate) { disposed = true; decoder.shutdownNow() } }
}
