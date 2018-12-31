package org.codice.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

public abstract class BaseSolrPlugin extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject mvnProject;

  /** Location of the solr script relative to target */
  @Parameter(defaultValue = "\\solr\\bin\\solr")
  public String solrScriptRelativePath;

  /** Port of the solr server */
  @Parameter(defaultValue = "8983")
  public String solrPort;

  /** Whether the integration tests are being skipped or not */
  @Parameter() public Boolean skip;

  /** Parameters for getting and unpacking solr if it isn't present */
  @Parameter(defaultValue = "ddf")
  public String groupId;

  @Parameter(defaultValue = "solr-distro")
  public String artifactId;
  // TODO: fetch version from maven.
  @Parameter(defaultValue = "2.14.0-SNAPSHOT")
  public String version;

  @Parameter(defaultValue = "zip")
  public String packaging;

  @Parameter(defaultValue = "assembly")
  public String classifier;

  /** Needed class variables involved in setting up the execution environment */
  Executor executor;

  private PumpStreamHandler streamHandler;
  private ProcessDestroyer processDestroyer;

  /** Location of the solr script, created by appending the relative to the project path */
  private File solrScript;

  /**
   * All child classes must override this method with the logic of their goal. This method is called
   * by the real execute method in the abstract super class.
   *
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  abstract void doExecute() throws MojoExecutionException, MojoFailureException;

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    // If we are skipping integration tests then no need to run solr.
    if (skip != null && skip) {
      return;
    }

    // Detect the os and either use the bash script or the windows "cmd" script.
    if (SystemUtils.IS_OS_WINDOWS) {
      solrScript = new File(mvnProject.getBuild().getDirectory() + solrScriptRelativePath + ".cmd");
    } else {
      solrScript = new File(mvnProject.getBuild().getDirectory() + solrScriptRelativePath);
    }

    // Does that file exist?
    if (!solrScript.exists()) {
      getLog().error("Could not find solr script");
      // If it doesn't we'll try to rebuild the solr package before failing the build.
      rebuildSolrPackage();
    }

    // Set up for execution
    setupExecutionEnvironment();

    // Execute child's goal
    doExecute();
  }

  /**
   * Overloaded method for convenience of calling solr with one argument
   *
   * @param argument optional, additional arguments to pass to the script.
   * @return statusCode of command.
   */
  int doSolr(String argument) {
    if (argument == null) {
      argument = "";
    }
    String[] arg = {argument};
    return doSolr(arg);
  }

  /**
   * Executes a command to the solr script with arguments.
   *
   * @param arguments optional, additional arguments to pass to the script.
   * @return statusCode of command
   */
  int doSolr(String[] arguments) {
    if (arguments == null) {
      arguments = new String[] {""};
    }

    CommandLine cmd = new CommandLine(solrScript);
    for (String arg : arguments) {
      cmd.addArgument(arg);
    }

    int exitCode = -1;
    executor.setStreamHandler(streamHandler);

    try {
      streamHandler.start();
      exitCode = executor.execute(cmd);

    } catch (ExecuteException e) {
      getLog().error("Failed to execute: " + cmd);
      getLog().debug(e.getCause());

    } catch (IOException e) {
      System.out.println("IO Exception, directory likely doesn't exist: " + cmd);
      getLog().debug(e.getCause());

    } finally {
      try {
        streamHandler.stop();
      } catch (IOException e) {
        getLog().debug(e.getCause());
      }
    }

    return exitCode;
  }

  /** Sets up the class specific variables needed for the asynchronous environment. */
  void setupExecutionEnvironment() {
    executor = new DefaultExecutor();

    // Responsible for shutting down the command.
    executor.setProcessDestroyer(getProcessDestroyer());

    // Combines command output from stout and sterr, sending it to null since we don't need their
    // output.
    streamHandler = new PumpStreamHandler(null);
  }

  private ProcessDestroyer getProcessDestroyer() {
    if (processDestroyer == null) {
      processDestroyer = new ShutdownHookProcessDestroyer();
    }
    return processDestroyer;
  }

  private void rebuildSolrPackage() throws MojoFailureException {

    // Create the arguments for
    StringJoiner joiner = new StringJoiner(":");
    joiner.add(groupId).add(artifactId).add(version).add(packaging).add(classifier);
    String coordinates = joiner.toString();
    Path outputPath = Paths.get(mvnProject.getBasedir().toString(), "target");

    // Place the arguments into a properties file.
    Properties props = new Properties();
    props.setProperty("artifact", coordinates);
    props.setProperty("outputDirectory", outputPath.toString());

    // Create the command to be download and unpack solr
    DefaultInvocationRequest req = new DefaultInvocationRequest();
    req.setPomFile(new File(mvnProject.getBasedir(), "pom.xml"));
    req.setGoals(Arrays.asList("dependency:unpack")); // command to run
    req.setProperties(props); // arguments for the command
    req.setBatchMode(true); // maven in non-interactive mode

    Invoker invoker = new DefaultInvoker();
    InvocationResult result;

    getLog().info("Trying to reacquire solr package");

    try {
      result = invoker.execute(req);
    } catch (MavenInvocationException e) {
      e.printStackTrace();
      throw new MojoFailureException("Could not unpack solr");
    }

    if (result.getExitCode() != 0) {
      if (result.getExecutionException() != null) {
        throw new MojoFailureException(
            "Failed to rebuild solr package.", result.getExecutionException());
      } else {
        throw new MojoFailureException(
            "Failed to rebuild solr package. Exit code: " + result.getExitCode());
      }
    }

    getLog().info("Fetched solr package; resuming.");
  }
}
