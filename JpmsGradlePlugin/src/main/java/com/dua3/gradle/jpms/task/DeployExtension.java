package com.dua3.gradle.jpms.task;

import org.gradle.internal.impldep.org.bouncycastle.util.Arrays;

public class DeployExtension {

    /** The fully qualified main class. */
    private String main = "";
    /** The tape of installer. */
    private String type = "";
    /** The fully qualified main class. */
    private String appName = "";
    /** Etra arguments to pass on to the packager. */
    private String[] extraArgs = {};

    /**
     * Get type of installer.
     * 
     * @return the installer tpye
     */
    public String getType() {
        return type;
    }

    /**
     * Set installer type.
     * 
     * @param type the installer to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get installer name.
     * 
     * @return the appName
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Set installer name.
     * 
     * @param appName the appName to set
     */
    public void setAppName(String appName) {
        this.appName = appName;
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

    public void setMain(String main) {
        this.main = main;
    }

    /**
     * @return the main
     */
    public String getMain() {
        return main;
    }
}
