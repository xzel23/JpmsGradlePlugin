package com.dua3.gradle.jpms.task;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.dua3.gradle.jpms.JpmsGradlePlugin;
import com.dua3.gradle.jpms.task.TaskHelper.ToolRunner;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public class Bundle extends DefaultTask {

	public static String FOLDER_NAME_BUNDLE = "bundle";

	@TaskAction
	public void bundle() {
		Project project = getProject();

		BundleExtension bundleExtension = (BundleExtension) project.getExtensions().getByName("bundle");
		JLinkExtension jlinkExtension = (JLinkExtension) project.getExtensions().getByName("jlink");

        // define folders
		String input = TaskHelper.getOutputFolder(project, "libs");
        String output = TaskHelper.getOutputFolder(project, FOLDER_NAME_BUNDLE);

		// remove output folder if it exists
		TaskHelper.removeFolder(output, true);
        
		// jpackager arguments
		String type = determineBundleType(TaskHelper.getFirst(bundleExtension.getType(), "image"));
		String name = TaskHelper.getFirst(bundleExtension.getName(), project.getName());
		String version = TaskHelper.getFirst(bundleExtension.getVersion(), "SNAPSHOT");
		String mainJar = TaskHelper.getFirst(bundleExtension.getMainJar(), project.getName()+".jar");
		String main = TaskHelper.getFirst(bundleExtension.getMain(), jlinkExtension.getMain());
		String runtimeImage = TaskHelper.getOutputFolder(project, JLink.FOLDER_NAME);
		String[] extraArgs = bundleExtension.getExtraArgs();

		List<String> args = new LinkedList<>();
		
		if (type.equals("image")) {
			Collections.addAll(args, "create-image");
		} else {
			Collections.addAll(args, "create-installer", type);
		}

		Collections.addAll(args, "--verbose");
		Collections.addAll(args, "--echo-mode");
		Collections.addAll(args, "--runtime-image", runtimeImage);
		Collections.addAll(args, "--input", input);
		Collections.addAll(args, "--output", output);
		Collections.addAll(args, "--name", name);
		Collections.addAll(args, "--version", version);

		addIfPresent(args, "--main-jar", mainJar);
		addIfPresent(args, "--class", main);		
		
		Collections.addAll(args, extraArgs);

        JpmsGradlePlugin.trace("jpackager arguments:%n%n%s %s%n%n", "jpackager", String.join(" ", args));

		// execute jpackager
		ToolRunner tool = TaskHelper.JPACKAGER;
		int rc = TaskHelper.runTool(tool, project, args);
		if (rc!=0) {
			throw new IllegalStateException("Executing "+tool+" failed with exit status "+ rc);
		}
	}

	private String determineBundleType(String t) {
		switch (t) {
		case "":
		case "image":
			return "image";
		case "installer":
			String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			if (os.contains("windows")) {
				return "msi";
			}
			if (os.contains("mac")) {
				return "pkg";
			}
			if (os.contains("linux")) {
				return "deb";
			}
			throw new GradleException("Could not determine installer type for system. Set type explicitly.");
		default:
			return t;
		}
	}

    public void addIfPresent(List<String> args, String option, String arg) {
		if (!arg.isEmpty()) {
			args.add(option);
			args.add(arg);
		}
	}
}
