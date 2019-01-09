# Solr-Plugin

This maven plugin is responsible for acquiring Solr Database for DDF and downstream projects. It also starts and stops Solr during the integration tests.

## Installation

In your pom file add the dependency and its executions.

### pom.xml
<details><summary>Click to expand</summary>
<p>

```
<plugin>
    <groupId>org.codice.maven</groupId>
    <artifactId>solr-plugin</artifactId>
    <version>0.3-SNAPSHOT</version>
    <extensions>true</extensions>
    <configuration>
    </configuration>
    <executions>
        <execution>
            <id>solr-unpack</id>
            <phase>package</phase>
            <goals>
                <goal>prepare</goal>
            </goals>
        </execution>
        <execution>
            <id>ensure-solr-stopped</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>stop</goal>
            </goals>
        </execution>
        <execution>
            <id>solr-start</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>start</goal>
            </goals>
        </execution>
        <execution>
            <id>solr-stop</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>stop</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

</p>
</details>

##

Make sure to add the repositories to your settings.xml.

### /.m2/Settings.xml
<details><summary>Click to expand</summary>
<p>

```
<repositories>
    <repository>
        <id>codice-snapshots</id>
        <url>http://artifacts.codice.org/content/repositories/snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
    <repository>
        <id>codice-releases</id>
        <url>http://artifacts.codice.org/content/repositories/releases/</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

</p>
</details>

## Configuration
System properties supported:
* `skipTests` - whether to skip the testing portion of this plugin.
* `solrScriptRelativePath` - path relative to the project to find Solr in, if not the default.
* `solrPort` - port to run solr on, if not the default.


## Advanced Configuration
There are more configuration options available to change what artifact the plugin fetches.
In particular the version modifier defaults to `${ddf.version}` in DDF or to `${project.version}` in downstream projects.

* `groupId`
* `artifactId`
* `version`
* `packaging`
* `classifier`
