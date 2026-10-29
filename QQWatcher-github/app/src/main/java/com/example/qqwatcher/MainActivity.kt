package com.example.qqwatcher

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ScrollView

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "QQ 关键词监控器"
            textSize = 22f
        })

        statusText = TextView(this).apply { setPadding(0, pad, 0, pad) }
        root.addView(statusText)

        root.addView(Button(this).apply {
            text = "① 授予通知读取权限"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        root.addView(label("规则A关键词（逗号分隔，任何群命中即提醒）"))
        val etKeywords = EditText(this).apply { id = View.generateViewId() }
        root.addView(etKeywords)

        root.addView(label("规则B 重点群名（逗号分隔）"))
        val etGroups = EditText(this).apply { id = View.generateViewId() }
        root.addView(etGroups)

        root.addView(label("规则B 指定成员（逗号分隔，如：管理员张三,李四）"))
        val etMembers = EditText(this).apply { id = View.generateViewId() }
        root.addView(etMembers)

        root.addView(Button(this).apply {
            text = "② 保存配置"
            setOnClickListener {
                getPrefs().edit()
                    .putString(QQWatcherService.KEY_KEYWORDS, etKeywords.text.toString())
                    .putString(QQWatcherService.KEY_GROUPS, etGroups.text.toString())
                    .putString(QQWatcherService.KEY_MEMBERS, etMembers.text.toString())
                    .apply()
                Toast.makeText(this@MainActivity, "已保存", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(Button(this).apply {
            text = "④ 查看记录本（命中过的消息）"
            setOnClickListener { startActivity(Intent(this@MainActivity, RecordsActivity::class.java)) }
        })

        root.addView(Button(this).apply {
            text = "⑤ 打开QQ通知设置（保持群通知开启）"
            setOnClickListener {
                try {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, "com.tencent.mobileqq")
                    })
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        })

        root.addView(TextView(this).apply {
            text = "小米手机额外设置：设置 → 应用设置 → 应用管理 → QQWatcher → 自启动；\n" +
                    "最近任务里下拉锁定本应用，防止被杀后台；\n" +
                    "省电策略设为\"无限制\"。"
            textSize = 13f
            setPadding(0, pad, 0, 0)
        })

        setContentView(root)

        val p = getPrefs()
        etKeywords.setText(p.getString(QQWatcherService.KEY_KEYWORDS, "志愿,二课"))
        etGroups.setText(p.getString(QQWatcherService.KEY_GROUPS, ""))
        etMembers.setText(p.getString(QQWatcherService.KEY_MEMBERS, ""))
    }

    override fun onResume() {
        super.onResume()
        // 确保提醒转发前台服务在运行（负责弹横幅）
        AlertRelayService.start(this)
        // 安卓13+需要运行时申请通知权限，否则提醒可能静默
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }
        refreshStatus()
    }

    private fun refreshStatus() {
        val granted = isListenerGranted()
        statusText.text = if (granted) {
            "✅ 状态：监控运行中"
        } else {
            "❌ 状态：未授权，请点击下面的按钮去开启\n（开启后建议重启一次手机）"
        }
    }

    private fun isListenerGranted(): Boolean {
        val cn = ComponentName(this, QQWatcherService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return enabled.split(":").any { it.equals(cn.flattenToString(), ignoreCase = true) }
    }

    private fun getPrefs() = getSharedPreferences(QQWatcherService.PREFS, Context.MODE_PRIVATE)

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
    }
}
