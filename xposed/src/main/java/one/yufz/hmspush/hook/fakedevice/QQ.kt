package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.IFakeParam
import one.yufz.hmspush.hook.XLog

class QQ : Common() {
    companion object {
        private const val TAG = "QQ"
    }

    override fun fake(param: IFakeParam): Boolean {
        // In API 102, process name is not available at package lifecycle level.
        // We rely on packageName matching; QQ helper processes are loaded separately.
        // For MSF process, super.fake handles it when loaded.
        return super.fake(param)
    }
}
