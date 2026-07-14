package one.yufz.hmspush.hook.hms

import android.os.Binder
import android.os.IBinder
import one.yufz.hmspush.common.BridgeUri
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.XposedAPI

object HmsPushService : Binder() {
    fun getBinder(): IBinder = this
    private const val TAG = "HmsPushService"

    fun notifyPushSignChanged() {
        XLog.d(TAG, "notifyPushSignChanged()")
        getCurrentApplication().let { app ->
            BridgeUri.PUSH_SIGN.notifyContentChanged(app)
        }
    }

    fun notifyPushHistoryChanged() {
        XLog.d(TAG, "notifyPushHistoryChanged()")
        getCurrentApplication().let { app ->
            BridgeUri.PUSH_HISTORY.notifyContentChanged(app)
        }
    }

    private fun getCurrentApplication(): android.app.Application = XposedAPI.currentApplication()
}
