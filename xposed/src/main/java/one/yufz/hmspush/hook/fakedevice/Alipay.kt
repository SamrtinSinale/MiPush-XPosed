package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.IFakeParam
import one.yufz.xposed.findClass
import one.yufz.xposed.hook

class Alipay : IFakeDevice {
    override fun fake(param: IFakeParam): Boolean {
        val classLoader = when (param) {
            is one.yufz.hmspush.hook.FakeDeviceParam -> param.classLoader
            else -> return true
        }

        classLoader.findClass("com.alipay.pushsdk.thirdparty.xiaomi.XiaoMIPushWorker")
            .declaredMethods
            .find { it.returnType == Boolean::class.java }
            ?.hook { _ -> true }

        return true
    }
}
