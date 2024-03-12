package server;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.Base64;

public class Server extends JFrame {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendImageButton;
    private JFileChooser fileChooser;

    public Server() {
        // GUI setup
        setTitle("Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        add(messageField, BorderLayout.SOUTH);

        sendImageButton = new JButton("Send Image");
        sendImageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendImage();
            }
        });
        add(sendImageButton, BorderLayout.NORTH);

        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        try {
            // Server setup
            serverSocket = new ServerSocket(3333);
            clientSocket = serverSocket.accept();
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Message receiving thread
        Thread messageReceiver = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        String receivedMessage = input.readLine();
                        if (receivedMessage != null) {
                            if (receivedMessage.startsWith("Image:")) {
                                // Handle image received
                                String encodedImage = receivedMessage.substring(6);
                                receiveImage(encodedImage);
                            } else {
                                chatArea.append("Client: " + receivedMessage + "\n");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        messageReceiver.start();
    }

    // Send a text message
    private void sendMessage() {
        String message = messageField.getText();
        chatArea.append("Server: " + message + "\n");
        output.println(message);
        messageField.setText("");
    }

    // Send an image
    private void sendImage() {
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName();

            if (fileName.toLowerCase().endsWith(".jpg")) {
                try {
                    FileInputStream fis = new FileInputStream(selectedFile);
                    byte[] buffer = new byte[(int) selectedFile.length()];
                    fis.read(buffer);
                    fis.close();

                    String encodedImage = Base64.getEncoder().encodeToString(buffer);

                    chatArea.append("Server: Sending JPG image '" + fileName + "'\n");

                    output.println("Image:" + encodedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                chatArea.append("Server: Please select a JPG image to send.\n");
            }
        }
    }

    // Receive and save an image
    private void receiveImage(String encodedImage) {
        try {
            byte[] decodedImage = Base64.getDecoder().decode(encodedImage);

            JFileChooser imageFileChooser = new JFileChooser();
            int result = imageFileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File savedFile = imageFileChooser.getSelectedFile();
                FileOutputStream fos = new FileOutputStream(savedFile);
                fos.write(decodedImage);
                fos.close();
                chatArea.append("Client: Received image '" + savedFile.getName() + "'\n");
                displayImage(savedFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Display an image
    private void displayImage(File imageFile) {
        try {
            Image image = ImageIO.read(imageFile);
            AbstractButton imageLabel = null; // (Assuming this was supposed to be a JLabel)
            imageLabel.setIcon(new ImageIcon(image));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Server().setVisible(true);
            }
        });
    }
}
