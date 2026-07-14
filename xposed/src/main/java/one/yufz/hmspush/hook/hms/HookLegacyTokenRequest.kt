package one.yufz.hmspush.hook.hms

import android.content.Context
import android.content.Intent
import one.yufz.hmspush.common.HMS_CORE_PUSH_ACTION_REGISTRATION
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.callMethod
import one.yufz.xposed.findClass
import one.yufz.xposed.get
import one.yufz.xposed.hook
import java.lang.reflect.Method

object HookLegacyTokenRequest {
    private const val TAG = "HookLegacyTokenRequest"

    fun hook(classLoader: ClassLoader) {
        val classKmsMessageCenter = try {
            classLoader.findClass("com.huawei.hms.fwkit.message.KmsMessageCenter")
        } catch (e: Throwable) {
            null
        }
        XLog.d(TAG, "hook() called with: classKmsMessageCenter = ${classKmsMessageCenter?.classLoader}")

        classKmsMessageCenter?.hookMethod("register", String::class.java, Class::class.java, Boolean::class.java, Boolean::class.java) { chain ->
            val uri = chain.getArg(0) as String
            if (uri == "push.gettoken") {
                // unhook self
                chain.executable.let { executable ->
                    one.yufz.xposed.XposedAPI.requireApi().hook(executable).intercept { c -> c.proceed() }
                }
                hookGetTokenProcess(chain.getArg(1) as Class<*>)
            }
            chain.proceed()
        }
    }

    private fun hookGetTokenProcess(clazz: Class<*>) {
        XLog.d(TAG, "hookGetTokenProcess() called with: clazz = $clazz")
        val classLoader = clazz.classLoader
        val classIMessageEntity = classLoader.findClass("com.huawei.hms.support.api.transport.IMessageEntity")
        val classTokenResp = classLoader.findClass("com.huawei.hms.support.api.entity.push.TokenResp")

        // Find methods by exact parameters using reflection
        val methods = clazz.superclass.declaredMethods.filter { method ->
            method.returnType == Void.TYPE &&
            method.parameterTypes.size == 2 &&
            method.parameterTypes[0] == classIMessageEntity &&
            method.parameterTypes[1] in listOf(Int::class.java, Class::class.java)
        }

        methods.forEach { method ->
            XLog.d(TAG, "hookGetTokenProcess() called with: method = $method")

            method.hook { chain ->
                val result = chain.proceed()
                if (chain.getArg(0).javaClass == classTokenResp) {
                    mockReceive(chain.getThisObject(), chain.getArg(0))
                }
                result
            }
        }
    }

    private fun mockReceive(process: Any, response: Any) {
        XLog.d(TAG, "mockReceive() called")

        val context: Context = process["context"]
        val packageName = process.get<Any>("clientIdentity").callMethod("getPackageName") as String
        @Suppress("UNCHECKED_CAST")
        val token = response.get<Any>("token")
        val intent = Intent(HMS_CORE_PUSH_ACTION_REGISTRATION)
        intent.setPackage(packageName)
        intent.putExtra("device_token", (token as? String)?.toByteArray() ?: token.toString().toByteArray())

        XLog.d(TAG, "mockReceive() called with: packageName = $packageName")

        context.sendBroadcast(intent)
    }

    private fun Class<*>.hookMethod(name: String, vararg paramTypes: Class<*>, interceptor: (io.github.libxposed.api.XposedInterface.Chain) -> Any?) {
        val method = getDeclaredMethod(name, *paramTypes).apply { isAccessible = true }
        one.yufz.xposed.XposedAPI.requireApi().hook(method).intercept { chain -> interceptor(chain) }
    }
}
