File log = new File(basedir, 'build.log')
assert log.exists()

//Assert that 4 maven goals got skipped: unpack, ensure-stopped, solr-start, solr-stop
searchString = "[INFO] Skipping goal due to skipTests setting"
int count = 0
for (String line : log.readLines()) {
    if (line.equals(searchString)) {
        count++;
    }
}

assert count == 4 // todo: hardcoded. Three because there are 3 steps to be skipped: ensure-stopped, solr-start, and solr-stop

//Assert there is a solr folder in the target directory. Skipping tests does not mean skip acquiring solr.
File targetDir = new File(basedir, 'target')
File[] files = targetDir.listFiles()
assert files != null && !files.contains(new File(targetDir, "solr"))
