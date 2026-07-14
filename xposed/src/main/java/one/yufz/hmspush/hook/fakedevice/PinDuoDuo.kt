package one.yufz.hmspush.hook.fakedevice

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import one.yufz.hmspush.hook.IFakeParam
import one.yufz.xposed.hookMethod

class PinDuoDuo : Common() {
    override fun fake(param: IFakeParam): Boolean {
        super.fake(param)
        Application::class.java.hookMethod("attach", Context::class.java) { chain ->
            val result = chain.proceed()
            val context = chain.getThisObject() as Context
            val hwPushReceiver = ComponentName(context, "com.aimi.android.common.push.huawei.HwPushReceiver")
            context.packageManager.setComponentEnabledSetting(hwPushReceiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0)
            result
        }
        return true
    }
}
