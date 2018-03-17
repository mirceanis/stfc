/*******************************************************************************
 * Copyright 2017 Mircea Nistor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package ro.mirceanistor.stf

import org.junit.Before
import org.junit.Test

import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.spy

/**
 * Created by mnistor on 13.03.2017.
 * ~
 */
class FilterTest {

//    String[] rawFilters = ["sdk=23",
//                           "free",
//                           "using=true",
//                           "sdk=18-22",
//                           "sdk=18+",
//                           //these filters require some reinterpretation
//                           "notes~[a-zA-Z0-9]*?:"
//    ]

    def gigelDevices = [
            new DeviceInfo("gigel_asdf", 480, 800, 15, "alab", "mockDevice", "10.10.20.13:5555", "", false, "gigel13@mailinator.com"),
            new DeviceInfo("gigel_ghjk", 480, 800, 15, "alap", "mockDevice", "10.10.20.13:5556", "", false, "gigel13@mailinator.com"),
            new DeviceInfo("gigel_qwer", 480, 800, 16, "orto", "mockDevice", "10.10.20.13:5557", "", false, "gigel13@mailinator.com"),
            new DeviceInfo("gigel_tyui", 480, 800, 23, "cala", "mockDevice", "10.10.20.13:5558", "", false, "gigel13@mailinator.com"),
    ]

    def freeDevices = [
            new DeviceInfo("free_asdf", 480, 800, 23, "alab", "mockDevice", "10.10.20.14:5555", "", false, null),
            new DeviceInfo("free_ghjk", 480, 800, 15, "alap", "mockDevice", "10.10.20.14:5556", "", false, null),
            new DeviceInfo("free_qwer", 480, 800, 16, "orto", "mockDevice", "10.10.20.14:5557", "", false, null),
            new DeviceInfo("free_tyui", 480, 800, 24, "cala", "mockDevice", "10.10.20.14:5558", "", false, null),
    ]

    def myDevices = [
            new DeviceInfo("my_asdf", 480, 800, 15, "alab", "mockDevice", "10.10.20.15:5555", "", true, "my@mailinator.com"),
            new DeviceInfo("my_ghjk", 480, 800, 15, "alap", "mockDevice", "10.10.20.15:5556", "", true, "my@mailinator.com"),
            new DeviceInfo("my_qwer", 480, 800, 16, "orto", "mockDevice", "10.10.20.15:5557", "", true, "my@mailinator.com"),
            new DeviceInfo("my_tyui", 480, 800, 25, "cala", "mockDevice", "10.10.20.15:5558", "", true, "my@mailinator.com"),
    ]

    def allDevices = gigelDevices + freeDevices + myDevices

    FilterTest() {
        MainClass.VERBOSE_OUTPUT = true
    }

    @Before
    void setUp() {
        Properties props = new Properties()
        props.setProperty("STF_ACCESS_TOKEN", "some token")
        props.setProperty("STF_URL", "http://localhost")
        PropertyLoader.setProps(props)
    }


    @Test
    void checkFilterByFreeDevices() throws Exception {
        STF mocked = spy(new STF(["free"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == freeDevices
    }

    @Test
    void checkFilterByOccupiedDevices() throws Exception {
        STF mocked = spy(new STF(["free=false"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == gigelDevices + myDevices
    }


    @Test
    void checkFilterByNotUsing() throws Exception {
        STF mocked = spy(new STF(["using=false"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == gigelDevices + freeDevices
    }

    @Test
    void checkFilterByInvalidBoolean() throws Exception {
        STF mocked = spy(new STF(["using=gigel"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == gigelDevices + freeDevices
    }

    @Test
    void checkFilterByMyDevices() throws Exception {
        STF mocked = spy(new STF(["using"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == myDevices
    }

    @Test
    void checkFilterBySerial() throws Exception {
        STF mocked = spy(new STF(["serial=asdf"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == [gigelDevices[0], freeDevices[0], myDevices[0]]
    }

    @Test
    void checkFilterBySdkEquals() throws Exception {
        STF mocked = spy(new STF(["sdk=23"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == [gigelDevices[3], freeDevices[0]]
    }

    @Test
    void checkFilterBySdkPlus() throws Exception {
        STF mocked = spy(new STF(["sdk=24+"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == [freeDevices[3], myDevices[3]]
    }

    @Test
    void checkFilterBySdkInterval() throws Exception {
        STF mocked = spy(new STF(["sdk=24-25"]))

        doReturn(allDevices).when(mocked).getAllDevices()

        def queryResult = mocked.queryDevices()

        assert queryResult == [freeDevices[3], myDevices[3]]
    }
}
