package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.IFakeParam
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass

open class XGPush : IFakeDevice {
    companion object {
        private const val TAG = "FakeForXGPush"
    }

    override fun fake(param: IFakeParam): Boolean {
        val classLoader = when (param) {
            is one.yufz.hmspush.hook.FakeDeviceParam -> param.classLoader
            else -> return false
        }

        XLog.d(TAG, "fake() called with: classLoader = $classLoader")

        return try {
            val classChannelUtils = classLoader.findClass("com.tencent.tpns.baseapi.base.util.ChannelUtils")
            fakeChannels(classChannelUtils)
            true
        } catch (e: ClassNotFoundException) {
            XLog.e(TAG, "fake ClassNotFoundError", e)
            false
        } catch (e: Throwable) {
            XLog.e(TAG, "fake error: ", e)
            false
        }
    }

    private fun fakeChannels(classChannelUtils: Class<*>): Boolean {
        XLog.d(TAG, "fakeChannels() called")

        classChannelUtils.declaredMethods.forEach { method ->
            one.yufz.xposed.XposedAPI.requireApi().hook(method).intercept { chain ->
                if (method.name == "getMiuiVersionCode") {
                    "13"
                } else if (method.name == "getMiuiVersionName") {
                    "V130"
                } else if (method.name == "isBrandXiaoMi") {
                    true
                } else if (method.returnType == Boolean::class.java) {
                    false
                } else if (method.returnType == String::class.java) {
                    ""
                } else {
                    chain.proceed()
                }
            }
        }
        return true
    }
}
