import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatClient {
    private String username;
    private JTextArea chatArea;
    private MulticastSocket multicastSocket;
    private InetAddress multicastGroup;
    private int multicastPort = 12345;
    private UserListManager userListManager;

    private ExecutorService executor = Executors.newFixedThreadPool(2);

    public ChatClient(String username, JTextArea chatArea, UserListManager userListManager) {
        this.username = username;
        this.chatArea = chatArea;
        this.userListManager = userListManager;

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
        userListManager.addUser(username);
    }

    public void sendMessage(String message) {
        try {
            message = username + ": " + message + "\n";
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, multicastPort);
            multicastSocket.send(packet);
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
                    userListManager.updateUsersFromMessage(message);
                } else {
                    SwingUtilities.invokeLater(() -> chatArea.append(message));
                    if (message.endsWith("har anslutit till chatten.\n")) {
                        String newUsername = message.split(" ")[0];
                        userListManager.addUser(newUsername);
                    } else if (message.endsWith("har kopplat ner från chatten.\n")) {
                        String disconnectedUsername = message.split(" ")[0];
                        userListManager.removeUser(disconnectedUsername);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            // Meddela när användaren kopplar ner
            sendSystemMessage(username + " har kopplat ner från chatten.\n");

            multicastSocket.leaveGroup(multicastGroup);
            multicastSocket.close();
            userListManager.removeUser(username);
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

    // Skicka användarlistan periodiskt
    private void sendUserListPeriodically() {
        try {
            while (true) {
                StringBuilder userListMessage = new StringBuilder("USERLIST:");
                for (String user : userListManager.getUsers()) {
                    userListMessage.append(user).append(",");
                }
                sendSystemMessage(userListMessage.toString() + "\n");
                Thread.sleep(5000); // Vänta 5 sekunder innan nästa uppdatering
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
