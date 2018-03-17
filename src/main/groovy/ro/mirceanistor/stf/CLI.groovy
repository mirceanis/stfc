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

import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options

import java.util.jar.Manifest

class CLI {

    /**
     * Prints the version of this tool
     */
    static void printVersion() {
        Manifest manifest = getJarManifest(MainClass.class)
        System.out.println(manifest.getMainAttributes().getValue("Implementation-Title"))
        System.out.println("Version: " + manifest.getMainAttributes().getValue("Implementation-Version"))
    }

    /**
     * Prints usage options
     *
     * @param options the options object used by the command line parser
     */
    static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter()

        formatter.printHelp(120, "java -jar stf.jar [options]", null, options, null, false);

    }

    /**
     * obtains the Manifest represented by the MANIFEST.MF file in this jar
     *
     * @param clz a class contained in this jar
     * @return the Manifest
     */
    private static Manifest getJarManifest(Class<?> clz) {

        String resource = "/" + clz.getName().replace(".", "/") + ".class"

        String fullPath = clz.getResource(resource).toString()

        String archivePath = fullPath.substring(0, fullPath.length() - resource.length())

        try {
            InputStream input = new URL(archivePath + "/META-INF/MANIFEST.MF").openStream()
            return new Manifest(input)
        } catch (Exception e) {
            throw new RuntimeException("Loading MANIFEST for class " + clz + " failed!", e)
        }

    }
}
