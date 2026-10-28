package com.campuslab.desktop;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class CampusLabDesktop {
    private CampusLabDesktop() {}
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
            }
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
