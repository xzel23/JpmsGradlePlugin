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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskOutputs;
import org.gradle.jvm.tasks.Jar;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class TaskHelper {

    /** Logger instance. */
    private static final Logger LOGGER = Logging.getLogger(TaskHelper.class);

    /** Interface for tool invocations. */
    interface ToolRunner {
        int run(PrintWriter out, PrintWriter err, String... args);
    }

    /** System property that points to the installation directory of jpackager. */
    public static final String PROPERTY_PATH_TO_JPACKAGER = "PATH_TO_JPACKAGER";
    /**
     * Value of the system property that points to the installation directory of
     * jpackager.
     */
    private static final String PATH_TO_JPACKAGER = System.getProperty(PROPERTY_PATH_TO_JPACKAGER);
    /** The default jar file name of the jpackager jar. */
    private static final String JPACKAGER_JAR_NAME = "jdk.packager.jar";
    /** The class conatining jpackager's main method. */
    private static final String JPACKAGER_CLASS = "jdk.packager.Main";

    /** The jlink tool. */
    public static final ToolRunner JLINK = new ToolProxy("jlink");
    /** The Java compiler. */
    public static final ToolRunner JAVAC = new ToolProxy("javac");
    /** The Java packager. */
    public static final ToolRunner JPACKAGER;

    // prepare the java packager tool
    static {
        ToolRunner packager = null;

        if (PATH_TO_JPACKAGER != null) {
            try {
                Path path;
                if (PATH_TO_JPACKAGER.toLowerCase().endsWith(".jar")) {
                    path = Paths.get(PATH_TO_JPACKAGER);
                } else {
                    path = Paths.get(PATH_TO_JPACKAGER, JPACKAGER_JAR_NAME);
                }
                URL[] packagerUrl = { path.toUri().toURL() };
                LOGGER.debug("path to jpackager jar: "+packagerUrl[0]);
                URLClassLoader cl = new URLClassLoader(packagerUrl);
                Class<?> cls = cl.loadClass(JPACKAGER_CLASS);
                Method run = cls.getMethod("run", PrintWriter.class, PrintWriter.class, String[].class);
                packager = (out, err, args) -> {
                    try {
                        LOGGER.debug("invoking packager");
                        return (int) run.invoke(null, out, err ,args);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        LOGGER.warn("error invoking jpackager", e);
                        return -1;
                    }
                };
            } catch (ClassNotFoundException e) {
                LOGGER.warn(JPACKAGER_CLASS + " not on classpath");
            } catch (NoSuchMethodException e) {
                LOGGER.warn(JPACKAGER_CLASS + " does not define a valid main method.", e);
            } catch (SecurityException e) {
                LOGGER.warn("cannot access main method of " + JPACKAGER_CLASS + ".", e);
            } catch (MalformedURLException e) {
                LOGGER.warn("the property " + PROPERTY_PATH_TO_JPACKAGER + " is not a valid path.", e);
            }
        }

        if (packager != null) {
            LOGGER.debug("using packager jar");
            JPACKAGER = packager;
        } else {
            LOGGER.warn("falling back to using executable; make sure  jpackager it is on the path.");
            JPACKAGER = TaskHelper.toolRunner("jpackager", "jpackager");
        }
    } 
     
    public static ToolRunner toolRunner(String name, String pathToExecutable) {
        return new TaskHelper.ToolRunner() {
            @Override
            public int run(PrintWriter out, PrintWriter err, String... args) {
                ProcessBuilder builder = new ProcessBuilder();
                List<String> command = new ArrayList<>(args.length+1);
                command.add(pathToExecutable);
                command.addAll(Arrays.asList(args));
                builder.command(command);
                
                try {
                    Process p = builder.start();
                    Executors.newSingleThreadExecutor().submit(() -> copyOutput(p.getInputStream(), out));
                    Executors.newSingleThreadExecutor().submit(() -> copyOutput(p.getErrorStream(), err));
                    return p.waitFor();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("interruppted", e);
                }
            }

            private void copyOutput(InputStream in, PrintWriter out) {
                new BufferedReader(new InputStreamReader(in)).lines().forEach(out::println);
            }

            @Override
            public String toString() {
                return name;
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

        public int run(PrintWriter out, PrintWriter err, String... args) {
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
            return name;
        }
	}

    static int runTool(ToolRunner tool, Project project, List<String> args) {
        JpmsGradlePlugin.trace("runTool %s%n%s %s%n", tool, project, String.join(" ", args));
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int rc = tool.run(new PrintWriter(out, true), new PrintWriter(out, true), args.toArray(new String[0]));

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

        return rc;
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

    public static String orDefault(String s, String dflt) {
        return s!=null && !s.isEmpty() ? s : dflt;
    }

	public static String getModulePath(Project project) {
		// setup module path - 1. collect all generated jars
		String projectModulePath = project.getTasks()
		    .withType(Jar.class)
		    .stream()
		    .filter(t
		    		-> !t.getClassifier().equalsIgnoreCase("javadoc")
		    		&& !t.getClassifier().equalsIgnoreCase("src")
		    		&& !t.getClassifier().equalsIgnoreCase("sources"))
		    .map(Task::getOutputs)
		    .map(TaskOutputs::getFiles)
		    .map(FileCollection::getAsPath)
            .collect(Collectors.joining(File.pathSeparator));
            
		// setup module path - 2. collect runtime dependencies
		String dependendyModulePath = project.getConfigurations().getByName("runtime").getAsPath();

		// setup module path - 3. the JDK modules
		String jmods = System.getProperties().getProperty("java.home") + File.separator + "jmods";

        // setup module path - putting it all together
        String modulePath = Arrays.asList(projectModulePath, dependendyModulePath, jmods)
            .stream()
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(File.pathSeparator));
		return modulePath;
	}

	public static String getModules(String rootModule, String extraModules) {
		if (rootModule.isEmpty()) {
			throw new GradleException("root module is not set in buid.gradle");
		}

		if (extraModules.isEmpty()) {
			return rootModule;
		} else {
			return rootModule + "," + extraModules;
		}
	}

    private TaskHelper() {
        // utility class
    }

}
