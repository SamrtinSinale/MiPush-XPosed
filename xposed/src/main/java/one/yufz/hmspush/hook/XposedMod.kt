package one.yufz.hmspush.hook

import android.os.Build
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import one.yufz.hmspush.common.HMS_CORE_PROCESS
import one.yufz.hmspush.common.HMS_PACKAGE_NAME
import one.yufz.hmspush.hook.fakedevice.FakeDevice
import one.yufz.hmspush.hook.hms.HookHMS
import one.yufz.hmspush.hook.system.HookSystemService
import one.yufz.hmspush.hook.systemui.HookNotificationSettingsManager
import one.yufz.hmspush.hook.systemui.HookSystemUIPlugin
import one.yufz.xposed.XposedAPI
import one.yufz.xposed.hook

class XposedMod : XposedModule() {
    companion object {
        private const val TAG = "XposedMod"
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        // Initialize global XposedAPI reference for DSL helpers
        XposedAPI.api = this

        XLog.d(TAG, "onModuleLoaded: ${param.processName}")

        // Process detection:
        // Legacy handleLoadPackage was called once per package per process.
        // In API 102, we use lifecycle callbacks instead.
        // system_server is handled by onSystemServerStarting
        if (param.isSystemServer) {
            // system_server hooks are done in onSystemServerStarting
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        XLog.d(TAG, "onPackageLoaded: ${param.packageName}")
        // No hooks here currently; hooks are done in onPackageReady
    }

    override fun onPackageReady(param: PackageReadyParam) {
        XLog.d(TAG, "onPackageReady: ${param.packageName}")

        val packageName = param.packageName
        val classLoader = param.classLoader

        when {
            // System server hooks are done in onSystemServerStarting
            // This callback is NOT fired for system_server (replaced by onSystemServerStarting)

            // SystemUI hooks
            packageName == "com.android.systemui" -> {
                removeHyperOSFocusNotificationPackageLimit(classLoader)
            }

            // MI Push core process hooks
            packageName == HMS_PACKAGE_NAME -> {
                // Check if this is the core MI Push process
                val processName = try {
                    Class.forName("android.app.ActivityThread")
                        .getDeclaredMethod("currentProcessName")
                        .apply { isAccessible = true }
                        .invoke(null) as String
                } catch (_: Throwable) {
                    packageName
                }

                if (processName == HMS_CORE_PROCESS) {
                    HookHMS().hook(classLoader)
                }
            }

            // Default: device faking for all other apps
            else -> {
                if (packageName == "com.google.android.webview") {
                    XLog.d(TAG, "fake() called, ignore $packageName")
                    return
                }

                val lpparam = FakeDeviceParam(packageName, classLoader ?: this::class.java.classLoader!!)
                FakeDevice.fake(lpparam)
            }
        }

        // Detach after initialization - we don't need further lifecycle callbacks
        // because all hooks are set up per-package
        detach()
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        XLog.d(TAG, "onSystemServerStarting: ${param.classLoader}")
        HookSystemService().hook(param.classLoader)
    }

    private fun removeHyperOSFocusNotificationPackageLimit(classLoader: ClassLoader) {
        HookSystemUIPlugin(
            "miui.systemui.plugin",
            HookNotificationSettingsManager()
        ).hook(classLoader)

        HookSystemUIPlugin("miui.systemui.plugin") { pluginLoader ->
            val tag = "HookFocusNotifUtils"
            try {
                val classFocusNotifUtils = Class.forName(
                    "miui.systemui.notification.focus.FocusNotifUtils",
                    false,
                    pluginLoader
                )

                XLog.d(tag, "hooking canShowFocus method")
                classFocusNotifUtils.declaredMethods.find { it.name == "canShowFocus" }!!
                    .hook { _ -> true }
            } catch (e: Throwable) {
                XLog.e(tag, "hook failure: " + e.message, e)
            }
        }.hook(classLoader)
    }
}

/**
 * Compatibility param for FakeDevice - provides packageName + classLoader like the old LoadPackageParam.
 */
data class FakeDeviceParam(
    override val packageName: String,
    val classLoader: ClassLoader
) : IFakeParam

interface IFakeParam {
    val packageName: String
}
