package client;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionService {

    private DataOutputStream os;
    private DataInputStream is;
    private final ClientFormController formController;

    private final String LIST_CMD = "list";
    private final String DOWNLOAD_CMD = "download";

    public ConnectionService(ClientFormController formController) {
        this.formController = formController;
    }

    public String upload(String fileName) {
        if (!isAlive()) return "Not connected";

        try {

            File file = new File("client" + File.separator + fileName);
            if (!file.exists()) return "File is not exists";

            FileInputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[256];
            int read;
            os.writeUTF("upload");
            os.writeUTF(fileName);
            os.writeLong(file.length());
            while ((read = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return "Something wrong";
        }

        return "Send success";
    }

    public boolean isAlive() {
        return (is != null);
    }

    public String openConnection() {

        if (isAlive()) return "Already connected";

        try {

            String SERVER = "localhost";
            int PORT = 8888;
            Socket socket = new Socket(SERVER, PORT);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            Thread readingThread = new Thread(() -> {

                while (true) {
                    try {
                        String message = is.readUTF();
                        switch (message) {
                            case LIST_CMD -> {
                                List<String> result = new ArrayList<>();
                                String fileName;
                                while (!(fileName = is.readUTF()).equals("~")) {
                                    result.add(fileName);
                                }
                                Platform.runLater(() -> formController.updateListOfFilesOnServer(result));
                            }
                            case DOWNLOAD_CMD -> {
                                String fileName = is.readUTF();
                                long length = is.readLong();

                                File file = new File("client" + File.separator + fileName);
                                if (file.exists() || (!file.exists() && file.createNewFile())) {
                                    FileOutputStream outputStream = new FileOutputStream(file);

                                    byte[] buffer = new byte[256];
                                    int read;
                                    for (int i = 0; i < ((length + 255) / 256); i++) {
                                        read = is.read(buffer);
                                        if (read == -1) break;
                                        outputStream.write(buffer, 0, read);
                                    }
                                    outputStream.flush();
                                    outputStream.close();

                                    Platform.runLater(formController::updateClientFiles);
                                }
                            }
                            default -> Platform.runLater(() -> formController.showMessage(message));
                        }
                    } catch (Exception e) {
                        is = null;
                        os = null;
                        e.printStackTrace();
                        break;
                    }
                }
            });

            readingThread.setDaemon(true);
            readingThread.start();

            return "Connected";

        } catch (Exception e) {
            return "Something wrong";
        }
    }

    public String download(String item) {
        if (!isAlive()) return "Not connected";

        try {

            os.writeUTF("download");
            os.writeUTF(item);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return "Something wrong";
        }

        return "Send success";
    }

    public String listOfFiles() {

        if (!isAlive()) {
            return "Not connected";
        }

        try {
            os.writeUTF("list");
            os.flush();
        } catch (IOException e) {
            return "Something wrong";
        }

        return "Send success";

    }

    public String remove(String item) {

        if (!isAlive()) {
            return "Not connected";
        }

        try {
            os.writeUTF("remove");
            os.writeUTF(item);
            os.flush();
        } catch (IOException e) {
            return "Something wrong";
        }

        return "Send success";
    }
}
