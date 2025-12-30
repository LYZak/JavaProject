// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol;

import com.bigcomp.accesscontrol.gui.MainWindow;
import javax.swing.SwingUtilities;

/**
 * Main program entry point
 */
//zz张照
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

