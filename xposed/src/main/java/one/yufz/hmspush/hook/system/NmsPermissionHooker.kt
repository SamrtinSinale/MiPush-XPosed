package one.yufz.hmspush.hook.system

import android.app.Notification
import android.app.NotificationChannelGroup
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Process
import one.yufz.hmspush.common.ANDROID_PACKAGE_NAME
import one.yufz.hmspush.common.HMS_PACKAGE_NAME
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.XposedAPI
import one.yufz.xposed.findMethodOrNull
import one.yufz.xposed.hook
import java.lang.reflect.Method

object NmsPermissionHooker {
    private const val TAG = "NmsPermissionHooker"

    private fun fromHms() = try {
        Binder.getCallingUid() == getPackageUid(HMS_PACKAGE_NAME)
    } catch (e: Throwable) {
        false
    }

    private fun getPackageUid(packageName: String): Int = XposedAPI.getPackageUid(packageName)

    private fun getContext(): Context = XposedAPI.currentApplication()

    fun hook(classINotificationManager: Class<*>) {
        // boolean areNotificationsEnabledForPackage(String pkg, int uid);
        findMethodExact(classINotificationManager, "areNotificationsEnabledForPackage", String::class.java, Int::class.java)
            ?.hook { chain ->
                doHookPermission(chain, 0, null)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            findMethodExact(classINotificationManager, "getNotificationChannelForPackage", String::class.java, Int::class.java, String::class.java, String::class.java, Boolean::class.java)
                ?.hook { chain -> doHookPermission(chain, 0, null) }
        } else {
            findMethodExact(classINotificationManager, "getNotificationChannelForPackage", String::class.java, Int::class.java, String::class.java, Boolean::class.java)
                ?.hook { chain -> doHookPermission(chain, 0, null) }
        }

        findMethodExact(classINotificationManager, "getNotificationChannelsForPackage", String::class.java, Int::class.java, Boolean::class.java)
            ?.hook { chain -> doHookPermission(chain, 0, null) }

        findMethodExact(classINotificationManager, "enqueueNotificationWithTag", String::class.java, String::class.java, String::class.java, Int::class.java, Notification::class.java, Int::class.java)
            ?.hook { chain ->
                val pkg = chain.getArg(0) as String
                if (fromHms() && pkg != HMS_PACKAGE_NAME) {
                    Binder.clearCallingIdentity()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val newArgs = chain.args.toMutableList()
                        newArgs[1] = ANDROID_PACKAGE_NAME
                        chain.proceed(newArgs.toTypedArray())
                    } else {
                        chain.proceed()
                    }
                } else {
                    chain.proceed()
                }
            }

        findMethodExact(classINotificationManager, "createNotificationChannelsForPackage", String::class.java, Int::class.java, Class.forName("android.content.pm.ParceledListSlice"))
            ?.hook { chain -> doHookPermission(chain, 0, null) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            findMethodExact(classINotificationManager, "cancelNotificationWithTag", String::class.java, String::class.java, String::class.java, Int::class.java, Int::class.java)
                ?.hook { chain ->
                    val pkg = chain.getArg(0) as String
                    if (fromHms() && pkg != HMS_PACKAGE_NAME) {
                        Binder.clearCallingIdentity()
                        val newArgs = chain.args.toMutableList()
                        newArgs[1] = ANDROID_PACKAGE_NAME
                        chain.proceed(newArgs.toTypedArray())
                    } else {
                        chain.proceed()
                    }
                }
        } else {
            findMethodExact(classINotificationManager, "cancelNotificationWithTag", String::class.java, String::class.java, Int::class.java, Int::class.java)
                ?.hook { chain -> doHookPermission(chain, 0, null) }
        }

        findMethodExact(classINotificationManager, "deleteNotificationChannel", String::class.java, String::class.java)
            ?.hook { chain -> doHookPermission(chain, 0, null) }

        findMethodExact(classINotificationManager, "getAppActiveNotifications", String::class.java, Int::class.java)
            ?.hook { chain -> doHookPermission(chain, 0, null) }

        findMethodExact(classINotificationManager, "getNotificationChannelsForPackage", String::class.java, Int::class.java, Boolean::class.java)
            ?.hook { chain -> doHookPermission(chain, 0, null) }

        // deleteNotificationChannel with different params per SDK
        val deleteNotificationChannelHook: (io.github.libxposed.api.XposedInterface.Chain) -> Any? = { chain ->
            if (Binder.getCallingUid() == Process.SYSTEM_UID) {
                val packageName = chain.getArg(0) as String
                val newArgs = chain.args.toMutableList()
                newArgs[1] = getPackageUid(packageName)
                chain.proceed(newArgs.toTypedArray())
            } else {
                chain.proceed()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                Class.forName("com.android.server.notification.PreferencesHelper", false, classINotificationManager.classLoader)
                    .hookMethod("deleteNotificationChannel", String::class.java, Int::class.java, String::class.java, Int::class.java, Boolean::class.java,
                        interceptor = deleteNotificationChannelHook
                    )
            } catch (_: NoSuchMethodError) {
                XLog.d(TAG, "hook deleteNotificationChannel error, NoSuchMethodError")
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Class.forName("com.android.server.notification.PreferencesHelper", false, classINotificationManager.classLoader)
                .hookMethod("deleteNotificationChannel", String::class.java, Int::class.java, String::class.java,
                    interceptor = deleteNotificationChannelHook
                )
        } else {
            Class.forName("com.android.server.notification.RankingHelper", false, classINotificationManager.classLoader)
                .hookMethod("deleteNotificationChannel", String::class.java, Int::class.java, String::class.java,
                    interceptor = deleteNotificationChannelHook
                )
        }

        findMethodExact(classINotificationManager, "updateNotificationChannelGroupForPackage", String::class.java, Int::class.java, NotificationChannelGroup::class.java)
            ?.hook { chain -> doHookPermission(chain, 0, null) }

        findMethodExact(classINotificationManager, "getNotificationChannelGroupForPackage", String::class.java, String::class.java, Int::class.java)
            ?.hook { chain -> doHookPermission(chain, 1, null) }

        findMethodExact(classINotificationManager, "getNotificationChannelGroupsForPackage", String::class.java, Int::class.java, Boolean::class.java)
            ?.hook { chain -> doHookPermission(chain, 0, null) }

        findMethodExact(classINotificationManager, "deleteNotificationChannelGroup", String::class.java, String::class.java)
            ?.hook { chain -> doHookPermission(chain, 0, null) }
    }

    private fun doHookPermission(chain: io.github.libxposed.api.XposedInterface.Chain, targetPkgIndex: Int, hookExtra: (() -> Unit)?): Any? {
        if (fromHms()) {
            Binder.clearCallingIdentity()
            hookExtra?.invoke()
            return chain.proceed()
        }
        return chain.proceed()
    }

    private fun findMethodExact(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): java.lang.reflect.Method? {
        return findMethodOrNull(clazz, name, *paramTypes)
    }

    private fun Class<*>.hookMethod(name: String, vararg paramTypes: Class<*>, interceptor: (io.github.libxposed.api.XposedInterface.Chain) -> Any?) {
        val method = findMethodExact(this, name, *paramTypes) ?: return
        XposedAPI.requireApi().hook(method).intercept { chain -> interceptor(chain) }
    }
}
