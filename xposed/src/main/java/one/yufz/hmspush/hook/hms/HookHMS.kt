package one.yufz.hmspush.hook.hms

class HookHMS {
    fun hook(classLoader: ClassLoader) {
        if (HookPushNC.canHook(classLoader)) {
            HookPushNC.hook(classLoader)
        }
    }
}
