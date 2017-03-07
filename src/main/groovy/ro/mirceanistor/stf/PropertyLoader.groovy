package ro.mirceanistor.stf

class PropertyLoader {

    static final String PROPERTIES_FILE = File.separator + "stf.properties"
    static final String PROPERTIES_STF_FILE = File.separator + ".stf" + PROPERTIES_FILE

    /**
     * builds a list of possible locations for the STF properties
     * @return list of String paths
     */
    static def getStfPropertiesPaths() {
        def result = [
                new File(getPathToJar()).parent + PROPERTIES_STF_FILE,
                new File(getPathToJar()).parent + PROPERTIES_FILE,
                System.getProperty("user.home") + PROPERTIES_STF_FILE,
                System.getProperty("user.dir") + PROPERTIES_STF_FILE,
                System.getProperty("user.dir") + PROPERTIES_FILE,
        ]
        result
    }

    private static String getPathToJar() {

        Class<?> clz = MainClass.class

        String resource = "/" + clz.getName().replace(".", "/") + ".class"

        String fullPath = clz.getResource(resource).toString()

        String archivePath = fullPath.substring(0, fullPath.length() - resource.length())
        if (archivePath.endsWith("\\WEB-INF\\classes") || archivePath.endsWith("/WEB-INF/classes")) {
            archivePath = archivePath.substring(0, archivePath.length() - "/WEB-INF/classes".length())
            // Required for wars
        }

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
        Properties properties = new Properties()
        def paths = getStfPropertiesPaths()
        paths.each {
            if (new File(it).exists()) {
                properties.load(new FileInputStream(it))
            }
        }

        return properties
    }
}