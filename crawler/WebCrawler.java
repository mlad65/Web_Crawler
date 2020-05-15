package crawler;

import javax.swing.*;
import java.awt.*;

public class WebCrawler extends JFrame {
    public WebCrawler() {
        super("Web Crawler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setVisible(true);
        setLayout(null);

        JTextArea textArea = new JTextArea("HTML code?");
        textArea.setName("TextArea");
        textArea.setBounds(25, 25, 250, 250);
        textArea.setVisible(true);
        textArea.setEnabled(false);
        add(textArea);
    }
}