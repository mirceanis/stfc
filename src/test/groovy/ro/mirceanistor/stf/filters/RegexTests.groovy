package ro.mirceanistor.stf.filters

import org.junit.Before
import org.junit.Test
import ro.mirceanistor.stf.PropertyLoader
import ro.mirceanistor.stf.STF

import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.spy
import static ro.mirceanistor.stf.filters.FilterTest.*

/**
 * Test filters that accept regex as parameter
 */
class RegexTests {

    @Before
    void setUp() {
        Properties properties = new Properties()
        properties.setProperty("STF_ACCESS_TOKEN", "some token")
        properties.setProperty("STF_URL", "http://localhost")
        PropertyLoader.setProps(properties)
    }

    // SERIAL - done
    // CONNECT - done
    // NOTES

    @Test
    void serialEquals() {

        STF mockedSTF = spy(new STF(["serial=gigel_asdf"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == [gigelDevices[0]]
    }

    @Test
    void serialContains() {

        STF mockedSTF = spy(new STF(["serial=.*ghjk"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == [gigelDevices[1], freeDevices[1], myDevices[1]]
    }

    @Test
    void serialMatchesSome() {

        STF mockedSTF = spy(new STF(["serial=gigel_qwer|free_qwer|my_qwer"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == [gigelDevices[2], freeDevices[2], myDevices[2]]
    }

    @Test
    void serialMatchesAll() {
        STF mockedSTF = spy(new STF(["serial=.*"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == allDevices
    }

    @Test
    void serialMatchesAnyExcept() {
        STF mockedSTF = spy(new STF(["serial=(?!gigel_).*"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == freeDevices + myDevices
    }

    @Test
    void connectEquals() {

        STF mockedSTF = spy(new STF(["connect=10.10.20.13:5555"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == [gigelDevices[0]]
    }

    @Test
    void connectContains() {

        STF mockedSTF = spy(new STF(["connect=.*5556"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == [gigelDevices[1], freeDevices[1], myDevices[1]]
    }

    @Test
    void connectMatchesSome() {

        STF mockedSTF = spy(new STF(["connect=10.10.20.13:5557|10.10.20.14:5557|10.10.20.15:5557"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == [gigelDevices[2], freeDevices[2], myDevices[2]]
    }

    @Test
    void connectMatchesAll() {
        STF mockedSTF = spy(new STF(["connect=.*"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == allDevices
    }

    @Test
    void connectMatchesAnyExcept() {
        STF mockedSTF = spy(new STF(["connect=(?!.*13).*"]))

        doReturn(allDevices).when(mockedSTF).getAllDevices()

        def queryResult = mockedSTF.queryDevices()

        assert queryResult == freeDevices + myDevices
    }
}
