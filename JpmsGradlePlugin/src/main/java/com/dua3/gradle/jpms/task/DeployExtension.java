package com.dua3.gradle.jpms.task;

public class DeployExtension {

    /** The fully qualified main class. */
    private String installerName = "installer";

    /** The path to the jpackager executable. */
    private String jpackager = "jpackager";

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
     * Get path to jpackager.
     * 
     * @return the jpackager
     */
    public String getJpackager() {
        return jpackager;
    }

    /**
     * Set path to jpackager.
     * 
     * @param jpackager the jpackager to set
     */
    public void setJpackager(String jpackager) {
        this.jpackager = jpackager;
    }
}
