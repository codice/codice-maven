package org.codice.maven;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

// TODO: get a logger in here for when things go wrong.
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class SolrPlugin extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject mvnProject;

  /** Location of the solr script relative to target */
  @Parameter(defaultValue = "\\solr\\bin\\solr")
  public String solrScriptRelativePath;

  /** Location of the solr script, created by appending the relative to the project path. */
  private File solrScript;

  private Executor executor;

  private PumpStreamHandler streamHandler;

  //private ByteArrayOutputStream outputStream;

  private ProcessDestroyer processDestroyer;

  public void execute() throws MojoExecutionException {

    // Detect the os and either use the bash script or the windows "cmd" script to run solr.
    if (SystemUtils.IS_OS_WINDOWS) {
      solrScript = new File(mvnProject.getBuild().getDirectory() + solrScriptRelativePath + ".cmd");
    } else {
      solrScript = new File(mvnProject.getBuild().getDirectory() + solrScriptRelativePath);
    }

    System.out.println(solrScript.getPath());

    // Does that file exist?
    if (!solrScript.exists()) {
      System.out.println("didn't find file");
      throw new MojoExecutionException("didn't find script.");
      // TODO: Decide on functionality when file isn't there. Rebuild.
    }

    // Set up for execution
    setupExecutionEnvironment();

    // Try starting solr.
    System.out.println("starting solr");
    int exitCode = doSolr(solrScript, "start");
    //System.out.println(exitCode);

    // If it didn't start, it may be running
    // TODO: What to do if solr was already running? Stop and Start?
    if (exitCode != 0 && exitCode != -1) {
      System.out.println("couldn't start solr");
      // Try to stop it
      System.out.println("trying to stop solr");
      String[] args = {"stop", "-all"};
      exitCode = doSolr(solrScript, args);

      if (exitCode != 0 && exitCode != -1) {
        // Couldn't stop solar.
        throw new MojoExecutionException("Cannot stop running solr. " + exitCode);
      }

      // try to start solr again.
      System.out.println("trying to start solr again.");
      exitCode = doSolr(solrScript, "start");

      if (exitCode != 0 && exitCode != -1) {
        // Couldn't start solar.
        throw new MojoExecutionException("Cannot start solr.");
      }
    }

    System.out.println("solar started fine");
  }

  /**
   * Overloaded method for convenience of calling solr with one argument
   *
   * @param script file pointing at the script to run
   * @param argument optional, additional arguments to pass to the script.
   * @return
   */
  private int doSolr(File script, String argument) {
    if (argument == null) {
      argument = "";
    }
    String[] arg = {argument};
    return doSolr(script, arg);
  }

  // TODO: this method will append the given arguments to the solr script
  private int doSolr(File script, String[] arguments) {
    if (arguments == null) {
      arguments = new String[] {""};
    }

    CommandLine cmd = new CommandLine(script);
    for (String arg : arguments) {
      cmd.addArgument(arg);
    }

    //    ExecuteResultHandler resultHandler =
    //        new ExecuteResultHandler() {
    //
    //          public void onProcessFailed(ExecuteException e) {
    //            getLog().error("Async process failed for: " + cmd, e);
    //          }
    //
    //          public void onProcessComplete(int exitValue) {
    //            getLog().info("Async process complete, exit value = " + exitValue + " for: " +
    // cmd);
    //            try {
    //              streamHandler.stop();
    //            } catch (IOException e) {
    //              getLog().error("Error stopping async process stream handler for: " + cmd, e);
    //            }
    //          }
    //        };

    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

    executor.setStreamHandler(streamHandler);

    try {
      streamHandler.start();
      executor.execute(cmd, resultHandler);

      resultHandler.waitFor();
      // System.out.println("got past waitfor");

      if (resultHandler.getException() != null) {
        throw resultHandler.getException();
      }
    } catch (ExecuteException e) {
      System.out.println("Failed to execute: " + cmd);
      e.printStackTrace(); // TODO: Log this somewhere

    } catch (IOException e) {
      System.out.println("IO Exception, directory likely doesn't exist: " + cmd);
      e.printStackTrace(); // TODO: Log this somewhere

    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      try { // todo: not a big fan of having another try/catch inside the finally block.
        streamHandler.stop();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return resultHandler.getExitValue();
  }

  /** Sets up the class specific variables needed for the asynchronous environment. */
  private void setupExecutionEnvironment() {
    executor = new DefaultExecutor();

    // If the process takes to long, we'll take control back.
    ExecuteWatchdog watchdog = new ExecuteWatchdog(15000);
    executor.setWatchdog(watchdog);

    // Responsible for shutting down the command.
    executor.setProcessDestroyer(getProcessDestroyer());

    // Output stream of the executed function, unused so far.
    //outputStream = new ByteArrayOutputStream();

    // Combines command output from stout and sterr.
    // send stdout and sterr nowhere. If they are placed into a stream, they block the thread from
    // continuing.
    streamHandler = new PumpStreamHandler(null);
  }

  private ProcessDestroyer getProcessDestroyer() {
    if (processDestroyer == null) {
      processDestroyer = new ShutdownHookProcessDestroyer();
    }
    return processDestroyer;
  }
}
