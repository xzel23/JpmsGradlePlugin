package com.dua3.gradle.jpms.task;

public class ModuleInfoExtension {

    /** Flag indicating whether a multi-release jar should be built. */
    private boolean multiRelease = false;

    public boolean isMultiRelease() {
        return multiRelease;
    }

    public void setMultiRelease(boolean multiRelease) {
        this.multiRelease = multiRelease;
    }

}
