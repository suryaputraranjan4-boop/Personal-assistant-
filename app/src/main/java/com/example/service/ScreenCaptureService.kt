package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

class ScreenCaptureService : Service() {

    private val tag = "ScreenCaptureService"
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null

    companion object {
        const val ACTION_START = "com.example.service.action.START_SCREEN_CAPTURE"
        const val ACTION_STOP = "com.example.service.action.STOP_SCREEN_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 4040

        private val _isCapturing = MutableStateFlow(false)
        val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

        private val _latestScreenshotBitmap = MutableStateFlow<Bitmap?>(null)
        val latestScreenshotBitmap: StateFlow<Bitmap?> = _latestScreenshotBitmap.asStateFlow()

        var mediaProjection: MediaProjection? = null
            private set

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun getLatestScreenshotBytes(): ByteArray? {
            val bmp = _latestScreenshotBitmap.value ?: return null
            return try {
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                stream.toByteArray()
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && data != null) {
                    startScreenCapture(resultCode, data)
                } else {
                    Log.e(tag, "Missing resultCode or Intent data for MediaProjection")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                tearDownCapture()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        val notification = buildForegroundNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = mpManager.getMediaProjection(resultCode, data)
            if (mp == null) {
                Log.e(tag, "MediaProjectionManager.getMediaProjection returned null")
                stopSelf()
                return
            }
            mediaProjection = mp

            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(tag, "MediaProjection stopped by system")
                    tearDownCapture()
                    stopSelf()
                }
            }
            mediaProjectionCallback = callback
            mp.registerCallback(callback, Handler(Looper.getMainLooper()))

            val metrics = resources.displayMetrics
            // Standardize resolution for responsive streaming and low memory footprint
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val density = metrics.densityDpi

            // Cap dimensions to max 720 width for optimal AI multimodal processing & memory
            val scale = if (screenWidth > 720) 720f / screenWidth else 1.0f
            val captureWidth = (screenWidth * scale).toInt()
            val captureHeight = (screenHeight * scale).toInt()

            val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            reader.setOnImageAvailableListener({ ir ->
                try {
                    val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * captureWidth

                    val rawBitmap = Bitmap.createBitmap(
                        captureWidth + rowPadding / pixelStride,
                        captureHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    rawBitmap.copyPixelsFromBuffer(buffer)
                    val cleanBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, captureWidth, captureHeight)
                    _latestScreenshotBitmap.value = cleanBitmap
                    image.close()
                } catch (e: Exception) {
                    Log.w(tag, "Error processing screen frame", e)
                }
            }, Handler(Looper.getMainLooper()))

            virtualDisplay = mp.createVirtualDisplay(
                "KavyaScreenCapture",
                captureWidth,
                captureHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )

            _isCapturing.value = true
            Log.i(tag, "Screen capture successfully initiated at ${captureWidth}x${captureHeight}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start MediaProjection virtual display", e)
            tearDownCapture()
            stopSelf()
        }
    }

    private fun tearDownCapture() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjectionCallback?.let {
                mediaProjection?.unregisterCallback(it)
            }
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.w(tag, "Error tearing down capture", e)
        }
        _isCapturing.value = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        tearDownCapture()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kavya Screen Sharing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Kavya AI is sharing or viewing your screen"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kavya Screen Sharing Active")
            .setContentText("Kavya AI has real-time visual screen access")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
