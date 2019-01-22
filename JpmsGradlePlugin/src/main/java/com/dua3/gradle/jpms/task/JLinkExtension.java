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
    private int compress = 2;
    /** Debugging flag. */
    private boolean debug = false;
    
    public String getAddModules() {
        return addModules;
    }

    public String getApplication() {
        return application;
    }

    public int getCompress() {
        return compress;
    }

    public String getMain() {
        return main;
    }

    public String getModule() {
        return module;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setAddModules(String addModules) {
        this.addModules = addModules;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setCompress(int compress) {
        this.compress = compress;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setMain(String main) {
        this.main = main;
    }
    
    public void setModule(String module) {
        this.module = module;
    }
}
