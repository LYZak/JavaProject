package com.bigcomp.accesscontrol;

import com.bigcomp.accesscontrol.gui.MainWindow;
import javax.swing.SwingUtilities;

/**
 * 主程序入口
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MainWindow().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

