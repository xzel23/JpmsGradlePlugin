package com.dua3.gradle.jpms;

import org.gradle.api.GradleException;

public class JigsawExtension {

    /**
     * The module.
     */
    private String module = "";
    /**
     * Flag indicating whether a multi-release jar should be built.
     */
    private boolean multiRelease = false;
    /**
     * The fully qualified main class.
     */
    private String main = "";
    /**
     * List of other modules to add, separated by comma.
     */
    private String addModules = "";
    /**
     * The application name to use.
     */
    private String application = "";
    /**
     * The compression level.
     */
    private int compress = 2;
    /**
     * Debugging flag.
     */
    private boolean debug = false;
    /**
     * The name of installer.
     */
    private String bundleName = "";
    /**
     * The version.
     */
    private String version = "";
    /**
     * The jar file containing the application class.
     */
    private String mainJar = "";
    /**
     * The type of installer.
     */
    private String bundleType = "";
    /**
     * Extra arguments to pass on to the packager.
     */
    private String[] packagerArgs = {};
    /**
     * The Module containing the test library.
     */
    private String testLibraryModule = "org.junit.jupiter.api";

    /**
     * Get name of bundle.
     *
     * @return the bundle name
     */
    public String getBundleName() {
        return bundleName;
    }

    /**
     * Set bundle name.
     *
     * @param name the bundle name to set
     */
    public void setBundleName(String name) {
        this.bundleName = bundleName;
    }

    /**
     * Get version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set version.
     *
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the main jar.
     *
     * @return the main jar
     */
    public String getMainJar() {
        return mainJar;
    }

    /**
     * Set the main jar.
     *
     * @param mainJar the main jar to set
     */
    public void setMainJar(String mainJar) {
        this.mainJar = mainJar;
    }

    /**
     * Get Application class.
     *
     * @return the Application class
     */
    public String getMain() {
        return main;
    }

    /**
     * Set Application class.
     *
     * @param main the Application class to set
     */
    public void setMain(String main) {
        this.main = main;
    }

    /**
     * Get type of bundle.
     *
     * @return the bundle tpye
     */
    public String getBundleType() {
        return bundleType;
    }

    /**
     * Set bundle type.
     *
     * @param bundleType the bundle type to set
     */
    public void setBundleType(String bundleType) {
        this.bundleType = bundleType;
    }

    /**
     * Get extra arguments to pass to the packager.
     *
     * @return the extraArgs
     */
    public String[] getPackagerArgs() {
        return packagerArgs;
    }

    /**
     * Set extra arguments to pass to the packager.
     *
     * @param packagerArgs the extraArgs to set
     */
    public void setPackagerArgs(String... packagerArgs) {
        this.packagerArgs = packagerArgs;
    }

    public boolean hasModule() {
        return !module.isEmpty();
    }
    
    public String getModule() {
        if (!hasModule()) {
            throw new GradleException("jigsaw.module not set!");
        }
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public boolean isMultiRelease() {
        return multiRelease;
    }

    public void setMultiRelease(boolean multiRelease) {
        this.multiRelease = multiRelease;
    }

    public String getAddModules() {
        return addModules;
    }

    public void setAddModules(String addModules) {
        this.addModules = addModules;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public int getCompress() {
        return compress;
    }

    public void setCompress(int compress) {
        this.compress = compress;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getTestLibraryModule() {
        return testLibraryModule;
    }

    public void setTestLibraryModule(String testLibraryModule) {
        this.testLibraryModule = testLibraryModule;
    }
}
