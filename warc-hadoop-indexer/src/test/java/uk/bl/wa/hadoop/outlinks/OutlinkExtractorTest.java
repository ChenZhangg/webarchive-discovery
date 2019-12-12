package uk.bl.wa.hadoop.outlinks;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputLogFilter;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

import uk.bl.wa.hadoop.mapreduce.MapReduceTestBaseClass;

public class OutlinkExtractorTest extends MapReduceTestBaseClass {

    private static final Log log = LogFactory
            .getLog(OutlinkExtractorTest.class);

    @Test
    public void test() throws Exception {
        log.info("Checking input file is present...");
        // Check that the input file is present:
        Path[] inputFiles = FileUtil.stat2Paths(
                getFileSystem().listStatus(input, new OutputLogFilter()));
        Assert.assertEquals(2, inputFiles.length);

        // Set up arguments for the job:
        // FIXME The input file could be written by this test.
        String[] args = { "src/test/resources/test-inputs.txt",
                this.output.getName() };

        // Setup:
        OutlinkExtractor job = new OutlinkExtractor();

        // run job
        log.info("Setting up job config...");
        JobConf conf = this.mrCluster.createJobConf();
        ToolRunner.run(conf, job, args);
        log.info("Job finished, checking the results...");

        // check the output
        Path[] outputFiles = FileUtil.stat2Paths(
                getFileSystem().listStatus(output, new OutputLogFilter()));
        // Assert.assertEquals(1, outputFiles.length);

        // Check contents of the output:
        for (Path output : outputFiles) {
            log.info(" --- output : " + output);
            if (getFileSystem().isFile(output)) {
                InputStream is = getFileSystem().open(output);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    log.info(line);
                }
                reader.close();
            } else {
                log.info(" --- ...skipping directory...");
            }
        }
    }

}