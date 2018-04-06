package com.dua3.gradle.jpms.task;

import java.io.File;
import java.util.Collections;
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

        // setup module path - 3. tthe JDK modules
		String jmods = System.getProperties().getProperty("java.home")+File.separator+"jmods";

		// setup module path - putting it all together
        String modulePath = String.join(File.pathSeparator, projectModulePath, dependendyModulePath, jmods);

        // output folder
        String output = project.getBuildDir().getAbsolutePath()+File.separator+"dist";

		// prepare jlink arguments - see jlink documentation
		String launcher = String.format("%s=%s/%s", extension.getApplication(), extension.getModule(), extension.getMain());


		List<String> jlinkArgs = new LinkedList<>();
		Collections.addAll(jlinkArgs, "--module-path", modulePath);
		Collections.addAll(jlinkArgs, "--add-modules", extension.getModule());
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
