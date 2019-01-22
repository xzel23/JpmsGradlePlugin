package com.dua3.gradle.jpms.task;

import org.gradle.internal.impldep.org.bouncycastle.util.Arrays;

public class DeployExtension {

    /** The fully qualified main class. */
    private String installerName = "installer";

    private String[] extraArgs = {};

    /**
     * Get installer name.
     * 
     * @return the installerName
     */
    public String getInstallerName() {
        return installerName;
    }

    /**
     * Set installer name.
     * 
     * @param installerName the installerName to set
     */
    public void setInstallerName(String installerName) {
        this.installerName = installerName;
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
