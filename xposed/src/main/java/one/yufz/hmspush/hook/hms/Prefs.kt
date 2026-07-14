package one.yufz.hmspush.hook.hms

import android.content.Context
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.XposedAPI

object Prefs {
    private const val TAG = "Prefs"
    private const val HMSPUSH_PREF_NAME = "HMSPush"

    private val pref by lazy {
        getCurrentApplication().getSharedPreferences(HMSPUSH_PREF_NAME, Context.MODE_PRIVATE)
    }

    val signCheckEnabled: Boolean
        get() = pref.getBoolean("sign_check", true)

    val notificationStyle: Int
        get() = pref.getInt("notification_style", 1)

    val notificationColor: Int
        get() = pref.getInt("notification_color", 0)

    val avatarRadius: Int
        get() = pref.getInt("avatar_radius", 0)

    val isDisableSignature: Boolean
        get() = pref.getBoolean("disable_signature", false)

    val useSelfNotificationManager: Boolean
        get() = pref.getBoolean("self_notification_manager", false)

    val iconColor: Int
        get() = pref.getInt("icon_color", 0)

    val isEnableLegacyPushService: Boolean
        get() = pref.getBoolean("enable_legacy_push_service", false)

    val prefModel: one.yufz.hmspush.common.model.PrefsModel
        get() = one.yufz.hmspush.common.model.PrefsModel(
            disableSignature = isDisableSignature,
            groupMessageById = pref.getBoolean("group_message_by_id", true),
            useCustomIcon = pref.getBoolean("use_custom_icon", false),
            tintIconColor = pref.getBoolean("tint_icon_color", true),
        )

    private fun getCurrentApplication(): android.app.Application = XposedAPI.currentApplication()
}
