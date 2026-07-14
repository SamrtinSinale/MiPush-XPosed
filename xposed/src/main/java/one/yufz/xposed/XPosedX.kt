package one.yufz.xposed

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Chain
import java.lang.reflect.*

/**
 * Global XposedAPI holder, initialized by XposedMod.onModuleLoaded().
 */
object XposedAPI {
    lateinit var api: XposedInterface
        internal set

    fun requireApi(): XposedInterface {
        if (!::api.isInitialized) throw IllegalStateException("XposedAPI not initialized")
        return api
    }

    /**
     * Get the current Application via ActivityThread reflection.
     * Works even when Context is not directly available.
     */
    fun currentApplication(): android.app.Application {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val method = activityThreadClass.getDeclaredMethod("currentApplication").apply { isAccessible = true }
        return method.invoke(null) as android.app.Application
    }

    /**
     * Get PackageManager and resolve UID for a package.
     */
    fun getPackageUid(packageName: String): Int {
        val app = currentApplication()
        val pm = android.content.Context::class.java.getMethod("getPackageManager").apply { isAccessible = true }.invoke(app)
        return pm.javaClass.getMethod("getPackageUid", String::class.java, Int::class.java).apply { isAccessible = true }
            .invoke(pm, packageName, 0) as Int
    }
}

// ---------------------------------------------------------------------------
// Method/Constructor invocation helpers (replaces XposedHelpers.callMethod)
// ---------------------------------------------------------------------------

fun Any.callMethod(methodName: String, vararg args: Any?): Any? {
    val paramTypes = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
    val method = javaClass.getDeclaredMethod(methodName, *paramTypes).apply { isAccessible = true }
    return method.invoke(this, *args)
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.callMethod(methodName: String, parameterTypes: Array<Class<*>>, vararg args: Any?): T? {
    val method = javaClass.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
    return method.invoke(this, *args) as? T?
}

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? {
    val paramTypes = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
    val method = getDeclaredMethod(methodName, *paramTypes).apply { isAccessible = true }
    return method.invoke(null, *args)
}

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.callStaticMethod(methodName: String, parameterTypes: Array<Class<*>>, vararg args: Any?): T? {
    val method = getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
    return method.invoke(null, *args) as? T?
}

// ---------------------------------------------------------------------------
// Class finding (replaces XposedHelpers.findClass)
// ---------------------------------------------------------------------------

fun ClassLoader.findClass(className: String): Class<*> {
    return Class.forName(className, false, this)
}

fun ClassLoader.findClassOrNull(className: String): Class<*>? {
    return try {
        Class.forName(className, false, this)
    } catch (_: ClassNotFoundException) {
        null
    }
}

// ---------------------------------------------------------------------------
// Constructor invocation (replaces XposedHelpers.newInstance)
// ---------------------------------------------------------------------------

fun <T> Class<T>.newInstance(vararg args: Any?): T {
    val paramTypes = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
    val constructor = getDeclaredConstructor(*paramTypes).apply { isAccessible = true }
    return constructor.newInstance(*args)
}

fun <T> Class<T>.newInstance(parameterTypes: Array<Class<*>>, vararg args: Any?): T {
    val constructor = getDeclaredConstructor(*parameterTypes).apply { isAccessible = true }
    return constructor.newInstance(*args)
}

// ---------------------------------------------------------------------------
// Hook helpers for API 102 interceptor chain
// ---------------------------------------------------------------------------

/**
 * Extension to hook a method. Returns the HookHandle so it can be unhooked later.
 */
fun Method.hook(interceptor: (Chain) -> Any?): XposedInterface.HookHandle {
    val api = XposedAPI.requireApi()
    return api.hook(this).intercept(object : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            return interceptor(chain)
        }
    })
}

/**
 * Hook a constructor, returns the HookHandle.
 */
fun <T> Constructor<T>.hook(interceptor: (Chain) -> Any?): XposedInterface.HookHandle {
    val api = XposedAPI.requireApi()
    return api.hook(this).intercept(object : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            return interceptor(chain)
        }
    })
}

/**
 * Hook all methods with the given name on this class.
 */
fun Class<*>.hookAllMethods(methodName: String, interceptor: (Chain) -> Any?) {
    declaredMethods.filter { it.name == methodName }.forEach { method ->
        XposedAPI.requireApi().hook(method).intercept { chain -> interceptor(chain) }
    }
    // Also check superclass methods
    var superClass = superclass
    while (superClass != null) {
        superClass.declaredMethods.filter { it.name == methodName }.forEach { method ->
            XposedAPI.requireApi().hook(method).intercept { chain -> interceptor(chain) }
        }
        superClass = superClass.superclass
    }
}

/**
 * Hook a method by class name + classLoader + method name + parameter types.
 */
fun hookMethod(
    className: String,
    classLoader: ClassLoader,
    methodName: String,
    vararg parameterTypes: Class<*>,
    interceptor: (Chain) -> Any?
): XposedInterface.HookHandle {
    val clazz = classLoader.findClass(className)
    val method = findMethodOrNull(clazz, methodName, *parameterTypes)
        ?: throw NoSuchMethodError("$className.$methodName")
    return XposedAPI.requireApi().hook(method).intercept { chain -> interceptor(chain) }
}

/**
 * Hook a constructor by class name + classLoader + parameter types.
 */
fun hookConstructor(
    className: String,
    classLoader: ClassLoader,
    vararg parameterTypes: Class<*>,
    interceptor: (Chain) -> Any?
): XposedInterface.HookHandle {
    val clazz = classLoader.findClass(className)
    val constructor = clazz.getDeclaredConstructor(*parameterTypes).apply { isAccessible = true }
    return XposedAPI.requireApi().hook(constructor).intercept { chain -> interceptor(chain) }
}

/**
 * Hook all constructors of a class.
 */
fun Class<*>.hookAllConstructor(interceptor: (Chain) -> Any?) {
    declaredConstructors.forEach { ctor ->
        XposedAPI.requireApi().hook(ctor).intercept { chain -> interceptor(chain) }
    }
}

// ---------------------------------------------------------------------------
// Fluent hook helpers (for Class.hookMethod style like the old DSL)
// ---------------------------------------------------------------------------

fun Class<*>.hookMethod(
    methodName: String,
    vararg parameterTypes: Class<*>,
    interceptor: (Chain) -> Any?
): XposedInterface.HookHandle {
    val method = findMethodOrNull(this, methodName, *parameterTypes)
        ?: throw NoSuchMethodError("${name}.$methodName")
    return XposedAPI.requireApi().hook(method).intercept { chain -> interceptor(chain) }
}

// ---------------------------------------------------------------------------
// Reflection utilities
// ---------------------------------------------------------------------------

/**
 * Find method in class hierarchy, returns null if not found.
 */
fun findMethodOrNull(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method? {
    var current: Class<*>? = clazz
    while (current != null) {
        try {
            return current.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
        } catch (_: NoSuchMethodException) {
            current = current.superclass
        }
    }
    return null
}

// ---------------------------------------------------------------------------
// Field access helpers (replaces XposedHelpers field access)
// ---------------------------------------------------------------------------

private fun findField(clazz: Class<*>, fieldName: String): Field {
    var current: Class<*>? = clazz
    while (current != null) {
        try {
            return current.getDeclaredField(fieldName).apply { isAccessible = true }
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    throw NoSuchFieldException("${clazz.name}.$fieldName")
}

inline fun <reified T> Any.getOrNull(name: String): T? = getField(name, T::class.java)

inline operator fun <reified T> Any.get(name: String): T = getField(name, T::class.java)!!

inline operator fun <reified T> Any.set(name: String, value: T?) = setField(name, value, T::class.java)

fun <T> Any.getField(name: String, fieldClazz: Class<T>): T? {
    val obj = if (this is Class<*>) null else this
    val thisClass = if (this is Class<*>) this else this.javaClass
    val field = findField(thisClass, name)

    @Suppress("UNCHECKED_CAST")
    val value = when (fieldClazz) {
        Boolean::class.java -> field.getBoolean(obj) as Any
        Byte::class.java -> field.getByte(obj) as Any
        Char::class.java -> field.getChar(obj) as Any
        Double::class.java -> field.getDouble(obj) as Any
        Float::class.java -> field.getFloat(obj) as Any
        Int::class.java -> field.getInt(obj) as Any
        Long::class.java -> field.getLong(obj) as Any
        Short::class.java -> field.getShort(obj) as Any
        else -> field.get(obj)
    }
    return value as? T
}

fun <T> Any.setField(name: String, value: T?, fieldClass: Class<T>) {
    val obj = if (this is Class<*>) null else this
    val thisClass = if (this is Class<*>) this else this.javaClass
    val field = findField(thisClass, name)

    when (fieldClass) {
        Boolean::class.java -> field.setBoolean(obj, value as Boolean)
        Byte::class.java -> field.setByte(obj, value as Byte)
        Char::class.java -> field.setChar(obj, value as Char)
        Double::class.java -> field.setDouble(obj, value as Double)
        Float::class.java -> field.setFloat(obj, value as Float)
        Int::class.java -> field.setInt(obj, value as Int)
        Long::class.java -> field.setLong(obj, value as Long)
        Short::class.java -> field.setShort(obj, value as Short)
        else -> field.set(obj, value as T)
    }
}

/**
 * Get an invoker for the given method (bypasses access checks).
 */
fun getInvoker(method: Method): XposedInterface.Invoker<*, Method> {
    return XposedAPI.requireApi().getInvoker(method)
}

fun <T> getInvoker(constructor: Constructor<T>): XposedInterface.CtorInvoker<T> {
    return XposedAPI.requireApi().getInvoker(constructor)
}
