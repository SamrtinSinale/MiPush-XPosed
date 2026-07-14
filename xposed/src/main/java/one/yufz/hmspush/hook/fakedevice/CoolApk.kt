package one.yufz.hmspush.hook.fakedevice

import android.app.Application
import one.yufz.hmspush.hook.IFakeParam
import one.yufz.xposed.hookMethod

class CoolApk : XGPush() {
    override fun fake(param: IFakeParam): Boolean {
        Application::class.java.hookMethod("onCreate") { chain ->
            val result = chain.proceed()
            super.fake(param)
            result
        }
        return true
    }
}
