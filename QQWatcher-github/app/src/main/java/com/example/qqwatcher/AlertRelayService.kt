package com.example.qqwatcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder

/**
 * 提醒转发前台服务：
 * HyperOS/MIUI 会对"通知监听服务自己发的通知"降级为静默红点，
 * 所以由这个前台服务（常驻、系统信任度高）负责真正弹出横幅+声音+震动。
 */
class AlertRelayService : Service() {

    companion object {
        const val ACTION_ALERT = "com.example.qqwatcher.ALERT"
        const val EXTRA_GROUP = "group"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_REASON = "reason"
        const val CHANNEL_ID = "qq_watcher_alerts_v3"
        const val FG_CHANNEL_ID = "qq_watcher_foreground"

        fun start(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(Intent(ctx, AlertRelayService::class.java))
            } else {
                ctx.startService(Intent(ctx, AlertRelayService::class.java))
            }
        }
    }

    private val alertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_ALERT) {
                postAlert(
                    intent.getStringExtra(EXTRA_GROUP) ?: "QQ消息",
                    intent.getStringExtra(EXTRA_SENDER) ?: "",
                    intent.getStringExtra(EXTRA_CONTENT) ?: "",
                    intent.getStringExtra(EXTRA_REASON) ?: ""
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        val filter = IntentFilter(ACTION_ALERT)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(alertReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(alertReceiver, filter)
        }
        startForeground(1, buildFgNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(alertReceiver)
        super.onDestroy()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 常驻小通知的渠道：低优先级、无声
        nm.createNotificationChannel(
            NotificationChannel(
                FG_CHANNEL_ID, "监控运行中（常驻）",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setSound(null, null); enableVibration(false) }
        )

        // 提醒渠道：横幅+声音+震动
        nm.deleteNotificationChannel("qq_watcher_alerts")
        nm.deleteNotificationChannel(CHANNEL_ID.takeIf { it != "qq_watcher_alerts" } ?: return)
        val ch = NotificationChannel(
            CHANNEL_ID, "QQ关键词提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "命中志愿/二课/指定成员时横幅+声音+震动"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            lightColor = android.graphics.Color.RED
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)
    }

    private fun buildFgNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, FG_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        return builder
            .setContentTitle("QQ关键词监控运行中")
            .setContentText("正在监听QQ消息通知…点此进入设置")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    private fun postAlert(groupName: String, sender: String, content: String, reason: String) {
        val titleAlert = "$groupName  $reason"
        val body = if (sender.isNotBlank()) "$sender: $content" else content

        val launch = packageManager.getLaunchIntentForPackage("com.tencent.mobileqq")
        val pi = PendingIntent.getActivity(
            this, 0, launch ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        builder.setContentTitle(titleAlert)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setDefaults(Notification.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            builder.setPriority(Notification.PRIORITY_MAX)
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((System.currentTimeMillis() and 0x7FFFFFFF).toInt(), builder.build())
    }
}
