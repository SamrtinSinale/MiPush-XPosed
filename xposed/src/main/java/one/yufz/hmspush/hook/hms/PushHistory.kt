package one.yufz.hmspush.hook.hms

import android.content.Context
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.XposedAPI

object PushHistory {
    private const val TAG = "PushHistory"

    private val store by lazy {
        getCurrentApplication().getSharedPreferences("push_history", Context.MODE_PRIVATE)
    }

    fun record(packageName: String) {
        val time = System.currentTimeMillis()
        XLog.d(TAG, "record() called with: packageName = $packageName, time = $time")

        store.edit().putLong(packageName, time).apply()
    }

    fun read(packageName: String): Long {
        return store.getLong(packageName, 0L)
    }

    fun readAll(): List<Pair<String, Long>> {
        return store.all.map { it.key to (it.value as? Long ?: 0L) }
    }

    fun clear(packageName: String) {
        store.edit().remove(packageName).apply()
    }

    private fun getCurrentApplication(): android.app.Application = XposedAPI.currentApplication()
}
