package ro.mirceanistor.stf

import org.junit.Test

import static org.mockito.Mockito.spy
import static org.mockito.Mockito.when

/**
 * Created by mnistor on 13.03.2017.
 * ~
 */
public class STFTest {

//    String[] rawFilters = ["sdk=23",
//                           "free",
//                           "using=true",
//                           "sdk=18-22",
//                           "sdk=18+",
//                           //these filters require some reinterpretation
//                           "notes~[a-zA-Z0-9]*?:"
//    ]

    def gigelDevices = [
            new DeviceInfo("gigel_asdf", 480, 800, 15, "alab", "mockDevice", "adb connect 10.10.20.13:5555", "", false, "gigel13@mailinator.com"),
            new DeviceInfo("gigel_ghjk", 480, 800, 15, "alap", "mockDevice", "adb connect 10.10.20.13:5556", "", false, "gigel13@mailinator.com"),
            new DeviceInfo("gigel_qwer", 480, 800, 16, "orto", "mockDevice", "adb connect 10.10.20.13:5557", "", false, "gigel13@mailinator.com"),
            new DeviceInfo("gigel_tyui", 480, 800, 23, "cala", "mockDevice", "adb connect 10.10.20.13:5558", "", false, "gigel13@mailinator.com"),
    ]

    def freeDevices = [
            new DeviceInfo("free_asdf", 480, 800, 23, "alab", "mockDevice", "adb connect 10.10.20.14:5555", "", false, null),
            new DeviceInfo("free_ghjk", 480, 800, 15, "alap", "mockDevice", "adb connect 10.10.20.14:5556", "", false, null),
            new DeviceInfo("free_qwer", 480, 800, 16, "orto", "mockDevice", "adb connect 10.10.20.14:5557", "", false, null),
            new DeviceInfo("free_tyui", 480, 800, 24, "cala", "mockDevice", "adb connect 10.10.20.14:5558", "", false, null),
    ]

    def myDevices = [
            new DeviceInfo("my_asdf", 480, 800, 15, "alab", "mockDevice", "adb connect 10.10.20.15:5555", "", true, "my@mailinator.com"),
            new DeviceInfo("my_ghjk", 480, 800, 15, "alap", "mockDevice", "adb connect 10.10.20.15:5556", "", true, "my@mailinator.com"),
            new DeviceInfo("my_qwer", 480, 800, 16, "orto", "mockDevice", "adb connect 10.10.20.15:5557", "", true, "my@mailinator.com"),
            new DeviceInfo("my_tyui", 480, 800, 25, "cala", "mockDevice", "adb connect 10.10.20.15:5558", "", true, "my@mailinator.com"),
    ]

    def allDevices = gigelDevices + freeDevices + myDevices

    STFTest() {
        MainClass.VERBOSE_OUTPUT = true
    }


    @Test
    public void checkFilterByFreeDevices() throws Exception {
        STF mocked = spy(new STF(["free"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == freeDevices
    }

    @Test
    public void checkFilterByOccupiedDevices() throws Exception {
        STF mocked = spy(new STF(["free=false"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == gigelDevices + myDevices
    }


    @Test
    public void checkFilterByNotUsing() throws Exception {
        STF mocked = spy(new STF(["using=false"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == gigelDevices + freeDevices
    }

    @Test
    public void checkFilterByInvalidBoolean() throws Exception {
        STF mocked = spy(new STF(["using=gigel"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == gigelDevices + freeDevices
    }

    @Test
    public void checkFilterByMyDevices() throws Exception {
        STF mocked = spy(new STF(["using"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == myDevices
    }

    @Test
    public void checkFilterBySerial() throws Exception {
        STF mocked = spy(new STF(["serial=asdf"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == [gigelDevices[0], freeDevices[0], myDevices[0]]
    }

    @Test
    public void checkFilterBySdkEquals() throws Exception {
        STF mocked = spy(new STF(["sdk=23"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == [gigelDevices[3], freeDevices[0]]
    }

    @Test
    public void checkFilterBySdkPlus() throws Exception {
        STF mocked = spy(new STF(["sdk=24+"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == [freeDevices[3], myDevices[3]]
    }

    @Test
    public void checkFilterBySdkInterval() throws Exception {
        STF mocked = spy(new STF(["sdk=24-25"]))

        when(mocked.getAllDevices()).thenReturn(allDevices)

        def queryResult = mocked.queryDevices()

        assert queryResult == [freeDevices[3], myDevices[3]]
    }

}