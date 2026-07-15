package one.yufz.hmspush.hook.hms

import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClassOrNull
import one.yufz.xposed.findMethodOrNull

/**
 * Hook BackgroundActivityStartEnabler in MI Push to suppress
 * the "initializing" status notification during XMPushService startup.
 * 
 * On HyperOS/Android 14+, posting a notification during service creation
 * throws SecurityException and causes XMPushService to crash + restart,
 * delaying push connection by several seconds.
 * 
 * This hook intercepts notifyPushStatusInitializing() and does nothing,
 * eliminating the delay without affecting any other functionality.
 */
object HookBackgroundActivityStart {
    private const val TAG = "HookBackgroundActivityStart"

    fun hook(classLoader: ClassLoader) {
        try {
            val clazz = classLoader.findClassOrNull(
                "com.xiaomi.push.service.BackgroundActivityStartEnabler"
            ) ?: return

            val method = clazz.getDeclaredMethod("notifyPushStatusInitializing").apply { isAccessible = true }

            one.yufz.xposed.XposedAPI.requireApi().hook(method).intercept { _ ->
                XLog.d(TAG, "suppressed notifyPushStatusInitializing to prevent XMPushService crash")
                null
            }

            XLog.d(TAG, "hook installed")
        } catch (e: Throwable) {
            XLog.e(TAG, "hook failed", e)
        }
    }
}
