package com.example.qqwatcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 核心逻辑：
 * 1. 监听系统通知栏里来自 QQ 的通知
 * 2. 按规则过滤：
 *    规则A：任何群消息文本包含关键词（志愿/二课）→ 提醒
 *    规则B：通知的群名在"重点群"列表里，且发送者在"指定成员"列表里 → 提醒
 * 3. 发一条自己的系统通知，标题格式："群名 关键词/成员"
 */
class QQWatcherService : NotificationListenerService() {

    companion object {
        const val CHANNEL_ID = "qq_watcher_alerts"
        const val PREFS = "config"
        const val KEY_KEYWORDS = "keywords"      // 逗号分隔
        const val KEY_GROUPS = "watched_groups"  // 逗号分隔
        const val KEY_MEMBERS = "watched_members" // 逗号分隔
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // 先删旧渠道再建新的（渠道一旦创建，重要度等属性不可更改，只能换ID重建）
            nm.deleteNotificationChannel("qq_watcher_alerts")
            nm.deleteNotificationChannel("qq_watcher_alerts_v2")
            val ch = NotificationChannel(
                CHANNEL_ID, "QQ关键词提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "检测到志愿/二课/指定成员消息时横幅+声音+震动提醒"
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
                setBypassDnd(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 只处理 QQ 的通知（包名 com.tencent.mobileqq）
        if (sbn.packageName != "com.tencent.mobileqq") return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""

        // 拆分"发送者:内容"。QQ 群通知一般是 "群名" 为标题，"发送者:消息" 为正文
        val (sender, content) = splitSender(text)

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val keywords = splitList(prefs.getString(KEY_KEYWORDS, "志愿,二课"))
        val groups = splitList(prefs.getString(KEY_GROUPS, ""))
        val members = splitList(prefs.getString(KEY_MEMBERS, ""))

        val hitReasons = mutableListOf<String>()

        // 规则A：关键词命中（任何通知文本，不限群）
        val hitKeywords = keywords.filter { it.isNotBlank() && text.contains(it) }
        if (hitKeywords.isNotEmpty()) {
            hitReasons.add(hitKeywords.joinToString("/"))
        }

        // 规则B：重点群 + 指定成员
        val hitGroup = groups.firstOrNull { title.contains(it) }
        if (hitGroup != null && members.any { sender.contains(it) }) {
            hitReasons.add("指定成员:$sender")
        }

        if (hitReasons.isEmpty()) return

        val reason = hitReasons.joinToString(" ")
        RecordStore.add(this, title, sender, content, reason)
        sendAlert(title, sender, content, reason)
    }

    /** 把 "张三:大家好" 拆成 ("张三", "大家好")；拆不开则整段当内容 */
    private fun splitSender(text: String): Pair<String, String> {
        val idx = text.indexOf(':')
        val idx2 = text.indexOf('：')
        val i = if (idx in 0..12) idx else if (idx2 in 0..12) idx2 else -1
        return if (i > 0) text.substring(0, i).trim() to text.substring(i + 1).trim()
        else "" to text
    }

    private fun splitList(s: String?) =
        s?.split(",", "，", "\n")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    private fun sendAlert(groupName: String, sender: String, content: String, reason: String) {
        // 交给前台服务 AlertRelayService 弹横幅（HyperOS下监听服务自己发的通知会被静默）
        val i = Intent(AlertRelayService.ACTION_ALERT)
            .setPackage(packageName)
            .putExtra(AlertRelayService.EXTRA_GROUP, groupName)
            .putExtra(AlertRelayService.EXTRA_SENDER, sender)
            .putExtra(AlertRelayService.EXTRA_CONTENT, content)
            .putExtra(AlertRelayService.EXTRA_REASON, reason)
        sendBroadcast(i)
    }
}
