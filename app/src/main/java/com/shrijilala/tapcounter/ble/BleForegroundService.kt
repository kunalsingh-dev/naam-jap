package com.shrijilala.tapcounter.ble

import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shrijilala.tapcounter.MainActivity
import com.shrijilala.tapcounter.R
import com.shrijilala.tapcounter.data.SettingsRepository

/**
 * Keeps the GATT connection to the ring alive when the app isn't in the foreground.
 * Android requires a visible notification for any foreground service — there's no way
 * around that at the OS level, so the notification text is kept generic/low-key rather
 * than announcing anything about counting.
 */
class BleForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "ble_ring_connection"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private lateinit var bleTapManager: BleTapManager
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getManager(): BleTapManager = bleTapManager
    }

    override fun onCreate() {
        super.onCreate()
        bleTapManager = BleTapManager(applicationContext, SettingsRepository(applicationContext))
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        val address = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS)
        if (address != null) {
            val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
            val device: BluetoothDevice? = adapter?.getRemoteDevice(address)
            device?.let { bleTapManager.connect(it) }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        bleTapManager.disconnect()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tap Counter")
            .setContentText("Connected")
            .setSmallIcon(android.R.drawable.presence_online)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Ring connection",
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
