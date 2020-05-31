package crawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.*;

public class WebCrawler extends JFrame {

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
    private Map<String, String> mapData = new LinkedHashMap<>();
    private Set<String> visitedLinks = new ConcurrentSkipListSet<>();
    private ConcurrentLinkedQueue<String[]> urlQueue = new ConcurrentLinkedQueue<>();
    private int depthLimit = 2;
    private int workerCount = 5;

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
            mapData.forEach((k, v) -> {
                writer.println(k);
                writer.println(v);
            });
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void startCrawl() {
        try {
            mapData.clear();
            urlQueue.clear();
            visitedLinks.clear();
            workerCount = 10;
            depthLimit = Integer.parseInt(depthTextField.getText());
            int depthLevel = 0;
            urlQueue.offer(new String[] { urlTextField.getText(), urlTextField.getText(), String.valueOf(depthLevel) });
            Thread[] threads = new Thread[workerCount];

            int i = 0;
            boolean loadNewJob;
            while (!urlQueue.isEmpty()) {
                while (!urlQueue.isEmpty()) {
                    if (threads[i] != null) {
                        if (!threads[i].isAlive()) loadNewJob = true;
                        else loadNewJob = false;
                    } else loadNewJob = true;
                    if (loadNewJob) {
                        String[] url = urlQueue.poll();
                        Webpage webpage = new Webpage(url[0], url[1], visitedLinks);

                        threads[i] = new Thread(() -> {
                            if (Integer.parseInt(url[2]) <= depthLimit) {
                                if (webpage.open()) {
                                    mapData.put(webpage.getValidUrl(), webpage.getTitle());
                                    visitedLinks.add(webpage.getValidUrl());
                                    while (!webpage.urlQueue.isEmpty()) {
                                        String[] addToQueue = webpage.urlQueue.poll();
                                        urlQueue.offer(new String[]{addToQueue[0], addToQueue[1], String.valueOf(Integer.parseInt(url[2]) + 1)});
                                    }
                                }
                            }
                        });
                        threads[i].start();
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
            runButton.setSelected(false);
        } catch (Exception e) {
            try (PrintWriter writer = new PrintWriter("C:\\Apps\\errorlog.txt")) {
                writer.print(e.getStackTrace().toString());
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        }
    }
}

class Webpage {

    private final Pattern patternTitle = Pattern.compile("(<title[\\w=\\-\"]*>)(.*?)(</title>)");
    private final Pattern patternTag = Pattern.compile("(<a[\\w\\s\"']*?href=[\"'])(.*?)([\"'].*?>)(.*?)(</a>)");
    private final Pattern patternBaseUrl = Pattern.compile("(https?://)([\\w.:-]+)(.*?)(/?)(.*?)(/?)([^/]*)");
    private final Pattern patternNormalUrl = Pattern.compile("https?://.*?");
    private final Pattern patternRelativeUrl = Pattern.compile("(/?)(.*)");
    private Set<String> visitedLinks;
    private String url;
    public ArrayDeque<String[]> urlQueue = new ArrayDeque<>();
    private String baseUrl;
    private String currentUrl;
    private String html;
    private String validUrl;
    private String title;

    public Webpage(String url, String homeUrl, Set<String> visitedLinks) {
        this.url = url;
        this.visitedLinks = visitedLinks;
        Matcher matcherBaseUrl = patternBaseUrl.matcher(homeUrl);
        if (matcherBaseUrl.matches()) {
            baseUrl = matcherBaseUrl.group(1) + matcherBaseUrl.group(2);
            currentUrl = matcherBaseUrl.group(1) + matcherBaseUrl.group(2) + matcherBaseUrl.group(3) + matcherBaseUrl.group(4);
        } else if (homeUrl.substring(homeUrl.length() - 1).equals("/")) {
            baseUrl = homeUrl.substring(0, homeUrl.length() - 1);
            currentUrl = homeUrl;
        } else {
            baseUrl = homeUrl;
            currentUrl = homeUrl + "/";
        }
    }

    public String getValidUrl() {
        return validUrl;
    }

    public String getTitle() {
        return title;
    }

    public boolean open() {
        if (patternNormalUrl.matcher(url).matches()) {
            return (parsePage(url));
        } else {
            Matcher matcherUrl = patternRelativeUrl.matcher(url);
            if (matcherUrl.matches()) {
                if (matcherUrl.group(0).substring(0, 1).equals("/")) {
                    url = baseUrl + matcherUrl.group(0);
                } else {
                    url = currentUrl + matcherUrl.group(0);
                }
                if (parsePage(url)) return true;
                else {
                    if (matcherUrl.group(0).substring(0, 1).equals("/")) {
                        url = "http:/" + matcherUrl.group(0);
                    } else {
                        url = "http://" + matcherUrl.group(0);
                    }
                    if (parsePage(url)) return true;
                    else {
                        if (matcherUrl.group(1).length() == 1) {
                            url = "https:/" + matcherUrl.group(0);
                        } else {
                            url = "https://" + matcherUrl.group(0);
                        }
                        return parsePage(url);
                    }
                }
            }
        }
        return false;
    }

    private boolean parseHtml(String url) {
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
            html = stringBuilder.toString();
            return true;
        } catch (Exception e) {
            try (PrintWriter writer = new PrintWriter("C:\\Apps\\errorlog.txt")) {
                writer.print(e.toString());
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        }
        return false;
    }

    private boolean parsePage(String url) {
        try {
            if (visitedLinks.contains(url)) return false;
            URL urlObject = new URL(url);
            URLConnection connection = urlObject.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            if (connection.getContentType() != null) {
                if
                (connection.getContentType().contains("text/html")) {
                    if (parseHtml(url)) {
                        Matcher matcherTitle = patternTitle.matcher(html);
                        if (matcherTitle.find()) {
                            title = matcherTitle.group(2);
                        } else {
                            title = "No title";
                        }
                        validUrl = url;
                        Matcher matcherTag = patternTag.matcher(html);
                        while (matcherTag.find()) {
                            urlQueue.offer(new String[]{matcherTag.group(2), url});
                        }
                        return true;
                    }
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