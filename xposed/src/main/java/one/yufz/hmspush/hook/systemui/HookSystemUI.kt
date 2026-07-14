package one.yufz.hmspush.hook.systemui

import android.app.Notification
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.callMethod
import one.yufz.xposed.findClass
import one.yufz.xposed.get
import one.yufz.xposed.hook
import one.yufz.xposed.hookAllMethods
import one.yufz.xposed.hookMethod
import one.yufz.xposed.XposedAPI

class HookSystemUI {
    companion object {
        private const val TAG = "HookSystemUI"
    }

    private val ID_ICON_IS_PRE_L: Int by lazy {
        val app = getCurrentApplication()
        app.resources.getIdentifier("icon_is_pre_L", "id", app.packageName)
    }

    private fun getCurrentApplication(): android.app.Application = XposedAPI.currentApplication()

    fun hook(classLoader: ClassLoader) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            classLoader.findClass("com.android.systemui.statusbar.notification.icon.IconManager")
                .hookAllMethods("setIcon") { chain ->
                    val result = chain.proceed()
                    val iconView = chain.getArg(2) as View
                    iconView.setTag(ID_ICON_IS_PRE_L, true)
                    result
                }
        } else {
            classLoader.findClass("com.android.systemui.statusbar.notification.collection.NotificationEntry")
                .hookMethod("setIconTag", Int::class.java, Any::class.java) { chain ->
                    if (chain.getArg(0) == ID_ICON_IS_PRE_L) {
                        val newArgs = chain.args.toMutableList()
                        newArgs[1] = true
                        chain.proceed(newArgs.toTypedArray())
                    } else {
                        chain.proceed()
                    }
                }
        }

        Notification.Builder::class.java.hookAllMethods("processSmallIconColor") { chain ->
            val thisObject = chain.getThisObject()
            val context: Context = thisObject["mContext"]
            val smallIcon = chain.getArg(0) as android.graphics.drawable.Icon
            val contentView = chain.getArg(1) as RemoteViews
            val p = chain.getArg(2)

            val colorUtil = thisObject.callMethod("getColorUtil")!!
            val isGrayscaleIcon = colorUtil.callMethod("isGrayscaleIcon", context, smallIcon) as Boolean

            if (!isGrayscaleIcon) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    contentView.setInt(android.R.id.icon, "setBackgroundColor", thisObject.callMethod("getBackgroundColor", p) as Int)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    contentView.setInt(android.R.id.icon, "setOriginalIconColor", 1)
                }
                true
            } else {
                chain.proceed()
            }
        }
    }
}
