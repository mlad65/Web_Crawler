package crawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler extends JFrame {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private JTextArea htmlTextArea;
    private JTextField urlTextField;
    private JButton runButton;
    private JButton exportButton;
    private JLabel titleLabel;
    private JTable titlesTable;
    private JTextField exportUrlTextField;
    private Map<String, String> mapData = new TreeMap<>();
    private Pattern patternTitle = Pattern.compile("(<title[\\w=\\-\"]*>)(.*?)(</title>)");
    final private String[] titlesTableHeader = new String[] {"URL", "Title"};

    public WebCrawler() {
        super("Web Crawler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        urlTextField = getUrlTextField();
        htmlTextArea = getHtmlTextArea();
        runButton = getRunButton();
        titleLabel = getTitleLabel();
        titlesTable = getTitlesTable();
        exportUrlTextField = getExportUrlTextField();
        exportButton = getExportButton();
        titlesTable.setEnabled(false);
        htmlTextArea.setVisible(false);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("URL: "), gbc);
        gbc.gridx = 1;
        panel.add(urlTextField, gbc);
        gbc.gridx = 2;
        panel.add(runButton, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(titleLabel, gbc);
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        panel.add(htmlTextArea, gbc);
        panel.add(new JScrollPane(titlesTable), gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Export: "), gbc);
        gbc.gridx = 1;
        panel.add(exportUrlTextField, gbc);
        gbc.gridx = 2;
        panel.add(exportButton, gbc);
        setContentPane(panel);
        setSize(620, 620);
        setVisible(true);
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
            for (Map.Entry<String, String> entry : mapData.entrySet()) {
                writer.println(entry.getKey());
                writer.println(entry.getValue());
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private JTextField getExportUrlTextField() {
        JTextField exportUrlTextField = new JTextField(30);
        exportUrlTextField.setName("ExportUrlTextField");
        return exportUrlTextField;
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

    private JTextField getUrlTextField() {
        JTextField textField = new JTextField(40);
        textField.setName("UrlTextField");
        return textField;
    }

    private JButton getRunButton() {
        JButton button = new JButton("Get text!");
        button.setSize( 100, 25);
        button.setName("RunButton");
        button.addActionListener(e -> getLinks());
        return button;
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

    private void getLinks() {
        try {
            mapData.clear();
            parseHtml();
            Pattern patternTag = Pattern.compile("(<a.*?href=[\"'])(.*?)([\"'].*?>)(.*?)(</a>)");
            Matcher matcherTag = patternTag.matcher(htmlTextArea.getText());
            Pattern patternBaseUrl = Pattern.compile("(https?://)([\\w.-]+)(.*?)(/?)([^/]*)");
            Pattern patternNormalUrl = Pattern.compile("https?://.*?");
            Pattern patternRelativeUrl = Pattern.compile("(/?)([\\w.%/-]*)");
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

    private boolean addToMapData(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            if (connection.getContentType() != null) {
                if (connection.getContentType().contains("text/html")) {
                    mapData.put(urlString, findTitleInUrl(connection));
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            htmlTextArea.setText(exception.getMessage());
            return false;
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

}