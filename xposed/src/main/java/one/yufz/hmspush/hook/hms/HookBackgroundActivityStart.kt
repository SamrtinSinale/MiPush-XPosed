package one.yufz.hmspush.hook.hms

import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClassOrNull

/**
 * Hook BackgroundActivityStartEnabler in MI Push to suppress
 * the "initializing" status notification during XMPushService startup.
 * 
 * On HyperOS/Android 14+, posting/canceling a notification during service
 * creation throws SecurityException and causes XMPushService to crash,
 * delaying push connection by several seconds.
 * 
 * This hook intercepts initialize() and returns immediately,
 * eliminating the delay without affecting any other functionality.
 */
object HookBackgroundActivityStart {
    private const val TAG = "HookBackgroundActivityStart"

    fun hook(classLoader: ClassLoader) {
        try {
            val clazz = classLoader.findClassOrNull(
                "com.xiaomi.push.service.BackgroundActivityStartEnabler"
            ) ?: return

            val method = clazz.getDeclaredMethod("initialize").apply { isAccessible = true }

            one.yufz.xposed.XposedAPI.requireApi().hook(method).intercept { chain ->
                XLog.d(TAG, "suppressed BackgroundActivityStartEnabler.initialize() to prevent XMPushService crash")
                null
            }

            XLog.d(TAG, "hook installed")
        } catch (e: Throwable) {
            XLog.e(TAG, "hook failed", e)
        }
    }
}
