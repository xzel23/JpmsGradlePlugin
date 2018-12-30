package com.dua3.gradle.jpms.task;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class TaskHelper {

    interface ToolRunner {
		int run​(PrintWriter out, PrintWriter err, String... args);
	}

    public static final ToolProxy JLINK = new ToolProxy("jlink");
    public static final ToolProxy JAVAC = new ToolProxy("javac");
     
	private static class ToolProxy implements ToolRunner {
        final Optional<?> jlink;
        final Method methodRun;
        final String tool;

        ToolProxy(String tool) {
            this.tool = tool;

            Optional<?> jlink_ = Optional.empty(); 
            Method methodRun_ = null;
            try {
                Class<?> clsTP = TaskHelper.class.getClassLoader().loadClass("java.util.spi.ToolProvider");
                jlink_ = (Optional<?>) clsTP.getMethod("findFirst", String.class).invoke(null, tool);
                methodRun_ = clsTP.getMethod("run", PrintWriter.class, PrintWriter.class, String[].class);
            } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                JpmsGradlePlugin.trace("Could not get "+tool+" instance");
            }
            jlink = jlink_;
            methodRun = methodRun_;
        }

        public int run​(PrintWriter out, PrintWriter err, String... args) {
            if (!jlink.isPresent() || methodRun==null) {
                throw  new GradleException("could not get an instance of the "+tool+" tool.");
            }
            try {
                return (int) methodRun.invoke(jlink.get(), out, err, args);
            } catch (Exception e) {
                throw new GradleException("exception while running "+tool+" (plugin needs Java >= 9): "+e.getMessage(), e);
            }
		}
	}

    static void runTool(ToolRunner tool, Project project, List<String> args) {
        JpmsGradlePlugin.trace("runTool %s%n%s %s%n", tool, project, String.join(" ", args));
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int rc = tool.run​(new PrintWriter(out, true), new PrintWriter(out, true), args.toArray(new String[0]));

        try {
            String compilerOutput = out.toString(StandardCharsets.UTF_8.name());

            if (rc!=0) {
                System.err.println(compilerOutput);
                String msg = "tool execution failed with return code "+rc;
                project.getLogger().error(msg);
                throw new GradleException(msg);
            }

            JpmsGradlePlugin.trace("tool output:\n%s", compilerOutput);
        } catch (UnsupportedEncodingException e) {
            project.getLogger().warn("exception retrieving tool output.", e);
        } finally {
            JpmsGradlePlugin.trace("tool exit status: %d", rc);
        }
    }

    private TaskHelper() {
        // utility class
    }

}
