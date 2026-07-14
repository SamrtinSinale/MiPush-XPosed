package one.yufz.hmspush.hook.systemui

import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.hook

class HookNotificationSettingsManager : ISystemUIPluginHooker {
    companion object {
        private const val TAG = "FocusNotification"
    }

    override fun hook(pluginLoader: ClassLoader) {
        try {
            XLog.d(TAG, "hook start")
            val classNotificationSettingsManager = Class.forName(
                "miui.systemui.notification.NotificationSettingsManager",
                false, pluginLoader
            )

            XLog.d(TAG, "hook method")
            classNotificationSettingsManager.declaredMethods.find { it.name == "canCustomFocus" }!!
                .hook { _ -> true }
            classNotificationSettingsManager.declaredMethods.find { it.name == "canShowFocus" }!!
                .hook { _ -> true }
            XLog.d(TAG, "hook end")
        } catch (e: Throwable) {
            XLog.e(TAG, "hook NotificationSettingsManager failure: " + e.message, e)
        }
    }
}
