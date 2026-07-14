package one.yufz.hmspush.hook.hms

import android.content.Context
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.hookMethod

object FakeHsf {
    private const val TAG = "FakeHsf"

    fun hook(classLoader: ClassLoader) {
        XLog.d(TAG, "hook() called with: classLoader = $classLoader")

        classLoader.findClass("com.huawei.hsf.common.api.HsfAvailability")
            .hookMethod("getInstance") { chain ->
                val result = chain.proceed()
                hookHsfAvailabilityImpl(result.javaClass)
                result
            }

        classLoader.findClass("com.huawei.hsf.common.api.HsfApi")
            .hookAllMethods("newInstance") { chain ->
                val result = chain.proceed()
                hookHsfApiImpl(result.javaClass)
                result
            }
    }

    private fun hookHsfAvailabilityImpl(classHsfAvailabilityImpl: Class<*>) {
        XLog.d(TAG, "hookHsfAvailabilityImpl() called with: classHsfAvailabilityImpl = $classHsfAvailabilityImpl")

        classHsfAvailabilityImpl.hookMethod("isHuaweiMobileServicesAvailable", Context::class.java) { _ ->
            0
        }
    }

    private fun hookHsfApiImpl(classHsfApiImpl: Class<*>) {
        XLog.d(TAG, "hookHsfApiImpl() called with: classHsfApiImpl = $classHsfApiImpl")

        classHsfApiImpl.hookMethod("isConnected") { _ -> true }
    }

    private fun Class<*>.hookMethod(name: String, vararg paramTypes: Class<*>, interceptor: (io.github.libxposed.api.XposedInterface.Chain) -> Any?) {
        val method = getDeclaredMethod(name, *paramTypes).apply { isAccessible = true }
        one.yufz.xposed.XposedAPI.requireApi().hook(method).intercept { chain -> interceptor(chain) }
    }

    private fun Class<*>.hookAllMethods(name: String, interceptor: (io.github.libxposed.api.XposedInterface.Chain) -> Any?) {
        declaredMethods.filter { it.name == name }.forEach { method ->
            one.yufz.xposed.XposedAPI.requireApi().hook(method).intercept { chain -> interceptor(chain) }
        }
    }
}
