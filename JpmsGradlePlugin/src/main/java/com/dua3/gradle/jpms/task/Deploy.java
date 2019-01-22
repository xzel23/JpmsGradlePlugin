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
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskOutputs;
import org.gradle.api.tasks.bundling.Jar;

import com.dua3.gradle.jpms.JpmsGradlePlugin;
import com.dua3.gradle.jpms.task.TaskHelper.ToolRunner;

public class Deploy extends DefaultTask {

	public static String FOLDER_NAME = "deploy";

	@TaskAction
	public void deploy() {
		Project project = getProject();

		DeployExtension extension = (DeployExtension) project.getExtensions().getByName("deploy");

        // output folder
        String output = TaskHelper.getOutputFolder(project, "deploy");

		// remove output folder if it exists
		TaskHelper.removeFolder(output);
        
		// jpackager arguments
		String jpackager = extension.getJPackager();
		String name = extension.getInstallerName();
		String appimage = TaskHelper.getOutputFolder(project, JLink.FOLDER_NAME);

		List<String> args = new LinkedList<>();
		Collections.addAll(args, "create-installer");
		Collections.addAll(args, "--output", output);
		Collections.addAll(args, "--name", name);
		Collections.addAll(args, "--app-image", appimage);

        JpmsGradlePlugin.trace("jpackager arguments: %s", args);

		// execute jpackager
		ToolRunner tool = TaskHelper.toolRunner("jpackager", jpackager);
        TaskHelper.runTool(tool, project, args);
	}

}
