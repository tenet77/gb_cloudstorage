package server;


import java.io.*;
import java.net.Socket;

@SuppressWarnings("FieldCanBeLocal")
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final String UPLOAD_CMD = "upload";
    private final String LIST_CMD = "list";
    private final String DOWNLOAD_CMD = "download";
    private final String REMOVE_CMD = "remove";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream is = new DataInputStream(socket.getInputStream());
             DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {

            byte[] buffer = new byte[256];
            while (true) {
                String command = is.readUTF();
                switch (command) {
                    case UPLOAD_CMD -> {
                        String fileName = is.readUTF();
                        long length = is.readLong();

                        File file = new File("server" + File.separator + fileName);
                        if (file.exists() || (!file.exists() && file.createNewFile())) {
                            FileOutputStream outputStream = new FileOutputStream(file);

                            int read;
                            for (int i = 0; i < ((length + 255) / 256); i++) {
                                read = is.read(buffer);
                                if (read == -1) break;
                                outputStream.write(buffer, 0, read);
                            }
                            outputStream.flush();
                            outputStream.close();

                            os.writeUTF("done");
                            os.flush();
                        }
                    }
                    case LIST_CMD -> {
                        File dir = new File("server");
                        File[] files = dir.listFiles();
                        if (files != null) {
                            os.writeUTF("list");
                            for (File file : files) {
                                os.writeUTF(file.getName());
                            }
                            os.writeUTF("~");
                            os.flush();
                        }
                    }
                    case DOWNLOAD_CMD -> {
                        String fileName = is.readUTF();
                        File file = new File("server" + File.separator + fileName);
                        if (!file.exists()) {
                            os.writeUTF("File is not exists");
                            os.flush();
                            break;
                        }
                        FileInputStream fis = new FileInputStream(file);
                        int read;
                        os.writeUTF("download");
                        os.writeUTF(fileName);
                        os.writeLong(file.length());
                        while ((read = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                        os.flush();
                    }
                    case REMOVE_CMD -> {
                        String fileName = is.readUTF();
                        File file = new File("server" + File.separator + fileName);
                        if (!file.exists()) {
                            os.writeUTF("File is not exists");
                            os.flush();
                            break;
                        }
                        if (file.delete()) {
                            os.writeUTF("File was removed");
                        } else {
                            os.writeUTF("Something wrong");
                        }
                        os.flush();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
