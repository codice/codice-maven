package org.codice.maven;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringJoiner;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

@Mojo(name = "prepare", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false)
public class SolrUnpack extends BaseSolrPlugin {
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject mvnProject;

  /** Parameters for getting and unpacking solr if it isn't present */
  @Parameter(defaultValue = "ddf")
  public String groupId;

  @Parameter(defaultValue = "solr-distro")
  public String artifactId;

  @Parameter(defaultValue = "${ddf.version}")
  public String version;

  @Parameter(defaultValue = "zip")
  public String packaging;

  @Parameter(defaultValue = "assembly")
  public String classifier;

  @Override
  public void doExecute() throws MojoExecutionException, MojoFailureException {
    acquireSolrPackage();
  }

  /**
   * This function calls the maven dependency plugin to reacquire and unpack solr in case the script
   * is missing.
   *
   * @throws MojoFailureException
   */
  private void acquireSolrPackage() throws MojoFailureException {

    Properties props = makeProperties();

    // Create the command to be download and unpack solr
    DefaultInvocationRequest req = new DefaultInvocationRequest();
    req.setPomFile(new File(mvnProject.getBasedir(), "pom.xml"));
    req.setGoals(Arrays.asList("dependency:unpack")); // command to run
    req.setProperties(props); // arguments for the command
    req.setBatchMode(true); // maven in non-interactive mode

    Invoker invoker = new DefaultInvoker();
    InvocationResult result;

    getLog().info("Trying to acquire solr package");

    // Execute function
    try {
      result = invoker.execute(req);
    } catch (MavenInvocationException e) {
      getLog().debug(e.getCause());
      throw new MojoFailureException("Could not unpack solr");
    }

    if (result.getExitCode() != 0) {
      if (result.getExecutionException() != null) {
        throw new MojoFailureException(
            "Failed to unpack solr package. ", result.getExecutionException());
      } else {
        throw new MojoFailureException("Failed to unpack solr package. " + result.getExitCode());
      }
    }

    getLog().info("Fetched solr package");
  }

  /**
   * This function creates the properties involved with the plugin to fetch and where to store it.
   *
   * @return properties
   */
  private Properties makeProperties() {
    // Create the arguments
    StringJoiner joiner = new StringJoiner(":");
    joiner.add(groupId).add(artifactId).add(getVersion()).add(packaging).add(classifier);
    String coordinates = joiner.toString();
    Path outputPath = Paths.get(mvnProject.getBasedir().toString(), "target");

    // Place the arguments into a properties file.
    Properties props = new Properties();
    props.setProperty("artifact", coordinates);
    props.setProperty("outputDirectory", outputPath.toString());

    return props;
  }

  /**
   * This function allows version to be correct in both ddf and downstream projects.
   *
   * @return version string
   */
  private String getVersion() {
    if (version == null || version == "") {
      return mvnProject.getVersion();
    } else {
      return version;
    }
  }
}
