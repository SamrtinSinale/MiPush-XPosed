package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.IFakeParam
import one.yufz.hmspush.hook.XLog

object FakeDevice {
    private const val TAG = "FakeDevice"

    private val Default = arrayOf(Common::class.java)
    private val FakeDeviceConfig: Map<String, Array<Class<out IFakeDevice>>> = mapOf(
        "com.coolapk.market" to arrayOf(CoolApk::class.java),
        "com.tencent.mobileqq" to arrayOf(QQ::class.java),
        "com.tencent.tim" to arrayOf(QQ::class.java),
        "com.sankuai.meituan" to arrayOf(FakeEmuiOnly::class.java),
        "com.sankuai.meituan.takeoutnew" to arrayOf(FakeEmuiOnly::class.java),
        "com.dianping.v1" to arrayOf(FakeEmuiOnly::class.java),
        "com.eg.android.AlipayGphone" to arrayOf(Alipay::class.java),
        "com.xunmeng.pinduoduo" to arrayOf(PinDuoDuo::class.java),
        "com.ss.android.ugc.aweme" to arrayOf(DouYin::class.java),
    )

    fun fake(param: IFakeParam) {
        XLog.d(TAG, "fake() called with: packageName = ${param.packageName}")
        if (param.packageName == "com.google.android.webview") {
            XLog.d(TAG, "fake() called, ignore ${param.packageName}")
            return
        }

        val fakes = FakeDeviceConfig[param.packageName] ?: Default
        fakes.forEach { it.newInstance().fake(param) }
    }
}
