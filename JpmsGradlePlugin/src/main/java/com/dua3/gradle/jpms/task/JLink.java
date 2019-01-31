package com.dua3.gradle.jpms.task;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public class JLink extends DefaultTask {

	public static String FOLDER_NAME = "jlink";

	@TaskAction
	public void jlink() {
		Project project = getProject();

		JavaVersion javaVersion = JavaVersion.current();
		if (!javaVersion.isJava9Compatible()) {
			JpmsGradlePlugin.trace("Java version 9 or above required, current version is: "+javaVersion);
		}

		JLinkExtension jlinkExtension = (JLinkExtension) project.getExtensions().getByName("jlink");
		BundleExtension bundleExtension = (BundleExtension) project.getExtensions().getByName("bundle");

		if (jlinkExtension.getApplication().isEmpty()) {
            project.getLogger().info("jlink.application not set, not executing jlink ({})", this);
	        return;
		}
		
        String modulePath = TaskHelper.getModulePath(project);

		// output folder
		String output = TaskHelper.getOutputFolder(project, FOLDER_NAME);

		// remove output folder if it exists
		TaskHelper.removeFolder(output);

		// get settings from extension
		String application=TaskHelper.orDefault(jlinkExtension.getApplication(), project.getName());
		String module=jlinkExtension.getMainModule();
		String main = TaskHelper.orDefault(jlinkExtension.getMain(), bundleExtension.getAppClass());

		// prepare jlink arguments - see jlink documentation
		String launcher = String.format("%s=%s/%s", application, module, main);

		// list of modules to include
		String addModules = TaskHelper.getModules(jlinkExtension.getMainModule(), jlinkExtension.getAddModules());

		// jlink arguments
		List<String> jlinkArgs = new LinkedList<>();
		Collections.addAll(jlinkArgs, "--module-path", modulePath);
		Collections.addAll(jlinkArgs, "--add-modules", addModules);
		Collections.addAll(jlinkArgs, "--launcher", launcher);
		Collections.addAll(jlinkArgs, "--output", output);

		// compression
		Collections.addAll(jlinkArgs, "--compress", String.valueOf(jlinkExtension.getCompress()));

		// debugging
		if (!jlinkExtension.isDebug()) {
			jlinkArgs.add("-G");
		}

		// other
		Collections.addAll(jlinkArgs, "--no-header-files", "--no-man-pages");

		JpmsGradlePlugin.trace("jlink arguments: %s", jlinkArgs);

		// execute jlink
		TaskHelper.runTool(TaskHelper.JLINK, project, jlinkArgs);
	}
}
