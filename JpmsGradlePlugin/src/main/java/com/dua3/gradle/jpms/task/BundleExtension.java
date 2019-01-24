package com.dua3.gradle.jpms.task;

public class BundleExtension {

    /** The tape of installer. */
    private String name = "";
    /** The Application class. */
    private String appClass = "";
    /** The tape of installer. */
    private String type = "";
    /** Etra arguments to pass on to the packager. */
    private String[] extraArgs = {};

    /**
     * Get name of bundle.
     * 
     * @return the bundle name
     */
    public String getName() {
        return name;
    }

    /**
     * Set bundle name.
     * 
     * @param name the bundle name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get Application class.
     * 
     * @return the Application class
     */
    public String getAppClass() {
        return appClass;
    }

    /**
     * Set Application class.
     * 
     * @param appClass the Application class to set
     */
    public void setAppClass(String appClass) {
        this.appClass = appClass;
    }

    /**
     * Get type of bundle.
     * 
     * @return the bundle tpye
     */
    public String getType() {
        return type;
    }

    /**
     * Set bundle type.
     * 
     * @param type the bundle type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Set extra arguments to pass to the packager.
     * 
     * @param extraArgs the extraArgs to set
     */
    public void setExtraArgs(String... extraArgs) {
        this.extraArgs = extraArgs;
    }

    /**
     * Get extra arguments to pass to the packager.
     * 
     * @return the extraArgs
     */
    public String[] getExtraArgs() {
        return extraArgs;
    }

}
