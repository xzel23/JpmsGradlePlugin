package com.dua3.gradle.jpms.task;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.spi.ToolProvider;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.dua3.gradle.jpms.JpmsGradlePlugin;

public class TaskHelper {

    static void runTool(ToolProvider tool, Project project, List<String> args) {
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
    }

    private TaskHelper() {
        // utility class
    }

}
