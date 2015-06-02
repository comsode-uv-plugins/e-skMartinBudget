package eu.comsode.unifiedviews.plugins.extractor.skmartinbudget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.files.WritableFilesDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dataunit.resource.Resource;
import eu.unifiedviews.helpers.dataunit.resource.ResourceHelpers;
import eu.unifiedviews.helpers.dataunit.virtualpath.VirtualPathHelpers;
import eu.unifiedviews.helpers.dpu.config.ConfigHistory;
import eu.unifiedviews.helpers.dpu.context.ContextUtils;
import eu.unifiedviews.helpers.dpu.exec.AbstractDpu;

/**
 * Main data processing unit class.
 */
@DPU.AsExtractor
public class SkMartinBudget extends AbstractDpu<SkMartinBudgetConfig_V1> {

    private static final Logger LOG = LoggerFactory.getLogger(SkMartinBudget.class);

    private static final String INPUT_URL = "http://martin.sk/rozpocet/ds-1215/p1=19902";

    @DataUnit.AsOutput(name = "filesOutput")
    public WritableFilesDataUnit filesOutput;

    public SkMartinBudget() {
        super(SkMartinBudgetVaadinDialog.class, ConfigHistory.noHistory(SkMartinBudgetConfig_V1.class));
    }

    @Override
    protected void innerExecute() throws DPUException {
        try {
            Document doc = null;
            doc = Jsoup.connect(INPUT_URL).userAgent("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2").get();

            Element content = doc.select("td.td_content_body").first();
            Elements links = content.select("a[href]");

            for (Element fileLink : links) {
                URL website = null;
                website = new URL(fileLink.absUrl("href"));
                String outputVirtualPath = FilenameUtils.getName(website.getPath());
                String outputSymbolicName = outputVirtualPath;
                File outputDirectory;
                outputDirectory = new File(URI.create(filesOutput.getBaseFileURIString()));
                File outputFile = File.createTempFile("____", FilenameUtils.getExtension(outputVirtualPath), outputDirectory);
                
                URLConnection hc = website.openConnection();
                hc.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
                InputStream input = hc.getInputStream();
                FileUtils.copyInputStreamToFile(input, outputFile);

                String description = fileLink.text();

                filesOutput.addExistingFile(outputVirtualPath, outputFile.toURI().toASCIIString());
                VirtualPathHelpers.setVirtualPath(filesOutput, outputSymbolicName, outputVirtualPath);
                Resource resource = ResourceHelpers.getResource(filesOutput, outputSymbolicName);
                Date now = new Date();
                resource.setCreated(now);
                resource.setLast_modified(now);
                resource.setSize(outputFile.length());
                resource.setDescription(description);

                ResourceHelpers.setResource(filesOutput, outputSymbolicName, resource);
            }
        } catch (DataUnitException | IOException ex) {
            throw ContextUtils.dpuException(ctx, ex, "SkUipsSchools.execute.exception");
        }

    }
    
    public static FileOutputStream openOutputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file);
    }
}
