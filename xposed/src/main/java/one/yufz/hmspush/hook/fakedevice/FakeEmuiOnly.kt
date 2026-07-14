package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.IFakeParam

class FakeEmuiOnly : IFakeDevice {
    override fun fake(param: IFakeParam): Boolean {
        fakeProperty(Property.EMUI_VERSION)
        return true
    }
}
