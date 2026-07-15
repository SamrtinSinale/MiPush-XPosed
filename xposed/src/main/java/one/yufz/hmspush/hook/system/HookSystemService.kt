package one.yufz.hmspush.hook.system

import android.app.NotificationManager
import android.content.Context
import android.os.Binder
import one.yufz.hmspush.common.IS_SYSTEM_HOOK_READY
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findMethodOrNull
import one.yufz.xposed.hook
import one.yufz.xposed.hookMethod

class HookSystemService {
    companion object {
        private const val TAG = "HookSystemService"
        val isSystemHookReady: Boolean = true
    }

    fun hook(classLoader: ClassLoader) {
        val classNotificationManagerService = Class.forName("com.android.server.notification.NotificationManagerService", false, classLoader)

        classNotificationManagerService.hookMethod("onStart") { chain ->
            val result = chain.proceed()
            XLog.d(TAG, "onStart invoked")
            val nmsClass = chain.getThisObject().javaClass
            try {
                val mServiceField = findField(nmsClass, "mService")
                val stubClass = mServiceField.get(chain.getThisObject())!!.javaClass
                hookPermission(stubClass)
                hookSystemReadyFlag(stubClass)
            } catch (t: Throwable) {
                XLog.e(TAG, "onStart hook error", t)
            }
            result
        }

        // private boolean isPackageSuspendedForUser(String pkg, int uid)
        classNotificationManagerService.hookMethod("isPackageSuspendedForUser", String::class.java, Int::class.java) { chain ->
            if (Binder.getCallingUid() == 1000) {
                false
            } else {
                chain.proceed()
            }
        }

        val classShortcutService = Class.forName("com.android.server.pm.ShortcutService", false, classLoader)
        ShortcutPermissionHooker.hook(classShortcutService)
    }

    private fun hookSystemReadyFlag(stubClass: Class<*>) {
        stubClass.hookMethod("isSystemConditionProviderEnabled", String::class.java) { chain ->
            if (chain.getArg(0) == IS_SYSTEM_HOOK_READY) {
                true
            } else {
                chain.proceed()
            }
        }
    }

    private fun hookPermission(stubClass: Class<*>) {
        NmsPermissionHooker.hook(stubClass)
    }
}

/**
 * Find a method in the class hierarchy (searches superclasses).
 */
private fun findMethod(clazz: Class<*>, name: String): java.lang.reflect.Method {
    var current: Class<*>? = clazz
    while (current != null) {
        try {
            return current.getDeclaredMethod(name).apply { isAccessible = true }
        } catch (_: NoSuchMethodException) {
            current = current.superclass
        }
    }
    throw NoSuchMethodException("${clazz.name}.$name")
}

/**
 * Find a field in the class hierarchy (searches superclasses).
 */
private fun findField(clazz: Class<*>, name: String): java.lang.reflect.Field {
    var current: Class<*>? = clazz
    while (current != null) {
        try {
            return current.getDeclaredField(name).apply { isAccessible = true }
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    throw NoSuchFieldException("${clazz.name}.$name")
}
