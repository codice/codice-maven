package org.codice.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class SolrStop extends BaseSolrPlugin {

  @Override
  public void doExecute() {

    executor.setExitValues(new int[] {0, 1});

    // Try stopping solr.
    getLog().info("Stopping solr");

    String[] StopArgs = {"stop", "-p", solrPort};
    int exitCode = doSolr(StopArgs);

    // If we got status code 1, we'll try again just to confirm the line-ending glitch is the cause.
    if (exitCode != 0) {
      getLog().warn("Solr did not stop");
      getLog().warn("Trying to stop solr again.");
      exitCode = doSolr(StopArgs); // Try to stop it again

      // If it didn't start
      if (exitCode != 0) {
        getLog().error("Solr could not be stopped.");
        return;
      }
    }

    getLog().info("Solr stopped");
  }
}
