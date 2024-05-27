import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Main extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton disconnectButton;
    private String username; // Användarnamn
    private JTextArea userListArea;

    private MulticastSocket multicastSocket;
    private InetAddress multicastGroup;
    private int multicastPort = 12345;

    // Använd ConcurrentHashMap för att hantera anslutna användare
    private static ConcurrentHashMap<String, Boolean> connectedUsers = new ConcurrentHashMap<>();
    private ExecutorService executor = Executors.newFixedThreadPool(2);

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

        userListArea = new JTextArea();
        userListArea.setEditable(false);
        JScrollPane userScrollPane = new JScrollPane(userListArea);
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

        // Starta mottagningstråden
        executor.execute(this::receiveMessages);

        // Starta tråd för att periodiskt skicka användarlistan
        executor.execute(this::sendUserListPeriodically);

        // Meddela när användaren kopplar upp sig
        sendSystemMessage(username + " har anslutit till chatten.\n");

        // Lägg till den nya användaren i listan
        connectedUsers.put(username, true);
        updateConnectedUsers();

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

    private void receiveMessages() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                if (message.startsWith("USERLIST:")) {
                    updateConnectedUsersFromMessage(message);
                } else {
                    SwingUtilities.invokeLater(() -> chatArea.append(message));
                    if (message.endsWith("har anslutit till chatten.\n")) {
                        String newUsername = message.split(" ")[0];
                        if (!connectedUsers.containsKey(newUsername)) {
                            connectedUsers.put(newUsername, true);
                            updateConnectedUsers();
                        }
                    } else if (message.endsWith("har kopplat ner från chatten.\n")) {
                        String disconnectedUsername = message.split(" ")[0];
                        connectedUsers.remove(disconnectedUsername);
                        updateConnectedUsers();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            // Meddela när användaren kopplar ner
            sendSystemMessage(username + " har kopplat ner från chatten.\n");

            multicastSocket.leaveGroup(multicastGroup);
            multicastSocket.close();
            connectedUsers.remove(username);
            updateConnectedUsers();
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

    // Uppdatera användarlistan i UI
    private void updateConnectedUsers() {
        SwingUtilities.invokeLater(() -> {
            userListArea.setText("");
            for (String user : connectedUsers.keySet()) {
                userListArea.append(user + "\n");
            }
        });
    }

    // Uppdatera användarlistan baserat på mottaget meddelande
    private void updateConnectedUsersFromMessage(String message) {
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

    // Skicka användarlistan periodiskt
    private void sendUserListPeriodically() {
        try {
            while (true) {
                StringBuilder userListMessage = new StringBuilder("USERLIST:");
                for (String user : connectedUsers.keySet()) {
                    userListMessage.append(user).append(",");
                }
                sendSystemMessage(userListMessage.toString() + "\n");
                Thread.sleep(5000); // Vänta 5 sekunder innan nästa uppdatering
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
