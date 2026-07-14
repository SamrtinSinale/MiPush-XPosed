package one.yufz.hmspush.hook.hms

import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass

class HookHMS {
    companion object {
        private const val TAG = "HookHMS"
    }

    fun hook(classLoader: ClassLoader) {
        if (HookPushNC.canHook(classLoader)) {
            HookPushNC.hook(classLoader)
        }
    }
}
