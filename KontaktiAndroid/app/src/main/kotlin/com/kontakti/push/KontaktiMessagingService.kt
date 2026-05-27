package com.kontakti.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kontakti.MainActivity
import com.kontakti.R
import com.kontakti.data.repository.PushRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class KontaktiMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushRepo: PushRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { pushRepo.register(token) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val notification = remoteMessage.notification
        val title = notification?.title ?: remoteMessage.data["title"] ?: "Kontakti"
        val body = notification?.body ?: remoteMessage.data["body"].orEmpty()
        val deeplink = remoteMessage.data["deeplink"] ?: "kontakti://today"
        showNotification(title, body, deeplink)
    }

    private fun showNotification(title: String, body: String, deeplink: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Kontakti Today", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(ch)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.parse(deeplink)
        }
        val pending = PendingIntent.getActivity(
            this, Random.nextInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        nm.notify(Random.nextInt(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "kontakti_today"
    }
}
