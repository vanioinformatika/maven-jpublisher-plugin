package com.gyszalai.maven.plugins.jpublisher;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * This goal calls Oracle JPublisher to generate Java classes from Oracle SQL types and procedures
 *
 * @author Gyula Szalai <gyszalai@gmail.com>
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, 
      threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JpublisherGenerateMojo extends AbstractMojo {

    /** Classpath separator */
    static private final String CLASSPATH_SEPARATOR = System.getProperty("os.name").startsWith("Windows") ? ";" : ":";
    /** The project being built. */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;
    @Parameter(defaultValue = "${run.classpath}")
    private String classpath;
    /** Flag to disable this goal. */
    @Parameter(property = "jpublisher.generate.skip", defaultValue = "true")
    private boolean skip;
    /** Database URL (e.g. jdbc:oracle:thin:
     * @myhost:1521:SID) */
    @Parameter(required = true)
    private String dbUrl;
    /** Database user */
    @Parameter(required = true)
    private String dbUser;
    /** Database user */
    @Parameter(required = true)
    private String dbPassword;
    /** JPublisher properties file (-props command line switch) */
    @Parameter(defaultValue = "${project.basedir}/src/main/jpublisher/jpub-props.txt")
    private String propsFile;
    /** JPublisher typelist file (-input command line switch) */
    @Parameter(defaultValue = "${project.basedir}/src/main/jpublisher/jpub-typelist.txt")
    private String typeListFile;

    /**
     * Main entry point
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("JPublisher code generation has been skipped per configuration of the skip parameter.");
            return;
        }
        
        String genSourceRoot = project.getBasedir().toString() + "/target/generated-sources/jpublisher";
        FileUtils.mkdir(genSourceRoot);
        project.addCompileSourceRoot(genSourceRoot);

        this.getLog().info("Generating JPublisher classes...");
        try {
            startJpublisherProcess(genSourceRoot);
            this.getLog().info("Generation finished");
        } catch (IOException ex) {
            Logger.getLogger(JpublisherGenerateMojo.class.getName()).log(Level.SEVERE, null, ex);
            throw new MojoExecutionException("Jpublisher generation failed", ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(JpublisherGenerateMojo.class.getName()).log(Level.SEVERE, null, ex);
            throw new MojoExecutionException("Jpublisher generation failed", ex);
        } catch (DependencyResolutionRequiredException ex) {
            Logger.getLogger(JpublisherGenerateMojo.class.getName()).log(Level.SEVERE, null, ex);
            throw new MojoExecutionException("Jpublisher generation failed", ex);
        }
    }

    /**
     * Creates a classpath string from runtime dependencies of the active project
     * @return The generated classpath string
     * @throws MojoExecutionException If getting runtime dependencies fails for any reason
     */
    private String getRuntimeClasspath() throws MojoExecutionException {
        StringBuilder sb = new StringBuilder();
        if (classpath != null) {
            sb.append(classpath);
        }
        try {
            List<String> classpathElements = project.getRuntimeClasspathElements();
            for (String element : classpathElements) {
                if (sb.length() > 0) {
                    sb.append(CLASSPATH_SEPARATOR);
                }
                sb.append(element);
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Getting runtime dependencies failed", e);
        }
        String runtimeClasspath = sb.toString();
        this.getLog().debug("Classpath: " + runtimeClasspath);
        return runtimeClasspath;
    }

    /**
     * Starts the JPublisher generator process
     * @param genSourceRoot The source root where the generated sources will be generated
     */
    private void startJpublisherProcess(String genSourceRoot) throws IOException, InterruptedException, MojoExecutionException, DependencyResolutionRequiredException {
        
        String javaHome = System.getenv("JAVA_HOME");
        String classPath = this.getRuntimeClasspath();
        this.getLog().debug("Classpath: " + classPath);

        StringBuilder jpubCommand = new StringBuilder();
        jpubCommand.append(javaHome).append("/bin/java ");
        jpubCommand.append(String.format("-cp %s ", classPath));
        jpubCommand.append("oracle.jpub.Main ");
        jpubCommand.append(String.format("-url=%s ", dbUrl));
        jpubCommand.append(String.format("-user=%s/%s ", dbUser, dbPassword));
        jpubCommand.append(String.format("-dir=%s ", genSourceRoot));
        jpubCommand.append(String.format("-props=%s ", propsFile));
        jpubCommand.append(String.format("-input=%s ", typeListFile));
        
        this.getLog().debug("Starting process: " + jpubCommand);

        Process p = Runtime.getRuntime().exec(jpubCommand.toString());
        readAndPrintStream(p.getInputStream());
        int exitValue = p.waitFor();
        readAndPrintStream(p.getErrorStream());

        this.getLog().debug("Exit value: " + exitValue);
    }
    
    private void readAndPrintStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                this.getLog().info(line);
            }
        } finally {
            reader.close();
        }
    }
    
}
