package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.Base64;

public class Client extends JFrame {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private JTextArea chatArea;
    private JTextField messageField;
    private JLabel imageLabel;
    private JButton sendImageButton;
    private JFileChooser fileChooser;
    private byte[] buffer = new byte[1024];

    public Client() {
        // GUI setup
        setTitle("Client");
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

        imageLabel = new JLabel();
        add(imageLabel, BorderLayout.NORTH);

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
            // Client setup
            socket = new Socket("127.0.0.1", 3333);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
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
                                chatArea.append("Server: " + receivedMessage + "\n");
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
        chatArea.append("Client: " + message + "\n");
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

                    chatArea.append("Client: Sending JPG image '" + fileName + "'\n");

                    output.println("Image:" + encodedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                chatArea.append("Client: Please select a JPG image to send.\n");
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
                chatArea.append("Server: Received image '" + savedFile.getName() + "'\n");
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
            imageLabel.setIcon(new ImageIcon(image));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Client().setVisible(true);
            }
        });
    }
}