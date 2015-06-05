package eu.comsode.unifiedviews.plugins.extractor.skmartinbudget;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dataunit.DataUnit;
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

    private static final String HOST = "http://martin.sk";

    private String sessionId = null;

    private Set<String> uniqueNames;

    @DataUnit.AsOutput(name = "filesOutput")
    public WritableFilesDataUnit filesOutput;

    public SkMartinBudget() {
        super(SkMartinBudgetVaadinDialog.class, ConfigHistory.noHistory(SkMartinBudgetConfig_V1.class));
    }

    @Override
    protected void innerExecute() throws DPUException {
        uniqueNames = new HashSet<String>();
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String response = getServerResponse(INPUT_URL, httpclient);
            Document doc = null;
            doc = Jsoup.parse(response);
            LOG.debug(doc.html());

            Element content = doc.select("div.kategorie").first();
            Element ul = content.select("ul.ui").first();
            Elements list = ul.select("li");

            for (Element item : list) {
                Element strong = item.select("strong").first();
                if (strong == null) {
                    continue;
                }
                Element subPageLink = strong.select("a[href]").first();
                String subPageUrl = null;
                if (subPageLink != null && !StringUtils.isBlank(subPageLink.attr("href"))) {
                    subPageUrl = HOST + subPageLink.attr("href");
                    if (subPageLink.text().contains("2009")) {
                        break;
                    }
                }

                String subPage = getServerResponse(subPageUrl, httpclient);

                String[] fileLink = getFinalBillFile(subPage, httpclient);
                if (fileLink == null || StringUtils.isBlank(fileLink[0])) {
                    continue;
                }
                String outputSymbolicName = null;

                HttpGet getFile = new HttpGet(HOST + fileLink[0]);
                CloseableHttpResponse getFileResp = httpclient.execute(getFile);
                Header[] getFileRespHeaders = getFileResp.getAllHeaders();
                StringBuilder headerSb = new StringBuilder();
                for (Header h : getFileRespHeaders) {
                    headerSb.append("Key : " + h.getName() + " ,Value : " + h.getValue() + "\n");
                }
                LOG.debug(headerSb.toString());
                Header[] contDis = getFileResp.getHeaders("Content-Disposition");
                if (contDis != null && contDis.length == 1) {
                    String[] contDisParts = contDis[0].getValue().split("; ");
                    for (String pair : contDisParts) {
                        String[] pairKeyValue = pair.split("=");
                        if (pairKeyValue.length == 2) {
                            String key = pairKeyValue[0].trim();
                            if (key.equals("filename*")) {
                                outputSymbolicName = URLDecoder.decode(pairKeyValue[1].trim(), "UTF-8").replaceAll("UTF-8''", "");
                            }
                        }
                    }
                }
                if (StringUtils.isBlank(outputSymbolicName) || !uniqueNames.add(outputSymbolicName)) {
                    outputSymbolicName = Long.toString((new Date()).getTime());
                    uniqueNames.add(outputSymbolicName);
                }
                File outputDirectory;
                outputDirectory = new File(URI.create(filesOutput.getBaseFileURIString()));
                File outputFile = File.createTempFile("____", FilenameUtils.getExtension(outputSymbolicName), outputDirectory);
                try {
                    FileUtils.copyInputStreamToFile(getFileResp.getEntity().getContent(), outputFile);
                } finally {
                    EntityUtils.consumeQuietly(getFileResp.getEntity());
                    getFileResp.close();
                }

                filesOutput.addExistingFile(outputSymbolicName, outputFile.toURI().toASCIIString());
                VirtualPathHelpers.setVirtualPath(filesOutput, outputSymbolicName, outputSymbolicName);
                Resource resource = ResourceHelpers.getResource(filesOutput, outputSymbolicName);
                Date now = new Date();
                resource.setCreated(now);
                resource.setLast_modified(now);
                resource.setSize(outputFile.length());
                resource.setDescription(fileLink[1]);

                ResourceHelpers.setResource(filesOutput, outputSymbolicName, resource);
            }
        } catch (Exception ex) {
            throw ContextUtils.dpuException(ctx, ex, "SkMartinBudget.execute.exception");
        }
    }

    private String getServerResponse(String url, CloseableHttpClient httpClient) {
        String response = null;
        try {
            URL subPageUrl = new URL(url);
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            httpGet.setHeader("Accept-Encoding", "gzip, deflate");
            httpGet.setHeader("Accept-Language", "en-US,cs;q=0.7,en;q=0.3");
            httpGet.setHeader("Connection", "keep-alive");
            if (!StringUtils.isBlank(sessionId)) {
                httpGet.setHeader("Cookie", sessionId);
            }
            httpGet.setHeader("Referer", INPUT_URL);
            httpGet.setHeader("Host", subPageUrl.getHost()); //"egov.martin.sk"
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0");
            CloseableHttpResponse response1 = httpClient.execute(httpGet);

            LOG.debug(String.format("GET response status line: %s", response1.getStatusLine()));
            int responseCode = response1.getStatusLine().getStatusCode();
            StringBuilder headerSb = new StringBuilder();
            for (Header h : response1.getAllHeaders()) {
                headerSb.append("Key : " + h.getName() + " ,Value : " + h.getValue());
            }
            LOG.debug(headerSb.toString());

            Header[] cookies = response1.getHeaders("Set-Cookie");
            if (cookies != null && cookies.length > 0 && cookies[0] != null) {
                String[] cookieParts = cookies[0].getValue().split("; ");
                sessionId = cookieParts[0];
            }
            if (responseCode != HttpStatus.SC_OK) {
                LOG.error("GET request not worked");
                throw new Exception("GET request not worked");
            }
            HttpEntity entity = null;
            try {
                entity = response1.getEntity();
                response = EntityUtils.toString(entity);
            } finally {
                EntityUtils.consumeQuietly(entity);
                response1.close();
            }
        } catch (Exception ex) {
            new Exception("Problem processing server response!", ex);
        }
        return response;
    }

    private String[] getFinalBillFile(String subPage, CloseableHttpClient httpclient) {
        Element page = Jsoup.parse(subPage);
        Elements linksOnPage = page.select("a[href]");
        for (Element link : linksOnPage) {
            if (StringUtils.isNotBlank(link.text()) && removeAccents(link.text()).startsWith("zaverecny ucet")) {
                if (link.attr("href").contains("id_dokumenty")) {
                    String result[] = { link.attr("href"), link.text() };
                    return result;
                } else {
                    String subSubPage = getServerResponse(HOST + link.attr("href"), httpclient);
                    return getFinalBillFile(subSubPage, httpclient);
                }
            }
        }
        return null;
    }

    private String removeAccents(String input) {
        String result = StringUtils.stripAccents(input);
        result = StringUtils.lowerCase(result).trim();
        result = result.replaceAll("[^a-zA-Z0-9\\s]", "");
        return result;
    }

}
