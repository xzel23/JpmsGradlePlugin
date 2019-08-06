package com.dua3.gradle.jpms.task;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.dua3.gradle.jpms.JigsawExtension;
import com.dua3.gradle.jpms.JpmsGradlePlugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
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

		JigsawExtension jigsaw = (JigsawExtension) project.getExtensions().getByName("jigsaw");

        String modulePath = TaskHelper.getModulePath(project);

		// output folder
		String output = TaskHelper.getOutputFolder(project, FOLDER_NAME);

		// remove output folder if it exists
		TaskHelper.removeFolder(output, true);

		// get settings from extension
		String application=TaskHelper.getFirst(jigsaw.getApplication(), jigsaw.getApplication(), project.getName());
		String module=TaskHelper.getFirst(jigsaw.getModule(), jigsaw.getModule());
		String main = TaskHelper.getFirst(jigsaw.getMain(), jigsaw.getMain());

		if (module.isEmpty()) {
			throw new GradleException("Main module not set. Set jlink.mainModule or bundle.mainModule.");
		}

		if (main.isEmpty()) {
			throw new GradleException("Main not set. Set jlink.main or bundle.main.");
		}

		// prepare jlink arguments - see jlink documentation
		String launcher = String.format("%s=%s/%s", application, module, main);

		// list of modules to include
		String addModules = TaskHelper.getModules(module, jigsaw.getAddModules());

		// jlink arguments
		List<String> jlinkArgs = new LinkedList<>();
		Collections.addAll(jlinkArgs, "--module-path", modulePath);
		Collections.addAll(jlinkArgs, "--add-modules", addModules);
		Collections.addAll(jlinkArgs, "--launcher", launcher);
		Collections.addAll(jlinkArgs, "--output", output);

		// compression
		Collections.addAll(jlinkArgs, "--compress", String.valueOf(jigsaw.getCompress()));

		// debugging
		if (!jigsaw.isDebug()) {
			jlinkArgs.add("-G");
		}

		// other
		Collections.addAll(jlinkArgs, "--no-header-files", "--no-man-pages");

		JpmsGradlePlugin.trace("jlink arguments: %s", jlinkArgs);

		// execute jlink
		TaskHelper.runTool(TaskHelper.JLINK, project, jlinkArgs);
	}
}
