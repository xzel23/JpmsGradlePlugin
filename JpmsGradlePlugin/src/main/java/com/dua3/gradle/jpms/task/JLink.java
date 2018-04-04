package com.dua3.gradle.jpms.task;

import java.util.Optional;
import java.util.spi.ToolProvider;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.TaskAction;

public class JLink extends DefaultTask {

	@TaskAction
	public void jlink() {
		Project project = getProject();
		JavaPluginConvention javaPlugin = project.getConvention().getPlugin(JavaPluginConvention.class);

		ToolProvider jl =ToolProvider.findFirst("jlink").orElseThrow(() -> new GradleException("could not get an instance of the jlink tool."));
				
				
				
		
		/*
		def java_home = System.getenv('JAVA_HOME')
				task link(type: Exec) {
				    dependsOn 'clean'
				    dependsOn 'jar'

				    workingDir 'build'

				    commandLine "${java_home}/bin/jlink", '--module-path', "libs${File.pathSeparatorChar}${java_home}/jmods",
				            '--add-modules', "${moduleName}", '--launcher', "${moduleName}=${moduleName}/com.dua3.md.jfx.MdViewer", '--output', 'dist', '--strip-debug',
				            '--compress', '2', '--no-header-files', '--no-man-pages'
				            */
	}
}
