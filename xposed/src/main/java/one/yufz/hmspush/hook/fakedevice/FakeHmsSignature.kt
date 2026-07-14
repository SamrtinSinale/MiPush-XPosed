package one.yufz.hmspush.hook.fakedevice

import android.content.pm.PackageInfo
import android.util.Base64
import dalvik.system.DexClassLoader
import one.yufz.hmspush.common.HMS_CORE_SIGNATURE
import one.yufz.hmspush.common.HMS_PACKAGE_NAME
import one.yufz.hmspush.hook.IFakeParam
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.XposedAPI
import one.yufz.xposed.findClass
import one.yufz.xposed.hook
import one.yufz.xposed.hookMethod
import one.yufz.xposed.set

object FakeHmsSignature {
    private const val TAG = "FakeHmsSignature"

    private var verifyApkHashHooked = false
    private var verifyApkHashUnhook: io.github.libxposed.api.XposedInterface.HookHandle? = null

    fun hook(param: IFakeParam) {
        val classLoader = when (param) {
            is one.yufz.hmspush.hook.FakeDeviceParam -> param.classLoader
            else -> return
        }

        XLog.d(TAG, "hook() called with: processName = ${param.packageName}")

        tryHookVerifyApkHash(classLoader)

        if (!verifyApkHashHooked) {
            verifyApkHashUnhook = DexClassLoader::class.java.declaredConstructors.first { 
                it.parameterCount == 4
            }.hook { chain ->
                val result = chain.proceed()
                tryHookVerifyApkHash(chain.getThisObject() as ClassLoader)
                result
            }
        }

        val classApplicationPackageManager = classLoader.findClass("android.app.ApplicationPackageManager")
        classApplicationPackageManager.hookMethod("getPackageInfo", String::class.java, Int::class.java) { chain ->
            val packageName = chain.getArg(0) as String
            val result = chain.proceed() as? PackageInfo
            if (packageName == HMS_PACKAGE_NAME && result != null) {
                result.signatures?.firstOrNull()?.let {
                    it["mSignature"] = Base64.decode(HMS_CORE_SIGNATURE, Base64.NO_WRAP)
                }
            }
            result
        }
    }

    private fun tryHookVerifyApkHash(classLoader: ClassLoader) {
        if (verifyApkHashHooked) return

        try {
            classLoader.findClass("com.huawei.hms.utils.ReadApkFileUtil")
                .hookMethod("verifyApkHash", String::class.java) { _ -> true }

            XLog.d(TAG, "tryHookVerifyApkHash: verifyApkHash() hooked")

            verifyApkHashHooked = true
            verifyApkHashUnhook?.unhook()
        } catch (e: ClassNotFoundException) {
            XLog.d(TAG, "tryHookVerifyApkHash: ClassNotFoundError")
        } catch (e: NoSuchMethodError) {
            XLog.d(TAG, "tryHookVerifyApkHash: NoSuchMethodError")
        } catch (e: Throwable) {
            // ignore
        }
    }
}
