// Group 2 ChenGong ZhangZhao LiangYizhuo
package com.bigcomp.accesscontrol;

import com.bigcomp.accesscontrol.gui.MainWindow;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Enumeration;
import java.util.List;

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
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored2) {
                try {
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                } catch (Exception ignored3) {
                }
            }
        }

        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("TabbedPane.tabInsets", new javax.swing.plaf.InsetsUIResource(10, 12, 10, 12));
        UIManager.put("TabbedPane.contentBorderInsets", new javax.swing.plaf.InsetsUIResource(8, 8, 8, 8));
        UIManager.put("Table.rowHeight", 28);

        Font baseFont = pickBestUiFont();
        FontUIResource base = new FontUIResource(baseFont);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, base);
            }
        }

        UIManager.put("OptionPane.messageFont", base);
        UIManager.put("OptionPane.buttonFont", base);
    }

    private static Font pickBestUiFont() {
        int size = 13;
        List<String> preferred = List.of(
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "SimSun",
            "NSimSun",
            "Noto Sans CJK SC",
            "Noto Sans CJK TC",
            "PingFang SC",
            "Heiti SC",
            "Arial Unicode MS",
            "Segoe UI",
            "SansSerif"
        );

        String sample = "中文示例ABC123";
        for (String name : preferred) {
            Font f = new Font(name, Font.PLAIN, size);
            if (canDisplayAll(f, sample)) {
                return f;
            }
        }

        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String family : families) {
            Font f = new Font(family, Font.PLAIN, size);
            if (canDisplayAll(f, sample)) {
                return f;
            }
        }

        return new Font("SansSerif", Font.PLAIN, size);
    }

    private static boolean canDisplayAll(Font font, String text) {
        if (font == null || text == null) {
            return false;
        }
        return font.canDisplayUpTo(text) == -1;
    }
}

