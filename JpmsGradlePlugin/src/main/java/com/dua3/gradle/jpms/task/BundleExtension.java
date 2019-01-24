package com.dua3.gradle.jpms.task;

public class BundleExtension extends JLinkExtension {

    /** The tape of installer. */
    private String type = "";
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
