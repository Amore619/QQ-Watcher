package com.example.qqwatcher

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 记录本：展示所有命中规则的历史消息，可一键清空 */
class RecordsActivity : Activity() {

    private lateinit var listArea: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "记录本（最新在上）"
            textSize = 20f
        })

        listArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val scroll = ScrollView(this).apply { addView(listArea) }
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        root.addView(Button(this).apply {
            text = "清空全部记录"
            setOnClickListener {
                RecordStore.clear(this@RecordsActivity)
                render()
            }
        })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        listArea.removeAllViews()
        val records = RecordStore.all(this)
        if (records.isEmpty()) {
            listArea.addView(TextView(this).apply {
                text = "还没有记录。命中规则的QQ消息会自动出现在这里。"
                setPadding(0, pad(), 0, 0)
            })
            return
        }
        for (r in records) {
            val card = TextView(this).apply {
                text = "【${r.time}】${r.group}  ${r.reason}\n" +
                        (if (r.sender.isNotBlank()) "${r.sender}: " else "") + r.content
                textSize = 15f
                setTextColor(Color.parseColor("#222222"))
                setBackgroundColor(Color.parseColor("#F0F0F5"))
                setPadding(pad(), pad() / 2, pad(), pad() / 2)
                val m = (8 * resources.displayMetrics.density).toInt()
                setPadding(m, m, m, m)
            }
            listArea.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = pad() / 2 })
        }
    }

    private fun pad() = (12 * resources.displayMetrics.density).toInt()
}
