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
    STF() {

        (STF_URL, STF_ACCESS_TOKEN) = parseProjectProperties()

        logger = Logger.getLogger("hello")

        if (VERBOSE_OUTPUT) {
            logger.setLevel(Level.INFO)
        } else {
            logger.setLevel(Level.WARNING)
        }

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
     * Check if any of the given set of `filters` prefix the provided `filterFullName`
     * @param filterFullName the full name of the filter to check against
     * @param filters the set of filters passed
     * @return
     */
    def startsWith(String filterFullName, String[] filters) {
        def matches = filters.findAll {
            filterFullName.startsWith(it)
        }

        if (matches.size() == 1) {
            return true
        } else {
            if (matches.size() > 1) {
                logger.warning("Multiple filters match $filterFullName: $filters")
            }
            return false
        }
    }

    /**
     * Transforms the array of filters into a list of maps [key,value], splitting by "="
     * XXX: perhaps in a future release this will support more conditional operators ( like != < <= > >= ) and/or ranges (like "sdk=18-24" )
     * @param filters the array of filters from command line
     * @return
     */
    static def getFilterMap(String[] filters) {
        filters.collect {
            def kv = it.split("=")
            def key = kv[0]
            def value = kv.size() > 1 ? kv[1..-1].join("=") : null
            [key: key, value: value]
        }
    }

    /**
     * Check if any of the given set of `filters` prefix the provided `filterFullName`
     * @param filterFullName the full name of the filter to check against
     * @param filters the set of filters passed
     * @return
     */
    def getFilterValue(String filterFullName, String[] filters) {

        def matches = getFilterMap(filters).findAll {
            filterFullName.startsWith(it.key)
        }

        if (matches.size() > 0) {
            if (matches.size() > 1) {
                logger.warning("Multiple filters match; returning the first match ONLY $filterFullName: $filters;")
            }
            return matches[0].value
        }
    }

    /**
     * Returns a collection of ALL devices that are usable or in use
     * @return Collection<DeviceInfo>
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
    Collection<String> queryDevices(String[] filters, boolean quiet) {

        //only unreserved devices
        boolean freeDevices = startsWith("unreserved", filters)

        //only devices in use by current user (ignores `freeDevices` filter)
        boolean deviceInUseFilter = startsWith("using", filters)

        //devices matching a particular Android SDK (int)
        def sdkFilter = getFilterValue("sdk", filters)

        //devices whose `serial` field contains the given substring
        def serialFilter = getFilterValue("serial", filters)

        //devices whose `adb connection` field contains the given substring
        def connectionFilter = getFilterValue("connect", filters)

        //devices whose `notes` field string contains the given substring
        def notesFilter = getFilterValue("notes", filters)




        def deviceList = getAllDevices().findAll {

            if (freeDevices && (it.ownerEmail != null)) {
                logger?.info("skipping device owned by `${it.ownerEmail}` because `unreserved` filter is active")
                return false
            }

            if (deviceInUseFilter && !it.using) {
                logger?.info("skipping device `${it.serial}` because it is not in use by current user and `using` filter is active")
                return false
            }

            if (sdkFilter != null && it.sdk != Integer.valueOf(sdkFilter)) {
                logger?.info("skipping device with sdk=${it.sdk} because `sdk=$sdkFilter` filter is active")
                return false
            }

            if (connectionFilter != null && !it.remoteConnectUrl?.contains(connectionFilter)) {
                logger?.info("skipping device with connectionString=${it.remoteConnectUrl} because `connect=$connectionFilter` filter is active")
                return false
            }

            if (notesFilter != null && !it.notes?.contains(notesFilter)) {
                logger?.info("skipping device with notes=${it.notes} because `notes=$notesFilter` filter is active")
                return false
            }

            if (serialFilter != null && !it.serial?.contains(serialFilter)) {
                logger?.info("skipping device with serial=${it.serial} because `serial=$serialFilter` filter is active")
                return false
            }

            true
        }

        if (quiet) {
            return deviceList*.serial
        } else {
            return deviceList
        }
    }

    /**
     * Use the STF API to reserve a list of devices
     * @param serials an array of SERIALs to reserve. If the array contains the item "all", then all the devices provided by STF will be reserved
     */
    def addDevicesWithSerials(String[] serials) {
        def availableDeviceSerials = getAvailableDeviceSerials()
        logger?.info "availableDeviceSerials are: $availableDeviceSerials; we're going to connect to $serials"

        if (serials.contains("all")) {
            availableDeviceSerials.each {
                def reserveDeviceOutput = reserveDevice(it)
                logger?.info "reserving device $it, got response: " + reserveDeviceOutput?.description
            }
        } else {
            serials.each { serial ->
                if (availableDeviceSerials.contains(serial)) {
                    def reserveDeviceOutput = reserveDevice(serial)
                    logger?.info "reserving device $serial"
                    logger?.info "got response: " + reserveDeviceOutput?.description
                }
            }
        }
    }

    /**
     * Use the STF API to get a connection string that can later be used with ADB to connect to a particular device
     * @param serial the target device SERIAL
     * @return the connect URL
     */
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
    def connectToReservedDevices() {

        def resp = stf_api.request(GET, JSON) { req ->
            uri.path = '/api/v1/user/devices'
        }

        logger?.info "There are ${resp.devices.size} devices available through STF.\nConnecting to each one of:\n" + resp.devices*.remoteConnectUrl + "\n"

        resp.devices*.serial.each {
            def connectionString = getConnectionString(it)
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
    def releaseReservedDevices() {
        def getUserDevicesResponse = stf_api.request(GET, JSON) { req ->
            uri.path = '/api/v1/user/devices'
        }
        getUserDevicesResponse.devices*.serial.each {
            if (it != null) {
                logger?.info "releasing device with serial $it"
                def releaseDeviceOutput = releaseDevice(it)
                logger?.info releaseDeviceOutput?.description
            }
        }
    }

}