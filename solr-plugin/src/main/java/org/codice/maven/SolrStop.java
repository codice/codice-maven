package org.codice.maven;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class SolrStop extends BaseSolrPlugin {

  @Override
  public void doExecute() {

    // Try stopping solr.
    getLog().info("Stopping solr");

    String[] StopArgs = {"stop", "-p", solrPort};
    int exitCode = doSolr(StopArgs);

    // On linux/mac, a stop command that found no process to end returns exit code 1.
    // On Windows it returns exit code 0.
    if (exitCode != 0) {
      getLog().warn("Solr didn't stop or wasn't running");
      getLog().warn("Trying to stop solr again.");
      exitCode = doSolr(StopArgs); // Try to stop it again

      // If it didn't stop, check the OS. If it's not windows, then its ok to see status code 1.
      if (exitCode != 0 && SystemUtils.IS_OS_WINDOWS) {
        getLog().error("Solr could not be stopped.");
        return;
      }
    }

    getLog().info("Solr stopped");
  }
}
