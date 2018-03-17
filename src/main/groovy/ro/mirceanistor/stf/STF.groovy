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

import groovyx.net.http.HTTPBuilder

import java.util.logging.Level
import java.util.logging.Logger

import static groovyx.gpars.GParsPool.withPool
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*
import static ro.mirceanistor.stf.Filters.*
import static ro.mirceanistor.stf.MainClass.VERBOSE_OUTPUT

/**
 * A class representing a subset of the STF API relating to device reservation and connection.
 * This provides utility tasks for reserving/releasing devices and for connection to reserved devices.
 */
class STF {

    HTTPBuilder stf_api

    def filters

    //per user access token
    def STF_ACCESS_TOKEN

    //the STF api URL
    def STF_URL

    //the logger used by the project
    Logger logger

    /**
     * STF class constructor.
     * This will be used in gradle tasks
     * @param project the Project that this class is being used from
     */
    STF(Collection<String> rawFilters) {

        (STF_URL, STF_ACCESS_TOKEN) = PropertyLoader.getStfCoordinates()

        logger = Logger.getLogger("hello")

        if (VERBOSE_OUTPUT) {
            logger.setLevel(Level.INFO)
        } else {
            logger.setLevel(Level.WARNING)
        }

        filters = parseFilters(rawFilters)

        stf_api = new HTTPBuilder("$STF_URL")
        stf_api.setHeaders([Authorization: "Bearer $STF_ACCESS_TOKEN"])

        stf_api.handler.'401' = { resp ->
            throw new RuntimeException("Unauthorized request: ${resp.statusLine}\n" +
                    "\nThis is most likely caused by a STF_ACCESS_TOKEN mismatch.\n" + PropertyLoader.TOKEN_GENERATION_INSTRUCTIONS)
        }
    }

    /**
     * Use the STF API to reserve a particular device
     * @param serial the target device SERIAL
     * @return the response given by the server (can be used for logging)
     */
    def reserveDevice(String serial) {
        def reserveDeviceResponse = stf_api.request(POST, JSON) { req ->
            body = [serial: serial]
            uri.path = '/api/v1/user/devices'

            response.failure = { resp ->
                throw new RuntimeException("Something went wrong with the request for serial=\"$serial\", we got: ${resp.statusLine}\n" +
                        "Most likely, the device has been disconnected or has been reserved in the meantime")
            }
        }

        return reserveDeviceResponse
    }

    /**
     * Release a reserved device from usage
     * @param serial the target device SERIAL
     * @return the response given by the server (can be used for logging)
     */
    def releaseDevice(String serial) {
        def removeDeviceResponse = stf_api.request(DELETE, JSON) { req ->
            uri.path = "/api/v1/user/devices/$serial"
        }
        return removeDeviceResponse
    }

    /**
     * Use the STF API to list all the FREE devices and generate a list of their SERIALs
     * @return a list of SERIALs from devices that are available to use
     */
    Collection<String> getAvailableDeviceSerials() {
        def response = stf_api.request(GET, JSON) { req ->
            uri.path = '/api/v1/devices'
        }

        def deviceList = response.devices.findAll {
            //devices that are ready and not reserved by other users
            (it.owner == null) && it.ready == true && it.present == true
        }

        return deviceList*.serial
    }

    /**
     * Check if any of the given set of `filters` prefix the provided `filterFullName` and return the filter
     * @param filterFullName the full name of the filter to check against
     * @param filters the set of filters passed
     * @return the FIRST parsed filter that matches the check or `null` if there is none
     */
    def getFilter(String filterFullName) {

        def matches = filters.findAll {
            filterFullName.startsWith(it.key as String)
        }

        if (matches.size() > 0) {
            if (matches.size() > 1) {
                logger.warning("Multiple filters match; returning the first match ONLY $filterFullName: $filters;")
            }
            return matches[0]
        }
    }

    /**
     * Returns a collection of ALL devices that are usable or in use
     * @return Collection < DeviceInfo >
     */
    def getAllDevices() {
        def response = stf_api.request(GET, JSON) { req ->
            uri.path = '/api/v1/devices'
        }

        return response.devices.findAll {
            //skip disconnected or preparing
            if (it.ready == false || it.present == false) {
                return false
            }
            true
        }.collect {
            new DeviceInfo(it.serial, it.display.width, it.display.height, it.sdk, it.name, it.model, it.remoteConnectUrl, it.notes, it.using, it.owner?.email)
        }
    }

    def getConnectionString(def serial) {

        def resp = stf_api.request(POST, JSON) { req ->
            body = []
            uri.path = "/api/v1/user/devices/${serial}/remoteConnect".toString()
        }

        return resp.remoteConnectUrl
    }

    /**
     * Filter through devices provided by STF by [sdk, connectionString, notes and reservation status]
     * @param filters set of filters usually provided by command line
     * @param quiet whether or not to output only device serials
     * @return
     */
    Collection<DeviceInfo> queryDevices() {

        //only unreserved devices
        def filterByAvailability = getFilter(F_FREE)

        //only devices in use by current user (ignores `freeDevices` filter)
        def filterByCurrentUser = getFilter(F_USING)

        //devices matching a particular Android SDK (int)
        def sdkFilter = getFilter(F_SDK)

        //devices whose `serial` field contains the given substring
        def filterBySerial = getFilter(F_SERIAL)?.value

        //devices whose `adb connection` field contains the given substring
        def filterByConnection = getFilter(F_CONNECT)?.value

        //devices whose `notes` field string contains the given substring
        def filterByNotes = getFilter(F_NOTES)?.value

        getAllDevices().findResults {

            if (filterByAvailability) {
                boolean isFree = (it.ownerEmail == null)
                if (filterByAvailability.value.toBoolean() == !isFree) {
                    logger?.info("skipping device owned by `${it.ownerEmail}` because `$F_FREE` filter is ${filterByAvailability.value}")
                    return null
                }
            }

            if (filterByCurrentUser) {
                if (filterByCurrentUser.value.toBoolean() != it.using) {
                    logger?.info("skipping device `${it.serial}` because in_use_by_current_user=${it.using} and `$F_USING` filter is set to `${filterByCurrentUser.value}`")
                    return null
                }
            }

            if (sdkFilter) {
                def deviceSDK = (int) it.sdk
                def matchesSDK

                switch (sdkFilter.value) {
                //for range filters of the form `sdk=18-23`
                    case ~/^[0-9]+-[0-9]+$/:
                        def range = (sdkFilter.value as String).split("-").collect { it as int }
                        matchesSDK = (deviceSDK in range[0]..range[1])
                        break

                //for range filters of the form `sdk=18+`
                    case ~/^[0-9]+\+$/:
                        matchesSDK = (deviceSDK >= ( (sdkFilter.value as String)[0..-2] as int))
                        break

                //simple case `sdk=23`
                    default:
                        matchesSDK = (deviceSDK == (sdkFilter.value as int))
                        break
                }

                if (!matchesSDK) {
                    logger?.info("skipping device with sdk=${it.sdk} because `$F_SDK=$sdkFilter` filter is active")
                    return null
                }
            }


            if (filterByConnection && !it.remoteConnectUrl?.contains(filterByConnection as String)) {
                logger?.info("skipping device with connectionString=${it.remoteConnectUrl} because `$F_CONNECT=$filterByConnection` filter is active")
                return null
            }

            if (filterByNotes && !it.notes?.contains(filterByNotes as String)) {
                logger?.info("skipping device with notes=${it.notes} because `$F_NOTES=$filterByNotes` filter is active")
                return null
            }

            if (filterBySerial && !it.serial?.contains(filterBySerial as String)) {
                logger?.info("skipping device with serial=${it.serial} because `$F_SERIAL=$filterBySerial` filter is active")
                return null
            }

            return it
        }
    }

    /**
     * Use the STF API to reserve a list of devices
     * @param devicesToReserve an array of SERIALs to reserve. If the array contains the item "all", then all the devices provided by STF will be reserved
     */
    def reserveDevicesWithSerials(Collection<DeviceInfo> devicesToReserve) {
        def availableDeviceSerials = getAvailableDeviceSerials()
        logger?.info "availableDeviceSerials are: $availableDeviceSerials; we're going to connect to $devicesToReserve"

        devicesToReserve.each { device ->
            def reserveDeviceOutput = reserveDevice(device.serial)
            logger?.info "reserving device $device"
            logger?.info "got response: " + ((String) reserveDeviceOutput?.description)
        }
    }

    /**
     * Run "adb connect" for all the devices passed as param that are currently reserved from this machine
     */
    def connectToDevices(Collection<DeviceInfo> devicesToConnect) {

        logger?.info "There are ${devicesToConnect.size} devices to connect to."

        devicesToConnect.each {
            def connectionString = it.remoteConnectUrl

            if (it.remoteConnectUrl == null) {
                it.remoteConnectUrl = getConnectionString(it.serial)
                logger?.info "got RemoteConnectUrl=${it.remoteConnectUrl}"
            }

            logger?.info "connecting ADB to ${connectionString}"
            def adbConnectOutput = "adb connect $connectionString".execute().text
            logger?.info adbConnectOutput
        }

        def adbDevicesOutput = "adb devices".execute().text
        logger?.info "these are all the devices currently accessible through ADB on this machine:\n" + adbDevicesOutput
    }

    /**
     * Start the STF identifier for all the devices passed as param
     */
    def showDevices(Collection<DeviceInfo> devicesToShow) {

        logger?.info "There are ${devicesToShow.size} devices to light up."

        withPool {
            devicesToShow.eachParallel {
                def serial = it.remoteConnectUrl

                if (it.remoteConnectUrl == null) {
                    it.remoteConnectUrl = getConnectionString(it.serial)
                    logger?.info "got RemoteConnectUrl=${it.remoteConnectUrl}"
                }

                logger?.info "starting STF identifier for ${serial}"
                def adbConnectOutput = "adb -s $serial shell am start -n jp.co.cyberagent.stf/.IdentityActivity".execute().text
                logger?.info adbConnectOutput
            }
        }
    }

    /**
     * Use the STF API to release all devices.
     * This is equivalent to clicking "Stop using" from the STF UI for each device.
     * This should automatically disconnect devices from ADB as well
     */
    def releaseDevices(Collection<DeviceInfo> devicesToRelease) {
        devicesToRelease*.serial.each {
            if (it != null) {
                logger?.info "releasing device with serial $it"
                def releaseDeviceOutput = releaseDevice(it)
                logger?.info ((String) releaseDeviceOutput?.description)
            }
        }
    }

    /**
     * Generates a collection of devices that are reserved but aren't usable in "adb devices"
     * @param myDevices devices passed in by another filter
     * @return
     */
    static Collection<DeviceInfo> diffDevices(Collection<DeviceInfo> myDevices) {

        def adbDevicesOutput = "adb devices".execute().text.readLines()

        Collection<String> connectedDevices = adbDevicesOutput.collect {
            if (it.contains("List of devices attached") || it.trim().length() == 0) {
                return null
            }
            def tokens = it.split(/\s/)
            if (tokens.length < 2 || !tokens[1].contains("device")) {
                return null
            }
            return tokens[0]
        }

        List<DeviceInfo> diffedDevices = new LinkedList(myDevices)
        diffedDevices.removeAll {
            connectedDevices?.contains(it.remoteConnectUrl)
        }

        return diffedDevices
    }
}
