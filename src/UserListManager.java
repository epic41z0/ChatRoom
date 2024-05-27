import javax.swing.*;
import java.util.concurrent.*;

public class UserListManager {
    private JTextArea userListArea;
    private ConcurrentHashMap<String, Boolean> connectedUsers = new ConcurrentHashMap<>();

    public UserListManager(JTextArea userListArea) {
        this.userListArea = userListArea;
    }

    public void addUser(String username) {
        connectedUsers.put(username, true);
        updateUserList();
    }

    public void removeUser(String username) {
        connectedUsers.remove(username);
        updateUserList();
    }

    public void updateUsersFromMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            userListArea.setText("");
            String[] users = message.substring(9).split(",");
            for (String user : users) {
                if (!user.isEmpty()) {
                    userListArea.append(user.trim() + "\n");
                    connectedUsers.put(user.trim(), true);
                }
            }
        });
    }

    public String[] getUsers() {
        return connectedUsers.keySet().toArray(new String[0]);
    }

    private void updateUserList() {
        SwingUtilities.invokeLater(() -> {
            userListArea.setText("");
            for (String user : connectedUsers.keySet()) {
                userListArea.append(user + "\n");
            }
        });
    }
}
