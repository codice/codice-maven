import java.nio.file.Path

File log = new File(basedir, 'build.log')
assert log.exists()

//Assert there is a solr folder in the target directory
File targetDir = new File(basedir, 'target')
File[] files = targetDir.listFiles()
assert files != null && files.contains(new File(targetDir, "solr"))

//Assert Solr started
searchString = "[INFO] Solr Started"    // todo: hardcoded
assert log.readLines().contains(searchString)

//Assert Solr stopped in logs
searchString = "[INFO] Solr stopped"    // todo: hardcoded
int count = 0
for (String line : log.readLines()) {
    if (line.equals(searchString)) {
        count++;
    }
}
assert count == 2 // todo: hardcoded. Two because solr will stop both before and after the tests.

//Assert Solr actually stopped
boolean portBound = true
try {
    socket = new ServerSocket(8983) // todo: hardcoded
    portBound = false
} catch (IOException e) {
}

assert !portBound


