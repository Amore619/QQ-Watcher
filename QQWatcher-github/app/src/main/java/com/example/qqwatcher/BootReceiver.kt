package com.example.qqwatcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 开机自动启动提醒前台服务 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlertRelayService.start(context)
        }
    }
}
