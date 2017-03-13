package ro.mirceanistor.stf

import org.apache.commons.cli.*

class MainClass {

    public static boolean VERBOSE_OUTPUT = false
    public static boolean QUIET_OUTPUT = false

    public static final String C_HELP = "help"
    public static final String C_VERSION = "version"
    public static final String C_VERBOSE = "verbose"
    public static final String C_ALL = "all"
    public static final String C_LIST = "list"
    public static final String C_RELEASE = "release"
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

            options.addOption("l", C_LIST, false, "list all available devices")
            options.addOption("a", C_ALL, false, "reserve all available devices")
            options.addOption("r", C_RELEASE, false, "release all reserved devices")
            options.addOption("c", C_CONNECT, false, "connect to all reserved devices")

            Option filterOption = new Option("f", C_FILTER, true, "filter devices, can be used multiple times")
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

            if (commandLine.hasOption(C_ALL)) {
                new STF().reserveDevicesWithSerials("all")
                hasAction = true
            }

            if (commandLine.hasOption(C_CONNECT)) {
                new STF().connectToAllReservedDevices()
                hasAction = true
            }

            if (commandLine.hasOption(C_RELEASE)) {
                new STF().releaseAllReservedDevices()
                hasAction = true
            }

            if (commandLine.hasOption(C_LIST)) {

                //by default, show all available devices
                String[] filters = ["free"]
                if (commandLine.hasOption(C_FILTER)) {
                    filters = commandLine.getOptionValues(C_FILTER)
                }

                def deviceSerials = new STF().queryDevices(filters, QUIET_OUTPUT)
                deviceSerials.each {
                    println it
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
