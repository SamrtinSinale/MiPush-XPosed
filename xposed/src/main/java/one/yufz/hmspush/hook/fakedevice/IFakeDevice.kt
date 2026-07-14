package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.IFakeParam

interface IFakeDevice {
    fun fake(param: IFakeParam): Boolean
}
