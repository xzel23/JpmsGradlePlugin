package com.dua3.gradle.jpms.task;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.spi.ToolProvider;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public class JLink extends DefaultTask {

	@TaskAction
	public void jlink() {
		Project project = getProject();

		ToolProvider jlink = ToolProvider.findFirst("jlink")
				.orElseThrow(() -> new GradleException("could not get an instance of the jlink tool."));

		// prepare compiler arguments		
		String modulePath = "libs${File.pathSeparatorChar}${java_home}/jmods";
		String moduleName = "test";
		String launcher = "${moduleName}=${moduleName}/com.dua3.md.jfx.MdViewer";

		List<String> compilerArgs = new LinkedList<>();
		Collections.addAll(compilerArgs, "--module-path", modulePath);
		Collections.addAll(compilerArgs, "--add-modules", moduleName);
		Collections.addAll(compilerArgs, "--launcher", launcher);
		Collections.addAll(compilerArgs, "--output", "dist");
		Collections.addAll(compilerArgs, "--compress", "2");
		Collections.addAll(compilerArgs, "--no-header-files", "--no-man-pages", "--strip-debug");

		/*
		 * def java_home = System.getenv('JAVA_HOME') task link(type: Exec) { dependsOn
		 * 'clean' dependsOn 'jar'
		 * 
		 * workingDir 'build'
		 * 
		 * commandLine "${java_home}/bin/jlink", '--module-path',
		 * "libs${File.pathSeparatorChar}${java_home}/jmods", '--add-modules',
		 * "${moduleName}", '--launcher',
		 * "${moduleName}=${moduleName}/com.dua3.md.jfx.MdViewer", '--output', 'dist',
		 * '--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages'
		 */
	}
}
