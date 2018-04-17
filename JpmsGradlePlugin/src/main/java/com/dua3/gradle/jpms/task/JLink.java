package com.dua3.gradle.jpms.task;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskOutputs;
import org.gradle.api.tasks.bundling.Jar;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class JLink extends DefaultTask {

	@TaskAction
	public void jlink() {
		Project project = getProject();

		ToolProvider jlink = ToolProvider.findFirst("jlink")
				.orElseThrow(() -> new GradleException("could not get an instance of the jlink tool."));

		JLinkExtension extension = (JLinkExtension) project.getExtensions().getByName("jlink");

		if (extension.getApplication().isEmpty()) {
            project.getLogger().info("jlink.application not set, not executing jlink ({})", this);
	        return;
		}
		
		// setup module path - 1. collect all generated jars
		String projectModulePath = project.getTasks()
		    .withType(Jar.class)
		    .stream()
		    .filter(t
		    		-> !t.getClassifier().equalsIgnoreCase("javadoc")
		    		&& !t.getClassifier().equalsIgnoreCase("src")
		    		&& !t.getClassifier().equalsIgnoreCase("sources"))
		    .map(Task::getOutputs)
		    .map(TaskOutputs::getFiles)
		    .map(FileCollection::getAsPath)
		    .collect(Collectors.joining(File.pathSeparator));

        // setup module path - 2. collect runtime dependencies
		String dependendyModulePath = project.getConfigurations()
		    .getByName("runtime")
		    .getAsPath();

        // setup module path - 3. the JDK modules
		String jmods = System.getProperties().getProperty("java.home")+File.separator+"jmods";

		// setup module path - putting it all together
        String modulePath = String.join(File.pathSeparator, projectModulePath, dependendyModulePath, jmods);

        // output folder
        String output = project.getBuildDir().getAbsolutePath()+File.separator+"dist";

        // remove output folder if it exists
        if (extension.isAutoClean()) {
        	Path outputFolder = Paths.get(output);
        	if (Files.exists(outputFolder)) {
        		JpmsGradlePlugin.trace("removing output folder: "+outputFolder);
	        	try {
	    	    	Files.walk(outputFolder, FileVisitOption.FOLLOW_LINKS)
	    	        .sorted(Comparator.reverseOrder())
	    	        .forEach(p -> {
	    	        	try {
	    	        		Files.deleteIfExists(p);
	    	        	} catch (IOException e) {
	    	        		throw new UncheckedIOException(e);
	    	        	}
	    	        });
				} catch (IOException|UncheckedIOException e) {
					throw new GradleException("could not delete output folder", e);
				}
        	}
        }
        
		// prepare jlink arguments - see jlink documentation
		String launcher = String.format("%s=%s/%s", extension.getApplication(), extension.getModule(), extension.getMain());

		// list of modules to include
		String addModules = extension.getAddModules();
		String rootModule = extension.getModule();

		if (rootModule.isEmpty()) {
		    throw new GradleException("root module is not set (buid.gradle: jlink.module=module.containing.mainclass)");
		}

		if (addModules.isEmpty()) {
		    addModules = rootModule;
		} else {
		    addModules = rootModule + "," + addModules;
		}

		// jlink arguments
		List<String> jlinkArgs = new LinkedList<>();
		Collections.addAll(jlinkArgs, "--module-path", modulePath);
		Collections.addAll(jlinkArgs, "--add-modules", addModules);
		Collections.addAll(jlinkArgs, "--launcher", launcher);
		Collections.addAll(jlinkArgs, "--output", output);

		// compression
		Collections.addAll(jlinkArgs, "--compress", String.valueOf(extension.getCompress()));

        // debugging
		if (!extension.isDebug()) {
		    jlinkArgs.add("-G");
		}

		// other
		Collections.addAll(jlinkArgs, "--no-header-files", "--no-man-pages");

        JpmsGradlePlugin.trace("jlink arguments: %s", jlinkArgs);

        // execute jlink
        TaskHelper.runTool(jlink, project, jlinkArgs);
	}
}
