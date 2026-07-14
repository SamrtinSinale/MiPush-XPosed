package one.yufz.hmspush.hook.hms.nm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import one.yufz.hmspush.common.ANDROID_PACKAGE_NAME
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.callMethod
import one.yufz.xposed.setField
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.InvocationTargetException


object SystemNotificationManager {
    private const val TAG = "SystemNotificationManager"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }

    private val notificationManager: Any by lazy {
        val nmClass = Class.forName("android.app.NotificationManager")
        val getServiceMethod = nmClass.getDeclaredMethod("getService").apply { isAccessible = true }
        getServiceMethod.invoke(null)!!
    }

    private fun getUid(packageName: String): Int {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentApplication = activityThreadClass.getDeclaredMethod("currentApplication").apply { isAccessible = true }
        val app = currentApplication.invoke(null) as Context
        val pm = Context::class.java.getMethod("getPackageManager").apply { isAccessible = true }.invoke(app)
        return pm.javaClass.getMethod("getPackageUid", String::class.java, Int::class.java).apply { isAccessible = true }
            .invoke(pm, packageName, 0) as Int
    }

    private fun getUserId(): Int {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentApplication = activityThreadClass.getDeclaredMethod("currentApplication").apply { isAccessible = true }
        val app = currentApplication.invoke(null) as Context
        return try {
            Context::class.java.getMethod("getUserId").apply { isAccessible = true }.invoke(app) as Int
        } catch (_: NoSuchMethodException) {
            0
        }
    }

    fun notify(
        packageName: String,
        tag: String?, id: Int, notification: Notification
    ) {
        XLog.d(TAG, "notify() called with: packageName = $packageName, tag = $tag, id = $id, notification = $notification")

        val method = notificationManager.javaClass.getDeclaredMethod(
            "enqueueNotificationWithTag",
            String::class.java, String::class.java, String::class.java,
            Int::class.java, Notification::class.java, Int::class.java
        ).apply { isAccessible = true }
        val opPkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ANDROID_PACKAGE_NAME else packageName
        method.invoke(notificationManager, packageName, opPkg, tag, id, notification, getUserId())
    }

    fun cancel(
        packageName: String,
        tag: String?, id: Int
    ) {
        XLog.d(TAG, "cancel() called with: packageName = $packageName, tag = $tag, id = $id")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val method = notificationManager.javaClass.getDeclaredMethod(
                "cancelNotificationWithTag",
                String::class.java, String::class.java, String::class.java,
                Int::class.java, Int::class.java
            ).apply { isAccessible = true }
            method.invoke(notificationManager, packageName, ANDROID_PACKAGE_NAME, tag, id, getUserId())
        } else {
            val method = notificationManager.javaClass.getDeclaredMethod(
                "cancelNotificationWithTag",
                String::class.java, String::class.java, Int::class.java, Int::class.java
            ).apply { isAccessible = true }
            method.invoke(notificationManager, packageName, tag, id, getUserId())
        }
    }

    fun createNotificationChannels(
        packageName: String,
        channels: List<NotificationChannel>
    ) {
        XLog.d(TAG, "createNotificationChannels() called with: packageName = $packageName, channels = $channels")

        val parceledListSliceClass = Class.forName("android.content.pm.ParceledListSlice")
        val constructor = parceledListSliceClass.getDeclaredConstructor(List::class.java).apply { isAccessible = true }
        val channelsList = constructor.newInstance(channels)

        notificationManager.javaClass.getDeclaredMethod(
            "createNotificationChannelsForPackage",
            String::class.java, Int::class.java, parceledListSliceClass
        ).apply { isAccessible = true }
            .invoke(notificationManager, packageName, getUid(packageName), channelsList)
    }

    fun getNotificationChannel(
        packageName: String,
        channelId: String?
    ): NotificationChannel? {
        XLog.d(TAG, "getNotificationChannel() called with: packageName = $packageName, channelId = $channelId")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val method = notificationManager.javaClass.getDeclaredMethod(
                "getNotificationChannelForPackage",
                String::class.java, Int::class.java, String::class.java, String::class.java, Boolean::class.java
            ).apply { isAccessible = true }
            method.invoke(notificationManager, packageName, getUid(packageName), channelId, null, false) as NotificationChannel?
        } else {
            val method = notificationManager.javaClass.getDeclaredMethod(
                "getNotificationChannelForPackage",
                String::class.java, Int::class.java, String::class.java, Boolean::class.java
            ).apply { isAccessible = true }
            method.invoke(notificationManager, packageName, getUid(packageName), channelId, false) as NotificationChannel?
        }
    }

    fun getNotificationChannels(
        packageName: String
    ): List<NotificationChannel?>? {
        XLog.d(TAG, "getNotificationChannels() called with: packageName = $packageName")

        val method = notificationManager.javaClass.getDeclaredMethod(
            "getNotificationChannelsForPackage",
            String::class.java, Int::class.java, Boolean::class.java
        ).apply { isAccessible = true }
        val parceledListSlice = method.invoke(notificationManager, packageName, getUid(packageName), false)
        val list = parceledListSlice?.javaClass?.getDeclaredMethod("getList")?.apply { isAccessible = true }?.invoke(parceledListSlice)
        @Suppress("UNCHECKED_CAST")
        return list as? List<NotificationChannel?>?
    }

    fun deleteNotificationChannel(
        packageName: String,
        channelId: String
    ) {
        XLog.d(TAG, "deleteNotificationChannel() called with: packageName = $packageName, channelId = $channelId")

        val method = notificationManager.javaClass.getDeclaredMethod(
            "deleteNotificationChannel", String::class.java, String::class.java
        ).apply { isAccessible = true }
        method.invoke(notificationManager, packageName, channelId)
    }

    fun createNotificationChannelGroups(
        packageName: String,
        groups: List<NotificationChannelGroup>
    ) {
        XLog.d(TAG, "createNotificationChannelGroups() called with: packageName = $packageName, groups = $groups")

        groups.forEach {
            it.setField("mName", "Mi Push", String::class.java)
            try {
                notificationManager.javaClass.getDeclaredMethod(
                    "updateNotificationChannelGroupForPackage",
                    String::class.java, Int::class.java, NotificationChannelGroup::class.java
                ).apply { isAccessible = true }
                    .invoke(notificationManager, packageName, getUid(packageName), it)
            } catch (e: Throwable) {
                // ignore - Attempt to invoke virtual method 'boolean android.app.NotificationChannelGroup.isBlocked()' on a null object reference
            }
        }
    }

    fun getNotificationChannelGroup(
        packageName: String,
        groupId: String
    ): NotificationChannelGroup? {
        XLog.d(TAG, "getNotificationChannelGroup() called with: packageName = $packageName, groupId = $groupId")

        val method = notificationManager.javaClass.getDeclaredMethod(
            "getNotificationChannelGroupForPackage",
            String::class.java, String::class.java, Int::class.java
        ).apply { isAccessible = true }
        return method.invoke(notificationManager, groupId, packageName, getUid(packageName)) as? NotificationChannelGroup?
    }

    fun getNotificationChannelGroups(
        packageName: String
    ): List<NotificationChannelGroup?>? {
        XLog.d(TAG, "getNotificationChannelGroups() called with: packageName = $packageName")

        val method = notificationManager.javaClass.getDeclaredMethod(
            "getNotificationChannelGroupsForPackage",
            String::class.java, Int::class.java, Boolean::class.java
        ).apply { isAccessible = true }
        val parceledListSlice = method.invoke(notificationManager, packageName, getUid(packageName), false)
        val list = parceledListSlice?.javaClass?.getDeclaredMethod("getList")?.apply { isAccessible = true }?.invoke(parceledListSlice)
        @Suppress("UNCHECKED_CAST")
        return list as? List<NotificationChannelGroup?>?
    }

    fun deleteNotificationChannelGroup(
        packageName: String,
        groupId: String
    ) {
        XLog.d(TAG, "deleteNotificationChannelGroup() called with: packageName = $packageName, groupId = $groupId")

        val method = notificationManager.javaClass.getDeclaredMethod(
            "deleteNotificationChannelGroup", String::class.java, String::class.java
        ).apply { isAccessible = true }
        method.invoke(notificationManager, packageName, groupId)
    }

    fun areNotificationsEnabled(
        packageName: String
    ): Boolean {
        XLog.d(TAG, "areNotificationsEnabled() called with: packageName = $packageName")

        val method = notificationManager.javaClass.getDeclaredMethod(
            "areNotificationsEnabledForPackage", String::class.java, Int::class.java
        ).apply { isAccessible = true }
        return method.invoke(notificationManager, packageName, getUid(packageName)) as Boolean
    }

    fun getActiveNotifications(
        packageName: String
    ): Array<StatusBarNotification?>? {
        XLog.d(TAG, "getActiveNotifications() called with: packageName = $packageName")

        val method = notificationManager.javaClass.getDeclaredMethod(
            "getAppActiveNotifications", String::class.java, Int::class.java
        ).apply { isAccessible = true }
        val parceledListSlice = method.invoke(notificationManager, packageName, getUserId())
        val list = parceledListSlice?.javaClass?.getDeclaredMethod("getList")?.apply { isAccessible = true }?.invoke(parceledListSlice)
        @Suppress("UNCHECKED_CAST")
        return (list as? List<StatusBarNotification>)?.toTypedArray()
    }
}
