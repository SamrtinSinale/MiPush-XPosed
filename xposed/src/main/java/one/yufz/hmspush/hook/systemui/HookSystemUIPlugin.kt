package one.yufz.hmspush.hook.systemui

import android.content.ComponentName
import android.content.ContextWrapper
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.getField
import one.yufz.xposed.hook

class HookSystemUIPlugin(
    private val pluginPackageName: String, private val hooker: ISystemUIPluginHooker
) {
    companion object {
        private const val TAG = "HookSystemUIPlugin"
    }

    fun hook(classLoader: ClassLoader) {
        try {
            val classPluginFactory = Class.forName(
                "com.android.systemui.shared.plugins.PluginInstance\$PluginFactory",
                false, classLoader
            )
            classPluginFactory.declaredMethods.find { it.name == "createPluginContext" }!!.hook { chain ->
                val result = chain.proceed()
                val componentName = chain.getThisObject()
                    .getField("mComponentName", ComponentName::class.java)
                if (componentName!!.packageName == pluginPackageName) {
                    chain.getThisObject().javaClass
                        .getDeclaredMethod("createPluginContext")
                        .apply { isAccessible = true }
                    // unhook this hook after first match
                    chain.executable.let { executable ->
                        one.yufz.xposed.XposedAPI.requireApi().hook(executable).intercept { c -> c.proceed() }
                    }

                    val pluginContext = result as ContextWrapper
                    val pluginLoader = pluginContext.classLoader
                    XLog.d(TAG, "hook [$pluginPackageName] by Plugin ClassLoader: [$pluginLoader]")
                    hooker.hook(pluginLoader)
                }
                result
            }
        } catch (e: Throwable) {
            XLog.e(
                TAG,
                "hook SystemUI Plugin [$pluginPackageName] with [${hooker.javaClass.name}] failure: " + e.message,
                e
            )
        }
    }
}
