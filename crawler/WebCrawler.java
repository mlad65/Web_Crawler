package crawler;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebCrawler extends JFrame {

    private JTextArea htmlTextArea;
    private JTextField urlTextField;
    private JButton runButton;

    public WebCrawler() {
        super("Web Crawler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel contents = new JPanel();
        urlTextField = getUrlTextField();
        htmlTextArea = getHtmlTextArea();
        runButton = getRunButton();
        contents.add(urlTextField);
        contents.add(runButton);
        contents.add(new JScrollPane(htmlTextArea), BorderLayout.CENTER);
        setContentPane(contents);
        setSize(560, 700);
        setVisible(true);
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
        JTextField textField = new JTextField(38);
        textField.setName("UrlTextField");
        return textField;
    }

    private JButton getRunButton() {
        JButton button = new JButton("Get text!");
        button.setSize( 100, 25);
        button.setName("RunButton");
        button.addActionListener(e -> {
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
                htmlTextArea.setText(siteText);
            } catch (Exception exception) {
                htmlTextArea.setText(exception.getMessage());
            }
        });
        return button;
    }
}