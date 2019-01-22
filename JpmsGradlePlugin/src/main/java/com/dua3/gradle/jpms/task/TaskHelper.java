package com.dua3.gradle.jpms.task;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class TaskHelper {

    interface ToolRunner {
		int run​(PrintWriter out, PrintWriter err, String... args);
	}

    public static final ToolRunner JLINK = new ToolProxy("jlink");
    public static final ToolRunner JAVAC = new ToolProxy("javac");
     
    public static ToolRunner toolRunner(String name, String pathToExecutable) {
        return new TaskHelper.ToolRunner() {
            @Override
            public int run​(PrintWriter out, PrintWriter err, String... args) {
                ProcessBuilder builder = new ProcessBuilder();
                List<String> command = new ArrayList<>(args.length+1);
                command.add(pathToExecutable);
                command.addAll(Arrays.asList(args));
                builder.command(command);
                
                try {
                    Process p = builder.start();
                    Executors.newSingleThreadExecutor().submit(() -> copyOutput(p.getInputStream(), out));
                    Executors.newSingleThreadExecutor().submit(() -> copyOutput(p.getErrorStream(), err));
                    return p.exitValue();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            private void copyOutput(InputStream in, PrintWriter out) {
                new BufferedReader(new InputStreamReader(in)).lines().forEach(out::println);
            }

            @Override
            public String toString() {
                return "ToolRunner["+name+"]";
            }
        };
    }

	private static class ToolProxy implements ToolRunner {
        final Optional<?> tool;
        final Method methodRun;
        final String name;

        ToolProxy(String tool) {
            this.name = tool;

            Optional<?> tool_ = Optional.empty(); 
            Method methodRun_ = null;
            try {
                Class<?> clsTP = TaskHelper.class.getClassLoader().loadClass("java.util.spi.ToolProvider");
                tool_ = (Optional<?>) clsTP.getMethod("findFirst", String.class).invoke(null, tool);
                methodRun_ = clsTP.getMethod("run", PrintWriter.class, PrintWriter.class, String[].class);
            } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                JpmsGradlePlugin.trace("Could not get "+tool+" instance");
            }
            this.tool = tool_;
            methodRun = methodRun_;
        }

        public int run​(PrintWriter out, PrintWriter err, String... args) {
            if (!tool.isPresent() || methodRun==null) {
                throw  new GradleException("could not get an instance of the "+name+" tool.");
            }
            try {
                return (int) methodRun.invoke(tool.get(), out, err, args);
            } catch (Exception e) {
                throw new GradleException("exception while running "+name+" (plugin needs Java >= 9): "+e.getMessage(), e);
            }
        }
        
        @Override
        public String toString() {
            return "ToolRunner["+name+"]";
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

	public static void removeFolder(String output) {
		Path outputFolder = Paths.get(output);
		if (Files.exists(outputFolder)) {
			JpmsGradlePlugin.trace("removing output folder: " + outputFolder);
			try {
				Files.walk(outputFolder, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder()).forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			} catch (IOException | UncheckedIOException e) {
				throw new GradleException("could not delete output folder", e);
			}
		}
    }
    
	public static String getOutputFolder(Project project, String name) {
		return project.getBuildDir().getAbsolutePath()+File.separator+name;
	}

    private TaskHelper() {
        // utility class
    }

}
