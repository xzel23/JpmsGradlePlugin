package com.dua3.gradle.jpms.task;

public class JLinkExtension {

    /** The root module containing the main class. */
    private String module = "";
    /** The fully qualified main class. */
    private String main = "";
    /** List of other modules to add, separated by comma. */
    private String addModules = "";
    /** The application name to use. */
    private String application = "";
    /** The compression level. */
    private int compress = 0;
    /** Debugging flag. */
    private boolean debug = false;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
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

}
