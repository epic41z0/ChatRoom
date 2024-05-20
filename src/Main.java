import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Main extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton disconnectButton;
    private String username; // Användarnamn
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    private MulticastSocket multicastSocket;
    private InetAddress multicastGroup;
    private int multicastPort = 12345;

    public Main() {
        // Visa dialogruta för att ange användarnamn
        username = JOptionPane.showInputDialog("Enter your username:");
        if (username == null || username.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Username cannot be empty. Exiting.");
            System.exit(0);
        }

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

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        add(userScrollPane, BorderLayout.EAST);

        // Skapa rubrik för användarlistan och lägg till den till userScrollPane
        JLabel userListLabel = new JLabel("I chatten just nu:");
        userListLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userScrollPane.setColumnHeaderView(userListLabel);

        try {
            multicastSocket = new MulticastSocket(multicastPort);
            multicastGroup = InetAddress.getByName("230.0.0.0");
            multicastSocket.joinGroup(multicastGroup);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread receiveThread = new Thread(() -> {
            while (true) {
                receiveMessage();
            }
        });
        receiveThread.start();

        // Meddela när användaren kopplar upp sig
        sendSystemMessage(username + " UPPKOPPLAD.\n");

        setVisible(true);
    }

    private void sendMessage() {
        try {
            String message = username + ": " + messageField.getText();
            message = message + "\n";
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, multicastPort);
            multicastSocket.send(packet);
            messageField.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessage() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            multicastSocket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength());
            if (message.contains("har anslutit till chatten") || message.contains("har kopplat ner från chatten")) {
                String username = message.split(" ")[0]; // Extrahera användarnamnet från meddelandet
                if (!userListModel.contains(username)) {
                    userListModel.addElement(username);
                } else {
                    userListModel.removeElement(username);
                }
            }
            chatArea.append(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            // Meddela när användaren kopplar ner
            sendSystemMessage(username + " har kopplat ner från chatten.");

            multicastSocket.leaveGroup(multicastGroup);
            multicastSocket.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Funktion för att skicka systemmeddelanden (användaranslutning/användarfrånkoppling)
    private void sendSystemMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, multicastPort);
            multicastSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main());
    }
}
