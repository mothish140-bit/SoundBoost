package com.mothish.soundboost

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class BoosterService : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            AudioFx.enabled = false; AudioFx.apply(); stopSelf(); return START_NOT_STICKY
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("boost", "Sound Booster", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, BoosterService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "boost")
            .setSmallIcon(R.drawable.ic_notif).setContentTitle("SoundBoost is active")
            .setContentText("Volume boost ${AudioFx.boost}%").setContentIntent(open)
            .addAction(0, "Turn off", stop).setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        AudioFx.start()
        return START_STICKY
    }

    override fun onDestroy() { AudioFx.release(); super.onDestroy() }
}
