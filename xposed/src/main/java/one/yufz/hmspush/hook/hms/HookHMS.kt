package one.yufz.hmspush.hook.hms

class HookHMS {
    companion object {
        private const val TAG = "HookHMS"
    }

    fun hook(classLoader: ClassLoader) {
        HookBackgroundActivityStart.hook(classLoader)

        if (HookPushNC.canHook(classLoader)) {
            HookPushNC.hook(classLoader)
        }
    }
}
