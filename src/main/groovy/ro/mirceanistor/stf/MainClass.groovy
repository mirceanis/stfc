package ro.mirceanistor.stf

import org.apache.commons.cli.*

class MainClass {

    public static boolean VERBOSE_OUTPUT = false
    public static boolean QUIET_OUTPUT = false

    public static final String C_HELP = "help"
    public static final String C_VERSION = "version"
    public static final String C_VERBOSE = "verbose"
    public static final String C_ALLOCATE = "allocate"
    public static final String C_RELEASE = "release"
    public static final String C_LIST = "list"
    public static final String C_CONNECT = "connect"
    public static final String C_FILTER = "filter"
    public static final String C_QUIET = "quiet"

    static void main(String[] args) {

        try {

            CommandLineParser parser = new DefaultParser()
            Options options = new Options()
            options.addOption("h", C_HELP, false, "print this help message")
            options.addOption("V", C_VERSION, false, "show version")
            options.addOption("v", C_VERBOSE, false, "show verbose output")
            options.addOption("q", C_QUIET, false, "only output device serials, nothing else. Negates --verbose option")

            options.addOption("l", C_LIST, false, "list available devices.\nCan be combined with --filter option to only show subsets")
            options.addOption("a", C_ALLOCATE, false, "allocate devices to the current user\nBy default, this reserves every available device.\nIt's best to combine with --filter option to be more specific")
            options.addOption("r", C_RELEASE, false, "release allocated devices\nBy default it releases all but can be combined with --filter to be more specific")
            options.addOption("c", C_CONNECT, false, "connect to reserved devices\nBy default it connects to every allocated device.\n Can be combined with --filter to be more specific.")

            Option filterOption = new Option("f", C_FILTER, true, "filter devices, can be used multiple times to filter by multiple fields.\n" +
                    "If the same field is specified in more than one filter, ONLY the FIRST one is used.\n" +
                    "Available filters are:${Filters.FILTERS_DESCRIPTION}" +
                    "\n Boolean filters can be specified without the actual value, in which case they default to `true`.\n" +
                    "Ex: `stfc -f using` is equivalent to `stfc -f using=true`\n"
            )
            filterOption.setArgs(Option.UNLIMITED_VALUES)

            options.addOption(filterOption)

            CommandLine commandLine
            try {
                commandLine = parser.parse(options, args)
            } catch (ParseException e) {
                e.printStackTrace()
                CLI.printUsage(options)
                return
            }

            if (commandLine.hasOption(C_VERSION)) {
                CLI.printVersion()
                return
            }

            if (commandLine.hasOption(C_VERBOSE)) {
                VERBOSE_OUTPUT = true
            }

            if (commandLine.hasOption(C_QUIET)) {
                VERBOSE_OUTPUT = false
                QUIET_OUTPUT = true
            }

            boolean hasAction = false
            def rawFilters = [] as List

            if (commandLine.hasOption(C_FILTER)) {
                rawFilters = commandLine.getOptionValues(C_FILTER) as List
            }

            if (commandLine.hasOption(C_ALLOCATE)) {
                STF stf = new STF(rawFilters + "free")
                def deviceSerials = stf.queryDevices()
                stf.reserveDevicesWithSerials(deviceSerials)
                hasAction = true
            }

            if (commandLine.hasOption(C_CONNECT)) {
                STF stf = new STF(rawFilters + "using")
                def deviceSerials = stf.queryDevices()
                stf.connectToDevices(deviceSerials)
                hasAction = true
            }

            if (commandLine.hasOption(C_RELEASE)) {
                STF stf = new STF(rawFilters + "using")
                def deviceSerials = stf.queryDevices()
                stf.releaseDevices(deviceSerials)
                hasAction = true
            }

            if (commandLine.hasOption(C_LIST)) {

                //by default, show all available devices
                def filters = (rawFilters.size() == 0 ? [ "free" ] : rawFilters)

                def devices = new STF(filters).queryDevices()
                devices.each {
                    if (QUIET_OUTPUT) {
                        println it.serial
                    } else {
                        println it
                    }
                }
                hasAction = true
            }

            if (commandLine.hasOption(C_HELP) || !hasAction) {
                CLI.printUsage(options)
            }

        } catch (Throwable ex) {
            System.err.print("generic error on the following arguments: ")
            System.err.print args?.join(" ")
            System.err.print("\n")
            ex.printStackTrace()
        }

    }


}
