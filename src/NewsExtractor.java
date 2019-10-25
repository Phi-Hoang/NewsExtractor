import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsExtractor {
    private HashSet<String> urls;
    private List<List<String>> articles;

    public NewsExtractor() {
        urls = new HashSet<>();
        articles = new ArrayList<>();
    }

    public void getPageLinks(String url) {
        Document doc;
        try {
            doc = Jsoup.connect(url).get();

            Elements links = doc.select("a[href]");
            String newsUrlPatternString = "^(\\/)([0-9]+)(\\/)([A-Za-z0-9]+-)*[A-Za-z0-9]+\\.html$";
            Pattern newsUrlPattern = Pattern.compile(newsUrlPatternString);

            for (Element link : links) {
                //System.out.println(link.attr("href"));

                Matcher matcher = newsUrlPattern.matcher(link.attr("href"));

                if (matcher.matches() && !urls.contains(link.absUrl("href"))) {
                    urls.add(link.absUrl("href"));
                }
            }
            urls.add(url);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void getNewsInfo() {
        List<String> newInfos;
        for (String url : urls) {
            Document doc;
            newInfos = new ArrayList<>();
            newInfos.add(url);
            try {
                Connection.Response execute = Jsoup.connect(url).execute();
                doc = Jsoup.parse(execute.body());
                //doc = Jsoup.connect(url).userAgent("Mozilla").get();
                Element title = doc.selectFirst("span.Title");
                newInfos.add(title.text());
                Element author = doc.selectFirst("span.ReferenceSourceTG");
                newInfos.add(author.text());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            articles.add(newInfos);
        }
    }

    public void exportCsv(String csvFile) {
        try {
            String header = "URL,Title,Author";
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8);
            writer.append("\uFEFF");
            writer.append(header);
            writer.append("\n");

            for (List<String> article : articles) {
                writeLine(writer, article);
            };

            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void writeLine(Writer w, List<String> values) throws IOException {
        boolean firstCol = true;
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if(!firstCol) {
                sb.append(",");
            }
            sb.append(escapeSpecialCharacters(value));
            firstCol = false;
        }
        sb.append("\n");
        w.append(sb.toString());
    }

    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        //"https://www.thesaigontimes.vn/td/295811/chu-so-huu-mon-hue-khai-tu-cua-hang-bien-mat-tren-mang.html"
        String url = "";

        while(!"exit".equalsIgnoreCase(url)) {
            System.out.println("Please enter the thesaigontimes news url or \"exit\" to quit:");
            url = reader.readLine();
            if (!url.isBlank() && !"exit".equalsIgnoreCase(url)) {
                if (url.startsWith("https://www.thesaigontimes.vn/")) {
                    NewsExtractor newsExtractor = new NewsExtractor();
                    newsExtractor.getPageLinks(url);
                    newsExtractor.getNewsInfo();
                    newsExtractor.exportCsv("NewsExported.csv");
                } else {
                    System.out.println("Invalid url.");
                }
            }
        }
    }
}
