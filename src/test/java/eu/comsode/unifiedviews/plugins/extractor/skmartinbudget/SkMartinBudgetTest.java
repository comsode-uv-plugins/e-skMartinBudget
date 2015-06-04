package eu.comsode.unifiedviews.plugins.extractor.skmartinbudget;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import cz.cuni.mff.xrg.odcs.dpu.test.TestEnvironment;
import eu.unifiedviews.dataunit.files.FilesDataUnit;
import eu.unifiedviews.dataunit.files.WritableFilesDataUnit;
import eu.unifiedviews.helpers.dataunit.files.FilesHelper;
import eu.unifiedviews.helpers.dpu.test.config.ConfigurationBuilder;

public class SkMartinBudgetTest {
    @Test
    public void testFile() throws Exception {
        SkMartinBudgetConfig_V1 config = new SkMartinBudgetConfig_V1();

        // Prepare DPU.
        SkMartinBudget dpu = new SkMartinBudget();
        dpu.configure((new ConfigurationBuilder()).setDpuConfiguration(config).toString());

        // Prepare test environment.
        TestEnvironment environment = new TestEnvironment();

        // Prepare data unit.
        WritableFilesDataUnit filesOutput = environment.createFilesOutput("filesOutput");
        try {
            // Run.
            environment.run(dpu);

            // Get file iterator.
            Map<String, FilesDataUnit.Entry> outputFiles = FilesHelper.getFilesMap(filesOutput);
            Assert.assertEquals(5, outputFiles.size());

            FilesDataUnit.Entry entry = outputFiles.get("Zaverecny_ucet_mesta_za_rok_2010.pdf");
            byte[] outputContent = FileUtils.readFileToByteArray(new File(new URI(entry.getFileURIString())));
            byte[] expectedContent = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream("Zaverecny_ucet_mesta_za_rok_2010.pdf"));

            Assert.assertArrayEquals(expectedContent, outputContent);
        } finally {
            // Release resources.
            environment.release();
        }
    }
}
