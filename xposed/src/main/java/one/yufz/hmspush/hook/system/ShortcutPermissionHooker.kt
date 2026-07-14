package one.yufz.hmspush.hook.system

import android.content.pm.ShortcutInfo
import android.os.Binder
import one.yufz.hmspush.common.HMS_PACKAGE_NAME
import one.yufz.xposed.XposedAPI
import one.yufz.xposed.findMethodOrNull
import one.yufz.xposed.hook

object ShortcutPermissionHooker {
    private fun fromHms() = try {
        Binder.getCallingUid() == getPackageUid(HMS_PACKAGE_NAME)
    } catch (e: Throwable) {
        false
    }

    private fun getPackageUid(packageName: String): Int = XposedAPI.getPackageUid(packageName)

    private fun doHookPermission(chain: io.github.libxposed.api.XposedInterface.Chain, targetPkgIndex: Int, hookExtra: (() -> Unit)? = null): Any? {
        val pkg = chain.getArg(targetPkgIndex) as? String
        if (pkg != null && fromHms()) {
            Binder.clearCallingIdentity()
            hookExtra?.invoke()
        }
        return chain.proceed()
    }

    fun hook(classShortcutService: Class<*>) {
        findMethodOrNull(classShortcutService, "pushDynamicShortcut", String::class.java, ShortcutInfo::class.java, Int::class.java)
            ?.hook { chain -> doHookPermission(chain, 0) }

        findMethodOrNull(classShortcutService, "getMaxShortcutCountPerActivity", String::class.java, Int::class.java)
            ?.hook { chain -> doHookPermission(chain, 0) }

        findMethodOrNull(classShortcutService, "verifyCaller", String::class.java, Int::class.java)
            ?.hook { chain -> doHookPermission(chain, 0) }
    }
}
