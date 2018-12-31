package org.codice.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

// TODO: get a logger in here for when things go wrong.
@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class SolrStop extends BaseSolrPlugin {

  @Override
  public void doExecute() {
    // solr.cmd stop status code is either a 1 or 0. Neither actually mean an error has occurred.
    executor.setExitValues(new int[] {0, 1});

    // Try stopping solr.
    getLog().info("Stopping solr");
    String[] StopArgs = {"stop", "-all"};
    int exitCode = doSolr(StopArgs);

    // If we got status code 1, we'll try again just to confirm the line-ending glitch is the cause.
    if (exitCode != 0) {
      getLog().warn("Solr didn't stop");
      getLog().warn("Trying to stop solr to confirm line-ending glitch.");
      doSolr(StopArgs); // Try to stop it again

      /* Why don't we throw an exception here if exitCode is 1 (an error)?
       * If the script has linux (LF) line endings on windows (CRLF), then it always gives status
       * code 1. Solr still will have stopped.
       */

    }

    getLog().info("Solr stopped");
  }
}
