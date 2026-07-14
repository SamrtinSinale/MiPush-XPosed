package one.yufz.hmspush.hook

import android.util.Log
import one.yufz.xposed.XposedAPI
import java.lang.reflect.Method

object XLog {
    fun t(tag: String, message: String?) {
        if (isDebug()) {
            XposedAPI.requireApi().log(Log.DEBUG, "[MiPush][T][$tag]", message ?: "")
        }
    }

    fun d(tag: String, message: String?) {
        XposedAPI.requireApi().log(Log.DEBUG, "[MiPush][D][$tag]", message ?: "")
    }

    fun i(tag: String, message: String?) {
        XposedAPI.requireApi().log(Log.INFO, "[MiPush][I][$tag]", message ?: "")
    }

    fun e(tag: String, message: String?, throwable: Throwable?) {
        XposedAPI.requireApi().log(Log.ERROR, "[MiPush][E][$tag]", message ?: "")
        if (throwable != null) {
            XposedAPI.requireApi().log(Log.ERROR, "[MiPush][E][$tag]", Log.getStackTraceString(throwable))
        }
    }

    /**
     * Log a method call with its parameters and return value/exception.
     */
    fun logMethod(tag: String, chain: io.github.libxposed.api.XposedInterface.Chain, stackTrace: Boolean = false) {
        d(tag, "╔═══════════════════════════════════════════════════════")
        d(tag, chain.executable.toString())
        d(tag, "${chain.executable.name} called with ${chain.args.joinToString()}")
        if (stackTrace) {
            d(tag, Log.getStackTraceString(Throwable()))
        }
        d(tag, "╚═══════════════════════════════════════════════════════")
    }

    private fun isDebug(): Boolean {
        return try {
            val buildConfigClass = Class.forName("one.yufz.hmspush.xposed.BuildConfig")
            buildConfigClass.getDeclaredField("DEBUG").getBoolean(null)
        } catch (_: Throwable) {
            false
        }
    }
}
