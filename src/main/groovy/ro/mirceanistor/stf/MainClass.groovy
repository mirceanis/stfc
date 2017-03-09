package ro.mirceanistor.stf

import org.apache.commons.cli.*

class MainClass {

    public static boolean VERBOSE_OUTPUT = false
    public static final String C_HELP = "help"
    public static final String C_VERSION = "version"
    public static final String C_VERBOSE = "quiet"
    public static final String C_ALL = "all"
    public static final String C_LIST = "list"
    public static final String C_RELEASE = "release"
    public static final String C_CONNECT = "connect"

    static void main(String[] args) {

        try {

            CommandLineParser parser = new DefaultParser()
            Options options = new Options()
            options.addOption("h", C_HELP, false, "print this help message")
            options.addOption("V", C_VERSION, false, "show version")
            options.addOption("v", C_VERBOSE, false, "show verbose output")

            options.addOption("l", C_LIST, false, "list all available devices")
            options.addOption("a", C_ALL, false, "reserve all available devices")
            options.addOption("r", C_RELEASE, false, "release all reserved devices")
            options.addOption("c", C_CONNECT, false, "connect to all reserved devices")

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

            boolean hasAction = false;

            if (commandLine.hasOption(C_ALL)) {
                new STF().addDevicesWithSerials("all")
                hasAction = true
            }

            if (commandLine.hasOption(C_CONNECT)) {
                new STF().connectToReservedDevices()
                hasAction = true
            }

            if (commandLine.hasOption(C_RELEASE)) {
                new STF().releaseReservedDevices()
                hasAction = true
            }

            if (commandLine.hasOption(C_LIST)) {
                def deviceSerials = new STF().getAvailableDeviceSerials()
                deviceSerials.each {
                    println it
                }
                hasAction = true
            }

            if (commandLine.hasOption(C_HELP) || !hasAction) {
                CLI.printUsage(options)
            }

        } catch (Throwable ignored) {
            System.err.print("generic error on the following arguments: ")
            for (String arg : args) {
                System.err.print(arg + " ")
            }
            System.err.print("\n")
            ignored.printStackTrace()
        }

    }


}
