package one.yufz.hmspush.hook.hms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.service.notification.StatusBarNotification
import one.yufz.hmspush.hook.XLog
import one.yufz.hmspush.hook.hms.nm.SystemNotificationManager
import one.yufz.hmspush.hook.system.HookSystemService
import one.yufz.xposed.XposedAPI
import one.yufz.xposed.findClass
import one.yufz.xposed.findClassOrNull
import one.yufz.xposed.set
import java.lang.reflect.InvocationTargetException

object HookPushNC {
    private const val TAG = "HookPushNC"

    private const val TargetClass = "com.nihility.notification.NotificationManagerEx"

    private val hookCheck = { HookSystemService.isSystemHookReady }

    fun canHook(classLoader: ClassLoader): Boolean {
        return classLoader.findClassOrNull(TargetClass) != null
    }

    fun hook(classLoader: ClassLoader) {
        XLog.d(TAG, "hookPushNC() called with: classLoader = $classLoader")

        val classNotificationManager = classLoader.findClass(TargetClass)

        try {
            classNotificationManager["isHooked"] = true
        } catch (_: Throwable) {

        }

        val api = XposedAPI.requireApi()

        // notify(String packageName, String tag, int id, Notification notification)
        classNotificationManager.declaredMethods.find {
            it.name == "notify" && it.parameterCount == 4
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        SystemNotificationManager.notify(
                            chain.getArg(0) as String,
                            chain.getArg(1) as String?,
                            chain.getArg(2) as Int,
                            chain.getArg(3) as Notification
                        )
                    }
                }
                null
            }
        }

        // cancel(String packageName, String tag, int id)
        classNotificationManager.declaredMethods.find {
            it.name == "cancel" && it.parameterCount == 3
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        SystemNotificationManager.cancel(
                            chain.getArg(0) as String,
                            chain.getArg(1) as String?,
                            chain.getArg(2) as Int
                        )
                    }
                }
                null
            }
        }

        // createNotificationChannels(String packageName, List channels)
        classNotificationManager.declaredMethods.find {
            it.name == "createNotificationChannels" && it.parameterCount == 2
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        @Suppress("UNCHECKED_CAST")
                        SystemNotificationManager.createNotificationChannels(
                            chain.getArg(0) as String,
                            chain.getArg(1) as List<NotificationChannel>
                        )
                    }
                }
                null
            }
        }

        // getNotificationChannel(String packageName, String channelId)
        classNotificationManager.declaredMethods.find {
            it.name == "getNotificationChannel" && it.parameterCount == 2
        }?.let { method ->
            api.hook(method).intercept { chain ->
                tryInvoke {
                    SystemNotificationManager.getNotificationChannel(
                        chain.getArg(0) as String,
                        chain.getArg(1) as String
                    )
                }
            }
        }

        // getNotificationChannels(String packageName)
        classNotificationManager.declaredMethods.find {
            it.name == "getNotificationChannels" && it.parameterCount == 1
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        SystemNotificationManager.getNotificationChannels(
                            chain.getArg(0) as String
                        )
                    }
                } else {
                    chain.proceed()
                }
            }
        }

        // deleteNotificationChannel(String packageName, String channelId)
        classNotificationManager.declaredMethods.find {
            it.name == "deleteNotificationChannel" && it.parameterCount == 2
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        SystemNotificationManager.deleteNotificationChannel(
                            chain.getArg(0) as String,
                            chain.getArg(1) as String
                        )
                    }
                }
                null
            }
        }

        // createNotificationChannelGroups(String packageName, List groups)
        classNotificationManager.declaredMethods.find {
            it.name == "createNotificationChannelGroups" && it.parameterCount == 2
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        @Suppress("UNCHECKED_CAST")
                        SystemNotificationManager.createNotificationChannelGroups(
                            chain.getArg(0) as String,
                            chain.getArg(1) as List<NotificationChannelGroup>
                        )
                    }
                }
                null
            }
        }

        // getNotificationChannelGroup(String packageName, String groupId)
        classNotificationManager.declaredMethods.find {
            it.name == "getNotificationChannelGroup" && it.parameterCount == 2
        }?.let { method ->
            api.hook(method).intercept { chain ->
                tryInvoke {
                    SystemNotificationManager.getNotificationChannelGroup(
                        chain.getArg(0) as String,
                        chain.getArg(1) as String
                    )
                }
            }
        }

        // getNotificationChannelGroups(String packageName)
        classNotificationManager.declaredMethods.find {
            it.name == "getNotificationChannelGroups" && it.parameterCount == 1
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        SystemNotificationManager.getNotificationChannelGroups(
                            chain.getArg(0) as String
                        )
                    }
                } else {
                    chain.proceed()
                }
            }
        }

        // deleteNotificationChannelGroup(String packageName, String groupId)
        classNotificationManager.declaredMethods.find {
            it.name == "deleteNotificationChannelGroup" && it.parameterCount == 2
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        SystemNotificationManager.deleteNotificationChannelGroup(
                            chain.getArg(0) as String,
                            chain.getArg(1) as String
                        )
                    }
                }
                null
            }
        }

        // areNotificationsEnabled(String packageName)
        classNotificationManager.declaredMethods.find {
            it.name == "areNotificationsEnabled" && it.parameterCount == 1
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        SystemNotificationManager.areNotificationsEnabled(
                            chain.getArg(0) as String
                        )
                    }
                } else {
                    chain.proceed()
                }
            }
        }

        // getActiveNotifications(String packageName)
        classNotificationManager.declaredMethods.find {
            it.name == "getActiveNotifications" && it.parameterCount == 1
        }?.let { method ->
            api.hook(method).intercept { chain ->
                if (hookCheck()) {
                    tryInvoke {
                        SystemNotificationManager.getActiveNotifications(
                            chain.getArg(0) as String
                        )
                    }
                } else {
                    chain.proceed()
                }
            }
        }
    }

    private inline fun <R> tryInvoke(invoke: () -> R): R {
        return try {
            invoke()
        } catch (e: InvocationTargetException) {
            XLog.e(TAG, "tryInvoke: ", e)
            XLog.e(TAG, "tryInvoke targetException: ", e.targetException)
            throw e.targetException ?: e
        } catch (e: Throwable) {
            XLog.e(TAG, "tryInvoke: ", e)
            XLog.e(TAG, "tryInvoke cause: ", e.cause)
            throw e.cause ?: e
        }
    }
}
