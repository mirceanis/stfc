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
    public static final String C_SHOW = "show"
    public static final String C_DIFF = "diff"

    static void main(String[] args) {

        try {

            CommandLineParser parser = new DefaultParser()
            Options options = new Options()
            options.addOption("h", C_HELP, false, "print this help message")
            options.addOption("V", C_VERSION, false, "show version")
            options.addOption("v", C_VERBOSE, false, "show verbose output")
            options.addOption("q", C_QUIET, false, "only output device serials, nothing else. Negates --verbose option")

            options.addOption("l", C_LIST, false, "list available devices.\nCan be combined with --filter option to only show subsets")
            options.addOption("d", C_DIFF, false, "list allocated devices that don't appear usable in adb-devices\n Can be combined with --filter to be more specific.")
            options.addOption("a", C_ALLOCATE, false, "allocate devices to the current user\nBy default, this reserves every available device.\nIt's best to combine with --filter option to be more specific")
            options.addOption("r", C_RELEASE, false, "release allocated devices\nBy default it releases all but can be combined with --filter to be more specific")
            options.addOption("c", C_CONNECT, false, "connect to reserved devices\nBy default it connects to every allocated device.\n Can be combined with --filter to be more specific.")
            options.addOption("s", C_SHOW, false, "light up the screens of connected devices. Equivalent to 'find device' from the web console\n Can be combined with --filter to be more specific.")

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
                STF stf = new STF(rawFilters + Filters.F_FREE)
                def deviceSerials = stf.queryDevices()
                stf.reserveDevicesWithSerials(deviceSerials)
                hasAction = true
            }

            if (commandLine.hasOption(C_CONNECT)) {
                STF stf = new STF(rawFilters + Filters.F_USING)
                def deviceSerials = stf.queryDevices()
                stf.connectToDevices(deviceSerials)
                hasAction = true
            }

            if (commandLine.hasOption(C_RELEASE)) {
                STF stf = new STF(rawFilters + Filters.F_USING)
                def deviceSerials = stf.queryDevices()
                stf.releaseDevices(deviceSerials)
                hasAction = true
            }

            if (commandLine.hasOption(C_LIST)) {

                //by default, show all available devices
                def filters = (rawFilters.size() == 0 ? [Filters.F_FREE] : rawFilters)
                STF stf = new STF(filters)
                def devices = stf.queryDevices()
                devices.each {
                    println (QUIET_OUTPUT ? it.serial : it)
                }
                hasAction = true
            }

            if (commandLine.hasOption(C_DIFF)) {

                def stf = new STF(rawFilters + Filters.F_USING)
                def devices = stf.queryDevices()
                def diffed = STF.diffDevices(devices)
                diffed.each {
                    println (QUIET_OUTPUT ? it.serial : it)
                }
                hasAction = true
            }

            if (commandLine.hasOption(C_SHOW)) {
                STF stf = new STF(rawFilters + Filters.F_USING)
                def deviceSerials = stf.queryDevices()
                stf.showDevices(deviceSerials)
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
