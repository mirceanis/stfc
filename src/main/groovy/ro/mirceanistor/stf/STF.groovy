package ro.mirceanistor.stf

import groovyx.net.http.HTTPBuilder

import java.util.logging.Level
import java.util.logging.Logger

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*
import static ro.mirceanistor.stf.MainClass.VERBOSE_OUTPUT

/**
 * A class representing a subset of the STF API relating to device reservation and connection.
 * This provides utility tasks for reserving/releasing devices and for connection to reserved devices.
 */
class STF {

    def stf_api

    def filters = {}

    //per user access token
    def STF_ACCESS_TOKEN = null

    //the STF api URL
    def STF_URL = null

    // the logger used by the project
    Logger logger = null

    def TOKEN_GENERATION_INSTRUCTIONS = "\nTo generate a token, go to the STF console.\n" +
            "Under Settings / Keys / Access Tokens, click the \"+\" button to generate a new token.\n" +
            "Copy that token to the GLOBAL `stf.properties` file that can be found in your \$HOME directory, under the `.stf` subfolder\n" +
            "On Windows that's \"%USERPROFILE%\\.stf\\stf.properties\"\n" +
            "On Linux, that's \"~/.stf/stf.properties\" "

    /**
     * STF class constructor.
     * This will be used in gradle tasks
     * @param project the Project that this class is being used from
     */
    STF(Collection<String> rawFilters) {

        (STF_URL, STF_ACCESS_TOKEN) = parseProjectProperties()

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
                    "\nThis is most likely caused by a STF_ACCESS_TOKEN mismatch.\n" + TOKEN_GENERATION_INSTRUCTIONS)
        }


    }

    /**
     * Performs some checks related to STF_URL and STF_ACCESS_TOKEN.
     * throws RuntimeException with details where it makes sense
     */
    private List parseProjectProperties() {
        Properties props = PropertyLoader.loadProperties()

        if (props.getProperty("STF_ACCESS_TOKEN") != null) {
            STF_ACCESS_TOKEN = props.getProperty('STF_ACCESS_TOKEN')
        } else if ("$System.env.STF_ACCESS_TOKEN".toString() != "null") {
            STF_ACCESS_TOKEN = "$System.env.STF_ACCESS_TOKEN".toString()
        } else {
            throw new RuntimeException("missing STF_ACCESS_TOKEN.\n" +
                    "Did you forget to define STF_ACCESS_TOKEN in your `stf.properties` file?\n"
                    + TOKEN_GENERATION_INSTRUCTIONS)
        }

        if (props.getProperty("STF_URL") != null) {
            STF_URL = props.getProperty('STF_URL')
        } else if ("$System.env.STF_URL".toString() != "null") {
            STF_URL = "$System.env.STF_URL".toString()
        } else {
            throw new RuntimeException("missing STF_URL.\n" +
                    "Did you forget to define STF_URL in your in your `stf.properties` file?")
        }

        return [STF_URL, STF_ACCESS_TOKEN]
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
                throw new RuntimeException("Something went wrong with the request, we got: ${resp.statusLine}\n" +
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
     * Transforms the array of filters into a list of maps [key,value], splitting by "="
     * XXX: perhaps in a future release this will support more conditional operators ( like != < <= > >= ) and/or ranges (like "sdk=18-24" )
     * @param filters the array of filters from command line
     * @return
     */
    static def parseFilters(def filters) {
        def pattern = /^(?<key>[a-zA-Z]+)(?<operator>=)*(?<value>.*)/

        filters.findResults {
            def matcher = (it =~ pattern)
            if (matcher.matches()) {
                def key = matcher.group("key")
                def operator = matcher.group("operator")
                def value = matcher.group("value")

                if (operator == null) {
                    value = true
                    operator = "="
                }

                [key: key, value: value, operator: operator]

            } else {
                logger?.warning("Can't parse filter: \"${it}\"")
                null
            }
        }
    }

    /**
     * Check if any of the given set of `filters` prefix the provided `filterFullName` and return the filter
     * @param filterFullName the full name of the filter to check against
     * @param filters the set of filters passed
     * @return the FIRST parsed filter that matches the check or `null` if there is none
     */
    def getFilter(String filterFullName) {

        def matches = filters.findAll {
            filterFullName.startsWith(it.key)
        }

        if (matches.size() > 0) {
            if (matches.size() > 1) {
                logger.warning("Multiple filters match; returning the first match ONLY $filterFullName: $filters;")
            }
            return matches[0]
        } else {
            return null
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
            new DeviceInfo(it.serial, it.display.width, it.display.height, Integer.valueOf(it.sdk), it.name, it.model, it.remoteConnectUrl, it.notes, it.using, it.owner?.email)
        }

    }

    /**
     * Filter through devices provided by STF by [sdk, connectionString, notes and reservation status]
     * @param filters set of filters usually provided by command line
     * @param quiet whether or not to output only device serials
     * @return
     */
    Collection<DeviceInfo> queryDevices() {

        //only unreserved devices
        def filterByAvailability = getFilter("free")

        //only devices in use by current user (ignores `freeDevices` filter)
        def filterByCurrentUser = getFilter("using")

        //devices matching a particular Android SDK (int)
        def sdkFilter = getFilter("sdk")

        //devices whose `serial` field contains the given substring
        def filterBySerial = getFilter("serial")?.value

        //devices whose `adb connection` field contains the given substring
        def filterByConnection = getFilter("connect")?.value

        //devices whose `notes` field string contains the given substring
        def filterByNotes = getFilter("notes")?.value

        getAllDevices().findAll {

            if (filterByAvailability) {
                boolean isFree = (it.ownerEmail == null)
                if (filterByAvailability.value.toBoolean() == !isFree) {
                    logger?.info("skipping device owned by `${it.ownerEmail}` because `free` filter is ${filterByAvailability.value}")
                    return false
                }
            }

            if (filterByCurrentUser) {
                if (filterByCurrentUser.value.toBoolean() != it.using) {
                    logger?.info("skipping device `${it.serial}` because in_use_by_current_user=${it.using} and `using` filter is set to `${filterByCurrentUser.value}`")
                    return false
                }
            }

            if (sdkFilter) {
                def deviceSDK = (int) it.sdk;
                def matchesSDK

                switch (sdkFilter.value) {
                //for range filters of the form `sdk=18-23`
                    case ~/^[0-9]+-[0-9]+$/:
                        def range = sdkFilter.value.split("-").collect { it as int }
                        matchesSDK = (deviceSDK in range[0]..range[1])
                        break

                //for range filters of the form `sdk=18+`
                    case ~/^[0-9]+\+$/:
                        matchesSDK = (deviceSDK >= (sdkFilter.value[0..-2] as int))
                        break

                //simple case `sdk=23`
                    default:
                        matchesSDK = (deviceSDK == (sdkFilter.value as int))
                        break
                }

                if (matchesSDK) {
                    return true
                } else {
                    logger?.info("skipping device with sdk=${it.sdk} because `sdk=$sdkFilter` filter is active")
                    return false
                }
            }


            if (filterByConnection && !it.remoteConnectUrl?.contains(filterByConnection)) {
                logger?.info("skipping device with connectionString=${it.remoteConnectUrl} because `connect=$filterByConnection` filter is active")
                return false
            }

            if (filterByNotes && !it.notes?.contains(filterByNotes)) {
                logger?.info("skipping device with notes=${it.notes} because `notes=$filterByNotes` filter is active")
                return false
            }

            if (filterBySerial && !it.serial?.contains(filterBySerial)) {
                logger?.info("skipping device with serial=${it.serial} because `serial=$filterBySerial` filter is active")
                return false
            }

            true
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
            logger?.info "got response: " + reserveDeviceOutput?.description
        }
    }

/**
 * Use the STF API to get a connection string that can later be used with ADB to connect to a particular device
 * @param serial the target device SERIAL
 * @return the connect URL
 */
    @SuppressWarnings("GroovyUnusedDeclaration")
    def getConnectionString(String serial) {
        def resp = stf_api.request(POST, JSON) { req ->
            body = []
            uri.path = "/api/v1/user/devices/${serial}/remoteConnect".toString()
        }
        return resp.remoteConnectUrl
    }

/**
 * Run "adb connect" for all the devices that are currently reserved from this machine
 */
    def connectToDevices(Collection<DeviceInfo> devicesToConnect) {

        logger?.info "There are ${devicesToConnect.size} devices available through STF.\nConnecting to each one of:\n" + devicesToConnect*.remoteConnectUrl + "\n"

        devicesToConnect.each {
            def connectionString = it.remoteConnectUrl
            logger?.info "connecting ADB to ${connectionString}"
            def adbConnectOutput = "adb connect $connectionString".execute().text
            logger?.info adbConnectOutput
        }

        def adbDevicesOutput = "adb devices".execute().text
        logger?.info "these are all the devices currently accessible through ADB on this machine:\n" + adbDevicesOutput
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
                logger?.info releaseDeviceOutput?.description
            }
        }
    }

}