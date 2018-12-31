package org.codice.maven;

import java.io.File;
import java.io.IOException;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class BaseSolrPlugin extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject mvnProject;

  /** Location of the solr script relative to target */
  @Parameter(defaultValue = "\\solr\\bin\\solr")
  public String solrScriptRelativePath;

  /** Port of the solr server */
  @Parameter(defaultValue = "8983")
  public String solrPort;

  /** Location of the solr script, created by appending the relative to the project path */
  private File solrScript;

  /** Needed class variables involved in setting up the execution environment */
  private Executor executor;

  private PumpStreamHandler streamHandler;
  private ProcessDestroyer processDestroyer;

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
    // Detect the os and either use the bash script or the windows "cmd" script.
    if (SystemUtils.IS_OS_WINDOWS) {
      solrScript = new File(mvnProject.getBuild().getDirectory() + solrScriptRelativePath + ".cmd");
    } else {
      solrScript = new File(mvnProject.getBuild().getDirectory() + solrScriptRelativePath);
    }

    // Does that file exist?
    if (!solrScript.exists()) {
      System.out.println("didn't find solr script");
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
      System.out.println("Failed to execute: " + cmd);
      e.printStackTrace(); // TODO: Log this somewhere

    } catch (IOException e) {
      System.out.println("IO Exception, directory likely doesn't exist: " + cmd);
      e.printStackTrace(); // TODO: Log this somewhere

    } finally {
      try {
        streamHandler.stop();
      } catch (IOException e) {
        e.printStackTrace();
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

  private void rebuildSolrPackage() {
    // TODO: rebuild solr package.
  }
}
