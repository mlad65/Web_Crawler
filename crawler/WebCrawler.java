package crawler;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebCrawler extends JFrame {

    private JTextArea htmlTextArea;
    private JTextField urlTextField;

    public WebCrawler() {
        super("Web Crawler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(540, 700);
        setVisible(true);
        setLayout(null);
        JPanel contents = new JPanel();
        urlTextField = getUrlTextField();
        contents.add(urlTextField);
        contents.add(getRunButton());
        htmlTextArea = getHtmlTextArea();
        contents.add(htmlTextArea);
        setContentPane(contents);
    }

    private JTextArea getHtmlTextArea() {
        JTextArea textArea = new JTextArea("HTML code?");
        textArea.setName("HtmlTextArea");
        textArea.setSize(515, 660);
        textArea.setVisible(true);
        textArea.setEnabled(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    private JTextField getUrlTextField() {
        JTextField textField = new JTextField();
        textField.setSize(410, 20);
        textField.setName("UrlTextField");
        return textField;
    }

    private JButton getRunButton() {
        JButton button = new JButton("Get text!");
        button.setSize( 100, 25);
        button.setName("RunButton");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)  {
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
            }
        });
        return button;
    }
}