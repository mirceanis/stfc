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

        formatter.printHelp("java -jar stf.jar [options]", options)

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