package org.codice.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

// TODO: get a logger in here for when things go wrong.
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class SolrStart extends BaseSolrPlugin {

  @Override
  public void doExecute() throws MojoFailureException {
    // Try starting solr.
    System.out.println("Starting solr");
    String[] startArgs = {"start", "-p", solrPort};
    int exitCode = doSolr(startArgs);

    // If it didn't start, it may be running
    if (exitCode != 0) {
      throw new MojoFailureException("Cannot start solr: " + exitCode);
    }

    System.out.println("Solr Started");
  }
}
