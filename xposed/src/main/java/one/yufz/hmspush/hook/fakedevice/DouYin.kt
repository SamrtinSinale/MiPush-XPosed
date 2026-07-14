package one.yufz.hmspush.hook.fakedevice

import android.os.Build
import one.yufz.hmspush.hook.IFakeParam
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.hookMethod
import org.json.JSONArray
import org.json.JSONObject

class DouYin : Common() {
    companion object {
        private const val TAG = "DouYin"
    }

    override fun fake(param: IFakeParam): Boolean {
        super.fake(param)
        if (Build.DISPLAY.contains("flyme", true) || Build.USER.contains("flyme", true)) {
            fakeProperty("ro.build.display.id" to "")
            fakeProperty("ro.build.user" to "")
            fakeProperty("ro.build.flyme.version" to "")
            fakeProperty("ro.flyme.version.id" to "")
        }

        val classLoader = when (param) {
            is one.yufz.hmspush.hook.FakeDeviceParam -> param.classLoader
            else -> return true
        }

        val classAppLogNetworkClient = classLoader.findClass("com.ss.android.ugc.aweme.statistic.AppLogNetworkClient")
        val classReqContext = classLoader.findClass("com.bytedance.common.utility.NetworkClient\$ReqContext")
        classAppLogNetworkClient.hookMethod("post", String::class.java, List::class.java, Map::class.java, classReqContext) { chain ->
            val url = chain.getArg(0) as String
            val result = chain.proceed() as? String

            if (url.contains("/cloudpush/update_sender/") && result != null) {
                XLog.d(TAG, result)
                val json = JSONObject(result)
                val allowPushList = json.getJSONArray("allow_push_list")
                val newArray = tryInsertMiPushChannel(allowPushList)
                json.put("allow_push_list", newArray)
                json.toString()
            } else {
                result
            }
        }
        return true
    }

    private fun tryInsertMiPushChannel(originArray: JSONArray): JSONArray {
        val array = ArrayList<Int>()
        for (i in 0 until originArray.length()) {
            array.add(originArray.getInt(i))
        }
        array.remove(1)
        array.add(0, 1)
        return JSONArray(array)
    }
}
