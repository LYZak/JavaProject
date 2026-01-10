// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol;

import com.bigcomp.accesscontrol.gui.MainWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.Enumeration;

/**
 * Main program entry point
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                applyGlobalUiStyle();
                new MainWindow().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void applyGlobalUiStyle() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored2) {
            }
        }

        Font baseFont = new Font("Segoe UI", Font.PLAIN, 13);
        FontUIResource base = new FontUIResource(baseFont);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, base);
            }
        }

        UIManager.put("TabbedPane.tabInsets", new javax.swing.plaf.InsetsUIResource(10, 12, 10, 12));
        UIManager.put("TabbedPane.contentBorderInsets", new javax.swing.plaf.InsetsUIResource(8, 8, 8, 8));
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("OptionPane.messageFont", base);
        UIManager.put("OptionPane.buttonFont", base);
    }
}

