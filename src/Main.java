import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String username = JOptionPane.showInputDialog("Enter your username:");
            if (username == null || username.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Username cannot be empty. Exiting.");
                System.exit(0);
            }
            new ChatGUI(username);
        });
    }
}
