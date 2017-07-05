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

/**
 * A class that handles loading and parsing the properties used to connect to an STF server
 */
class PropertyLoader {

    static final String PROPERTIES_FILE = File.separator + "stf.properties"
    static final String PROPERTIES_STF_FILE = File.separator + ".stf" + PROPERTIES_FILE


    protected static def TOKEN_GENERATION_INSTRUCTIONS = "\nTo generate a token, go to the STF console.\n" +
            "Under Settings / Keys / Access Tokens, click the \"+\" button to generate a new token.\n" +
            "Copy that token to the GLOBAL `stf.properties` file that can be found in your \$HOME directory, under the `.stf` subfolder\n" +
            "On Windows that's \"%USERPROFILE%\\.stf\\stf.properties\"\n" +
            "On Linux, that's \"~/.stf/stf.properties\" "

    /**
     * builds a list of possible locations for the STF properties
     * @return list of String paths
     */
    static def STF_PROPERTIES_LOCATIONS = [
                System.getProperty("user.home") + "/.gradle/gradle.properties",
                new File(getPathToJar()).parent + PROPERTIES_STF_FILE,
                new File(getPathToJar()).parent + PROPERTIES_FILE,
                System.getProperty("user.home") + PROPERTIES_STF_FILE,
                System.getProperty("user.dir") + PROPERTIES_STF_FILE,
                System.getProperty("user.dir") + PROPERTIES_FILE,
        ]

    static Properties mProps = null

    /**
     * Builds the path to the *.jar file that is currently running
     * @return
     */
    private static String getPathToJar() {

        Class<?> clz = MainClass.class

        String resource = "/" + clz.getName().replace(".", "/") + ".class"

        String fullPath = clz.getResource(resource).toString()

        String archivePath = fullPath.substring(0, fullPath.length() - resource.length())

        if (archivePath.startsWith("jar:")) {
            archivePath = archivePath.substring("jar:".length())
        }

        if (archivePath.endsWith("!")) {
            archivePath = archivePath.substring(0, archivePath.length() - 1)
        }

        String pathToThisJar = new URL(archivePath).file
        return pathToThisJar
    }

    static Properties loadProperties() {
        Properties properties = mProps?:new Properties()
        STF_PROPERTIES_LOCATIONS.each {
            if (new File(it).exists()) {
                properties.load(new FileInputStream(it))
            }
        }

        return properties
    }

    /**
     * Performs some checks related to STF_URL and STF_ACCESS_TOKEN.
     * throws RuntimeException with details where it makes sense if the user still needs to make some settings
     */
    static List getStfCoordinates() {
        Properties props = loadProperties()

        def stf_access_token
        if (props.getProperty("STF_ACCESS_TOKEN") != null) {
            stf_access_token = props.getProperty('STF_ACCESS_TOKEN')
        } else if ("$System.env.STF_ACCESS_TOKEN".toString() != "null") {
            stf_access_token = "$System.env.STF_ACCESS_TOKEN".toString()
        } else {
            throw new RuntimeException("missing STF_ACCESS_TOKEN.\n" +
                    "Did you forget to define STF_ACCESS_TOKEN in your `stf.properties` file?\n"
                    + TOKEN_GENERATION_INSTRUCTIONS)
        }

        def stf_url
        if (props.getProperty("STF_URL") != null) {
            stf_url = props.getProperty('STF_URL')
        } else if ("$System.env.STF_URL".toString() != "null") {
            stf_url = "$System.env.STF_URL".toString()
        } else {
            throw new RuntimeException("missing STF_URL.\n" +
                    "Did you forget to define STF_URL in your in your `stf.properties` file?")
        }

        return [stf_url, stf_access_token]
    }

    //for testing purposes
    static void setProps(Properties props) {
        mProps = props
    }
}