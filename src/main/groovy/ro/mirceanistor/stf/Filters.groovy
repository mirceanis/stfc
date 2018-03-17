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

class Filters {

    /**
     * Filter names
     */
    public static final String F_FREE = "free"
    public static final String F_USING = "using"
    public static final String F_SDK = "sdk"
    public static final String F_SERIAL = "serial"
    public static final String F_CONNECT = "connect"
    public static final String F_NOTES = "notes"

    public static String FILTERS_DESCRIPTION = "\n\n" +
            "* `$F_FREE` - devices that are not allocated to any user\n" +
            "* `$F_USING` - devices that are in use by current user\n" +
            "* `$F_SDK` - devices with a specific sdk. This filter can also be ranged,\nEx: `sdk=18-23` OR `sdk=24+`\n" +
            "* `$F_SERIAL` - devices whose serial contains the specified string\n" +
            "* `$F_CONNECT` - devices whose connection string contains the specified string\n" +
            "* `$F_NOTES` - devices whose notes contain the specified string\n" +
            "\n"

    /**
     * Transforms the array of filters into a list of dictionaries that have [key, value]
     * @param filters the array of raw filters to be parsed. Usually from command line
     * @return a list of dictionaries with [key, value] corresponding to each filter
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
                }

                [key: key, value: value]

            } else {
                logger?.warning("Can't parse filter: \"${it}\"")
                null
            }
        }
    }
}
