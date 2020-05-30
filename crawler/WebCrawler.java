package crawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.*;

public class WebCrawler extends JFrame {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private JTextArea htmlTextArea;
    private JTextField urlTextField;
    private JToggleButton runButton;
    private JTextField depthTextField;
    private JCheckBox depthCheckBox;
    private JLabel parsedLabel;
    private JButton exportButton;
    private JLabel titleLabel;
    private JTable titlesTable;
    private JTextField exportUrlTextField;
    private String[] titlesTableHeader = new String[] {"URL", "Title"};
    private ConcurrentLinkedQueue<String[]> dataQueue = new ConcurrentLinkedQueue<>();
    private Map<String, String> mapData = new LinkedHashMap<>();
    private Set<String> visitedLinks = new ConcurrentSkipListSet<>();
    private ConcurrentLinkedQueue<String[]> urlQueue = new ConcurrentLinkedQueue<>();
    private final Pattern patternTitle = Pattern.compile("(<title[\\w=\\-\"]*>)(.*?)(</title>)");
    private final Pattern patternTag = Pattern.compile("(<a.*?href=[\"'])(.*?)([\"'].*?>)(.*?)(</a>)");
    private final Pattern patternBaseUrl = Pattern.compile("(https?://)([\\w.-]+)(.*?)(/?)([^/]*)");
    private final Pattern patternNormalUrl = Pattern.compile("https?://.*?");
    private final Pattern patternRelativeUrl = Pattern.compile("(/?)([\\w.%/-]*)");
    private int depthLimit = 2;
    private int workerCount = 5;
    private int parsedPagesCount = 0;

    public WebCrawler() {
        super("Web Crawler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        urlTextField = getUrlTextField();
        runButton = getRunButton();
        depthTextField = getDepthTextField();
        depthCheckBox = getDepthCheckBox();
        titleLabel = getTitleLabel();
        htmlTextArea = getHtmlTextArea();
        titlesTable = getTitlesTable();
        parsedLabel = getParsedLabel();
        exportUrlTextField = getExportUrlTextField();
        exportButton = getExportButton();
        titlesTable.setVisible(false);
        htmlTextArea.setVisible(false);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("URL:"), gbc);
        gbc.gridx = 1;
        panel.add(urlTextField, gbc);
        gbc.gridx = 2;
        panel.add(runButton, gbc);
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Maximum depth:"), gbc);
        gbc.gridx = 1;
        panel.add(depthTextField, gbc);
        gbc.gridx = 2;
        panel.add(depthCheckBox, gbc);
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(new JLabel("Parsed pages:"), gbc);
        gbc.gridwidth = 2;
        gbc.gridx = 1;
        panel.add(parsedLabel, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(new JLabel("Export: "), gbc);
        gbc.gridx = 1;
        panel.add(exportUrlTextField, gbc);
        gbc.gridx = 2;
        panel.add(exportButton, gbc);
        setContentPane(panel);
        setSize(650, 200);
        setVisible(true);
    }

    private JTextField getUrlTextField() {
        JTextField textField = new JTextField(40);
        textField.setName("UrlTextField");
        return textField;
    }

    private JToggleButton getRunButton() {
        JToggleButton button = new JToggleButton("Run");
        button.setSize( 100, 25);
        button.setName("RunButton");
        button.addActionListener(e -> startCrawl());
        return button;
    }



    private JTextField getDepthTextField() {
        JTextField depthTextField = new JTextField();
        depthTextField.setName("DepthTextField");
        depthTextField.setText("1");
        return depthTextField;
    }

    private JCheckBox getDepthCheckBox() {
        JCheckBox depthCheckBox = new JCheckBox("Enabled");
        depthCheckBox.setName("DepthCheckBox");
        return depthCheckBox;
    }

    private JLabel getParsedLabel() {
        JLabel parsedLabel = new JLabel("0");
        parsedLabel.setName("ParsedLabel");
        return parsedLabel;
    }

    private JLabel getTitleLabel() {
        JLabel titleLabel = new JLabel("Title: ");
        titleLabel.setName("TitleLabel");
        return titleLabel;
    }

    private JTable getTitlesTable() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("URL");
        model.addColumn("Title");
        JTable titlesTable = new JTable(model);
        titlesTable.setName("TitlesTable");
        return titlesTable;
    }

    private JTextArea getHtmlTextArea() {
        JTextArea textArea = new JTextArea("HTML code?", 38, 47);
        textArea.setName("HtmlTextArea");
        textArea.setVisible(true);
        textArea.setEnabled(false);
        textArea.setLineWrap(true);
        return textArea;
    }

    private JTextField getExportUrlTextField() {
        JTextField exportUrlTextField = new JTextField(30);
        exportUrlTextField.setName("ExportUrlTextField");
        return exportUrlTextField;
    }

    private JButton getExportButton() {
        JButton exportButton = new JButton("Export");
        exportButton.setSize( 100, 25);
        exportButton.setName("ExportButton");
        exportButton.addActionListener(e -> exportToFile());
        return exportButton;
    }

    private void exportToFile() {
        try (PrintWriter writer = new PrintWriter(exportUrlTextField.getText())) {
            String[] entry;
            /*
            while (dataQueue.peek() != null) {
                entry = dataQueue.poll();
                writer.println(entry[0]);
                writer.println(entry[1]);
            }
             */
            mapData.forEach((k, v) -> {
                writer.println(k);
                writer.println(v);
            });
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void parseHtml() {
        try {
            final String url = urlTextField.getText();
            final InputStream inputStream = new URL(url).openStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            final StringBuilder stringBuilder = new StringBuilder();
            final String LINE_SEPARATOR = System.getProperty("line.separator");

            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                stringBuilder.append(nextLine);
                stringBuilder.append(LINE_SEPARATOR);
            }
            final String siteText = stringBuilder.toString();
            Matcher matcher = patternTitle.matcher(siteText);
            if (matcher.find()) {
                titleLabel.setText(matcher.group(2));
            } else {
                titleLabel.setText("No title");
            }
            htmlTextArea.setText(siteText);
            mapData.put(url, titleLabel.getText());
        } catch (Exception exception) {
            htmlTextArea.setText(exception.getMessage());
        }
    }

    private void startCrawl() {
        try {
            mapData.clear();
            urlQueue.clear();
            visitedLinks.clear();
            workerCount = 10;
            CrawlerThread[] threads = new CrawlerThread[workerCount];
            depthLimit = Integer.parseInt(depthTextField.getText());
            //if (depthLimit > 100) depthLimit = 100;
            int depthLevel = 0;
            String[] url;
            int i = 0;
            threads[0] = new CrawlerThread(urlTextField.getText(), urlTextField.getText(), depthLevel, mapData, visitedLinks, urlQueue);
            threads[0].start();
            threads[0].join();
            while (!urlQueue.isEmpty()) {
                while (!urlQueue.isEmpty()) {
                    if (threads[i] == null) {
                        url = urlQueue.poll();
                        if (Integer.valueOf(url[2]) < depthLimit) {
                            threads[i] = new CrawlerThread(url[0], url[1], Integer.valueOf(url[2]) + 1, mapData, visitedLinks, urlQueue);
                            threads[i].start();
                        }
                    } else if (!threads[i].isAlive()) {
                        threads[i].join();
                        url = urlQueue.poll();
                        if (Integer.valueOf(url[2]) < depthLimit) {
                            threads[i] = new CrawlerThread(url[0], url[1], Integer.valueOf(url[2]) + 1, mapData, visitedLinks, urlQueue);
                            threads[i].start();
                        }
                    }
                    parsedLabel.setText(String.valueOf(mapData.size()));
                    i = (i + 1) % workerCount;
                }
                for (int k = 0; k < workerCount; k++) {
                    if (threads[k] != null) threads[k].join();
                }
                parsedLabel.setText(String.valueOf(mapData.size()));
            }
            parsedLabel.setText(String.valueOf(mapData.size()));
            if (!urlQueue.isEmpty()) {
                try (PrintWriter writer = new PrintWriter("C:\\Apps\\errorlog.txt")) {
                    writer.print(urlQueue.poll().toString());
                } catch (FileNotFoundException fileNotFoundException) {
                    fileNotFoundException.printStackTrace();
                }
            }
        } catch (Exception e) {
            try (PrintWriter writer = new PrintWriter("C:\\Apps\\errorlog.txt")) {
                writer.print(e.toString());
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        }
    }


    private void getLinks() {
        try {
            mapData.clear();
            parseHtml();
            Matcher matcherTag = patternTag.matcher(htmlTextArea.getText());
            Matcher matcherUrl;
            String baseUrl;
            String currentUrl;
            String urlString;
            Matcher matcherBaseUrl = patternBaseUrl.matcher(urlTextField.getText());
            if (matcherBaseUrl.matches()) {
                baseUrl = matcherBaseUrl.group(1) + matcherBaseUrl.group(2);
                currentUrl = matcherBaseUrl.group(1) + matcherBaseUrl.group(2) + matcherBaseUrl.group(3) + matcherBaseUrl.group(4);
            } else {
                baseUrl = "http://localhost";
                currentUrl = "http://localhost/";
            }
            while (matcherTag.find()) {
                if (patternNormalUrl.matcher(matcherTag.group(2)).matches()) {
                    addToMapData(matcherTag.group(2));
                } else {
                    matcherUrl = patternRelativeUrl.matcher(matcherTag.group(2));
                    if (matcherUrl.matches()) {
                        if (matcherUrl.group(1).length() == 1) {
                            urlString = baseUrl + matcherUrl.group(0);
                        } else {
                            urlString = currentUrl + matcherUrl.group(0);
                        }
                        if (!addToMapData(urlString)) {
                            if (matcherUrl.group(1).length() == 1) {
                                urlString = "http:/" + matcherUrl.group(0);
                            } else {
                                urlString = "http://" + matcherUrl.group(0);
                            }
                            if (!addToMapData(urlString)) {
                                if (matcherUrl.group(1).length() == 1) {
                                    urlString = "https:/" + matcherUrl.group(0);
                                } else {
                                    urlString = "https://" + matcherUrl.group(0);
                                }
                                addToMapData(urlString);
                            }
                        }
                    }
                }
            }
            String[][] tableData = new String[mapData.size()][];
            int i = 0;
            for (Map.Entry<String, String> entry : mapData.entrySet()) {
                tableData[i] = new String[] { entry.getKey(), entry.getValue() };
                i++;
            }
            if (i > 0) {
                titlesTable.setModel(new DefaultTableModel(tableData, titlesTableHeader));
                titlesTable.setEnabled(true);
            }
        } catch (Exception exception) {
             htmlTextArea.setText(exception.getMessage());
        }
    }

    private boolean addToMapData(String url) {
        try {
            URL urlObject = new URL(url);
            URLConnection connection = urlObject.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            if (connection.getContentType() != null) {
                if (connection.getContentType().contains("text/html")) {
                    mapData.put(url, findTitleInUrl(connection));
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            htmlTextArea.setText(exception.getMessage());
            return false;
        }
    }

    private void crawlPage(String url) throws IOException {
        String nextUrl;
        String baseUrl;
        String currentUrl;
        Matcher matcherTag = patternTag.matcher(getHtml(url));
        Matcher matcherBaseUrl = patternBaseUrl.matcher(url);
        if (matcherBaseUrl.matches()) {
            baseUrl = matcherBaseUrl.group(1) + matcherBaseUrl.group(2);
            currentUrl = matcherBaseUrl.group(1) + matcherBaseUrl.group(2) + matcherBaseUrl.group(3) + matcherBaseUrl.group(4);
        } else {
            baseUrl = "http://localhost";
            currentUrl = "http://localhost/";
        }
        while (matcherTag.find()) {
            nextUrl = getValidUrl(matcherTag.group(2), baseUrl, currentUrl);
            if (!"".equals(nextUrl)) {
                crawlPage(url);
            }
        }
    }

    private String findTitleInUrl(URLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String nextLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((nextLine = reader.readLine()) != null) {
            stringBuilder.append(nextLine);
            stringBuilder.append(LINE_SEPARATOR);
        }
        Matcher matcherTitle = patternTitle.matcher(stringBuilder);
        if (matcherTitle.find()) {
            return matcherTitle.group(2);
        }
        return "No title";
    }

    private String getHtml(String url) throws IOException {
        final InputStream inputStream = new URL(url).openStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        final StringBuilder stringBuilder = new StringBuilder();
        final String LINE_SEPARATOR = System.getProperty("line.separator");

        String nextLine;
        while ((nextLine = reader.readLine()) != null) {
            stringBuilder.append(nextLine);
            stringBuilder.append(LINE_SEPARATOR);
        }
        return stringBuilder.toString();
    }

    private String getValidUrl(String attrHref, String baseUrl, String currentUrl) throws IOException {
        String url;
        if (patternNormalUrl.matcher(attrHref).matches()) {
            if (addToMapData(attrHref)) return attrHref;
        } else {
            Matcher matcherUrl = patternRelativeUrl.matcher(attrHref);
            if (matcherUrl.matches()) {
                if (matcherUrl.group(1).length() == 1) {
                    url = baseUrl + matcherUrl.group(0);
                } else {
                    url = currentUrl + matcherUrl.group(0);
                }
                if (addToMapData(url)) {
                    return url;
                } else {
                    if (matcherUrl.group(1).length() == 1) {
                        url = "http:/" + matcherUrl.group(0);
                    } else {
                        url = "http://" + matcherUrl.group(0);
                    }
                    if (addToMapData(url)) {
                        return url;
                    } else {
                        if (matcherUrl.group(1).length() == 1) {
                            url = "https:/" + matcherUrl.group(0);
                        } else {
                            url = "https://" + matcherUrl.group(0);
                        }
                        if (addToMapData(url)) return url;
                    }
                }
            }
        }
        return "";
    }

}

class CrawlerThread extends Thread {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private final Pattern patternTitle = Pattern.compile("(<title[\\w=\\-\"]*>)(.*?)(</title>)");
    private final Pattern patternTag = Pattern.compile("(<a[\\w\\s\"']*?href=[\"'])(.*?)([\"'].*?>)(.*?)(</a>)");
    private final Pattern patternBaseUrl = Pattern.compile("(https?://)([\\w.:-]+)(.*?)(/?)([^/]*)");
    private final Pattern patternNormalUrl = Pattern.compile("https?://.*?");
    private final Pattern patternRelativeUrl = Pattern.compile("(/?)(.*)");
    private ConcurrentLinkedQueue<String[]> dataQueue;
    private Map<String, String> mapData;
    private Set<String> visitedLinks;
    private String url;
    private ConcurrentLinkedQueue<String[]> urlQueue;
    private String baseUrl;
    private String currentUrl;
    private int depthLevel;

    public CrawlerThread(String url, String homeUrl, int depthLevel, Map<String, String> mapData, Set<String> visitedLinks, ConcurrentLinkedQueue<String[]> urlQueue) {
        this.url = url;
        this.mapData = mapData;
        this.urlQueue = urlQueue;
        this.depthLevel = depthLevel;
        this.visitedLinks = visitedLinks;
        Matcher matcherBaseUrl = patternBaseUrl.matcher(homeUrl);
        if (matcherBaseUrl.matches()) {
            baseUrl = matcherBaseUrl.group(1) + matcherBaseUrl.group(2);
            currentUrl = matcherBaseUrl.group(1) + matcherBaseUrl.group(2) + matcherBaseUrl.group(3) + matcherBaseUrl.group(4);
        } else if (homeUrl.substring(homeUrl.length() - 1).equals(LINE_SEPARATOR)) {
            baseUrl = homeUrl.substring(0, homeUrl.length() - 1);
            currentUrl = homeUrl;
        } else {
            baseUrl = homeUrl;
            currentUrl = homeUrl + LINE_SEPARATOR;
        }
    }

    @Override
    public void run() {
        if (patternNormalUrl.matcher(url).matches()) {
            checkWebpage(url);
        } else {
            Matcher matcherUrl = patternRelativeUrl.matcher(url);
            if (matcherUrl.matches()) {
                if (matcherUrl.group(0).substring(0, 1).equals(LINE_SEPARATOR)) {
                    url = baseUrl + matcherUrl.group(0);
                } else {
                    url = currentUrl + matcherUrl.group(0);
                }
                if (!checkWebpage(url)) {
                    if (matcherUrl.group(0).substring(0, 1).equals(LINE_SEPARATOR)) {
                        url = "http:/" + matcherUrl.group(0);
                    } else {
                        url = "http://" + matcherUrl.group(0);
                    }
                    if (!checkWebpage(url)) {
                        if (matcherUrl.group(1).length() == 1) {
                            url = "https:/" + matcherUrl.group(0);
                        } else {
                            url = "https://" + matcherUrl.group(0);
                        }
                        checkWebpage(url);
                    }
                }
            }
        }
    }

    private String getHtml(String url) {
        final InputStream inputStream;
        try {
            inputStream = new URL(url).openStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            final StringBuilder stringBuilder = new StringBuilder();
            final String LINE_SEPARATOR = System.getProperty("line.separator");
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                stringBuilder.append(nextLine);
                stringBuilder.append(LINE_SEPARATOR);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            try (PrintWriter writer = new PrintWriter("C:\\Apps\\errorlog.txt")) {
                writer.print(e.toString());
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        }
        return "";
    }

    private boolean checkWebpage(String url) {
        try {
            if (visitedLinks.contains(url)) return false;
            String title;
            URL urlObject = new URL(url);
            URLConnection connection = urlObject.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            if (connection.getContentType() != null) {
                if
                (connection.getContentType().contains("text/html")) {
                    String html = getHtml(url);
                    Matcher matcherTitle = patternTitle.matcher(html);
                    if (matcherTitle.find()) {
                        title = matcherTitle.group(2);
                    } else {
                        title = "No title";
                    }
                    //dataQueue.offer(new String[] { url, title });
                    synchronized (CrawlerThread.class) {
                        mapData.put(url, title);
                        visitedLinks.add(url);
                    }

                    Matcher matcherTag = patternTag.matcher(html);
                    while (matcherTag.find()) {
                        urlQueue.offer(new String[]{matcherTag.group(2), url, String.valueOf(depthLevel)});
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            try (PrintWriter writer = new PrintWriter("C:\\Apps\\errorlog.txt")) {
                writer.print(e.toString());
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        }
        return false;
    }

}