package com.dua3.gradle.jpms.task;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.dua3.gradle.jpms.JpmsGradlePlugin;
import com.dua3.gradle.jpms.task.TaskHelper.ToolRunner;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public class Deploy extends DefaultTask {

	public static String FOLDER_NAME_BUNDLE = "bundle";

	@TaskAction
	public void bundle() {
		Project project = getProject();

		DeployExtension extension = (DeployExtension) project.getExtensions().getByName("deploy");

        // define folders
		String input = TaskHelper.getOutputFolder(project, JLink.FOLDER_NAME);
        String output = TaskHelper.getOutputFolder(project, FOLDER_NAME_BUNDLE);

		// remove output folder if it exists
		TaskHelper.removeFolder(output);
        
		// jpackager arguments
		String jpackager = "jpackager";
		String name = TaskHelper.orDefault(extension.getAppName(), project.getName());
		String mainClass = extension.getMain();
		String[] extraArgs = extension.getExtraArgs();

		List<String> args = new LinkedList<>();
		Collections.addAll(args, "create-image");
		Collections.addAll(args, "--runtime-image", input);
		Collections.addAll(args, "--output", output);
		Collections.addAll(args, "--name", name);
		Collections.addAll(args, "--class", mainClass);
		Collections.addAll(args, extraArgs);

        JpmsGradlePlugin.trace("jpackager arguments: %s", args);

		// execute jpackager
		ToolRunner tool = TaskHelper.toolRunner("jpackager", jpackager);
        TaskHelper.runTool(tool, project, args);
	}

}
