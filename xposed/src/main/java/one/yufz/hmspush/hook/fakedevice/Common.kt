package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.IFakeParam
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.hookMethod

open class Common : IFakeDevice {
    companion object {
        private const val TAG = "Common"
    }

    override fun fake(param: IFakeParam): Boolean {
        XLog.d(TAG, "fake() called with: packageName = ${param.packageName}")
        fakeAllBuildInProperties()
        fakeClass(param)
        return true
    }

    private fun fakeClass(param: IFakeParam) {
        val classLoader = when (param) {
            is one.yufz.hmspush.hook.FakeDeviceParam -> param.classLoader
            else -> return
        }

        var isMIUI = false
        try {
            Class.forName("miui.os.Build", false, classLoader)
            isMIUI = true
        } catch (_: Throwable) {
        }
        if (isMIUI) {
            return
        }

        val classMap: Map<String, Class<out Any>> = mapOf(
            "miui.os.Build" to Object::class.java,
            "miui.external.SdkHelper" to Object::class.java,
        )
        Class::class.java.hookMethod(
            "forName",
            String::class.java,
            Boolean::class.java,
            ClassLoader::class.java
        ) { chain ->
            val requestClass = chain.getArg(0) as String
            val returnClass = classMap[requestClass]
            if (returnClass != null) {
                XLog.d(TAG, "forHook $requestClass")
                returnClass
            } else {
                XLog.t(TAG, "forName $requestClass")
                chain.proceed()
            }
        }
    }
}
