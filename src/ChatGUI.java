import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton disconnectButton;
    private JTextArea userListArea;

    private ChatClient chatClient;
    private UserListManager userListManager;

    public ChatGUI(String username) {
        setTitle("Chat " + username); // Uppdatera titeln med användarnamnet
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        add(chatScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        bottomPanel.add(messageField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        bottomPanel.add(sendButton, BorderLayout.EAST);

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });
        bottomPanel.add(disconnectButton, BorderLayout.WEST);

        add(bottomPanel, BorderLayout.SOUTH);

        userListArea = new JTextArea();
        userListArea.setEditable(false);
        JScrollPane userScrollPane = new JScrollPane(userListArea);
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        add(userScrollPane, BorderLayout.EAST);

        // Skapa rubrik för användarlistan och lägg till den till userScrollPane
        JLabel userListLabel = new JLabel("I chatten just nu:");
        userListLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userScrollPane.setColumnHeaderView(userListLabel);

        userListManager = new UserListManager(userListArea);

        chatClient = new ChatClient(username, chatArea, userListManager);

        setVisible(true);
    }

    private void sendMessage() {
        chatClient.sendMessage(messageField.getText());
        messageField.setText("");
    }

    private void disconnect() {
        chatClient.disconnect();
        System.exit(0);
    }
}
