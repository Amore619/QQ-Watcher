package com.example.qqwatcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 简单的记录存储：用 SharedPreferences + JSON 数组保存所有命中记录。
 * 新记录插在最前面，最多保留 500 条，超出自动淘汰最旧的。
 */
object RecordStore {
    private const val KEY = "records"
    private const val MAX = 500

    data class Record(val time: String, val group: String, val sender: String, val content: String, val reason: String)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(QQWatcherService.PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun add(ctx: Context, group: String, sender: String, content: String, reason: String) {
        val arr = JSONArray(prefs(ctx).getString(KEY, "[]"))
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
        val obj = JSONObject()
            .put("time", fmt.format(java.util.Date()))
            .put("group", group)
            .put("sender", sender)
            .put("content", content)
            .put("reason", reason)
        val newArr = JSONArray()
        newArr.put(obj)
        for (i in 0 until minOf(arr.length(), MAX - 1)) newArr.put(arr.get(i))
        prefs(ctx).edit().putString(KEY, newArr.toString()).apply()
    }

    @Synchronized
    fun all(ctx: Context): List<Record> {
        val arr = JSONArray(prefs(ctx).getString(KEY, "[]"))
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Record(o.optString("time"), o.optString("group"), o.optString("sender"), o.optString("content"), o.optString("reason"))
        }
    }

    @Synchronized
    fun clear(ctx: Context) {
        prefs(ctx).edit().putString(KEY, "[]").apply()
    }
}
