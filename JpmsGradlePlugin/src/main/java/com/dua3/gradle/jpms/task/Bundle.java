package com.dua3.gradle.jpms.task;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.dua3.gradle.jpms.JpmsGradlePlugin;
import com.dua3.gradle.jpms.task.TaskHelper.ToolRunner;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public class Bundle extends DefaultTask {

	public static String FOLDER_NAME_BUNDLE = "bundle";

	@TaskAction
	public void bundle() {
		Project project = getProject();

		BundleExtension extension = (BundleExtension) project.getExtensions().getByName("bundle");

        // define folders
		String input = TaskHelper.getOutputFolder(project, "libs");
        String output = TaskHelper.getOutputFolder(project, FOLDER_NAME_BUNDLE);

		// remove output folder if it exists
		TaskHelper.removeFolder(output);
        
		// jpackager arguments
        String modulePath = TaskHelper.getModulePath(project);
		String addModules = TaskHelper.getModules(extension.getMainModule(), extension.getAddModules());
		String jpackager = "jpackager";
		String name = TaskHelper.orDefault(extension.getApplication(), project.getName());
		String mainJar = project.getName()+".jar";
		String mainClass = extension.getMain();
		String[] extraArgs = extension.getExtraArgs();

		List<String> args = new LinkedList<>();
		Collections.addAll(args, "create-image");
		Collections.addAll(args, "--verbose");
		Collections.addAll(args, "--echo-mode");
		Collections.addAll(args, "--input", input);
		Collections.addAll(args, "--output", output);
		Collections.addAll(args, "--module-path", modulePath);
		Collections.addAll(args, "--add-modules", addModules);
		Collections.addAll(args, "--name", name);

		addIfPresent(args, "--main-jar", mainJar);
		addIfPresent(args, "--class", mainClass);		
		
		Collections.addAll(args, extraArgs);

        JpmsGradlePlugin.trace("jpackager arguments: %s", args);

		// execute jpackager
		ToolRunner tool = TaskHelper.toolRunner("jpackager", jpackager);
        TaskHelper.runTool(tool, project, args);
	}
    public void addIfPresent(List<String> args, String option, String arg) {
		if (!arg.isEmpty()) {
			args.add(option);
			args.add(arg);
		}
	}
}
