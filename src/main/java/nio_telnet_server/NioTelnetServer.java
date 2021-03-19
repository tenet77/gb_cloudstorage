package nio_telnet_server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NioTelnetServer {
    private final ByteBuffer buffer = ByteBuffer.allocate(512);
    private Path currentPath;

    private final String ROOT_DIR = "server";

    public static final String LS_COMMAND = "ls\t\t\t view all files from current directory\n\r";
    public static final String MKDIR_COMMAND = "mkdir\t\t\t view all files from current directory\n\r";
    public static final String TOUCH_COMMAND = "touch [file name]\t create file\n\r";
    public static final String CD_COMMAND = "cd\t\t\t change dir\n\r";
    public static final String RM_COMMAND = "rm [file name]\t\t remove file or dir\n\r";
    public static final String COPY_COMMAND = "copy [src] [dest]\t copy file or dir\n\r";
    public static final String CAT_COMMAND = "cat [file name]\t\t type content\n\r";

    public NioTelnetServer() throws IOException {

        currentPath = Path.of(ROOT_DIR);

        ServerSocketChannel server = ServerSocketChannel.open(); // открыли
        server.bind(new InetSocketAddress(8889));
        server.configureBlocking(false);
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (server.isOpen()) {
            selector.select();
            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key, Selector selector) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        int readBytes = channel.read(buffer);
        if (readBytes < 0) {
            channel.close();
            return;
        } else if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();

        if (key.isValid()) {
            String command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "");

            String[] commandArray = command.split(" ");

            if ("--help".equals(command)) {
                sendMessage(LS_COMMAND, selector);
                sendMessage(MKDIR_COMMAND, selector);
                sendMessage(TOUCH_COMMAND, selector);
                sendMessage(CD_COMMAND, selector);
                sendMessage(RM_COMMAND, selector);
                sendMessage(COPY_COMMAND, selector);
                sendMessage(RM_COMMAND, selector);
                sendMessage(CAT_COMMAND, selector);
            } else if ("ls".equals(command)) {
                sendMessage(getFilesList().concat("\n\r"), selector);
            } else if ("exit".equals(command)) {
                System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
                channel.close();
                return;
            } else if (commandArray.length > 2) {
                if ("copy".equals(commandArray[0])) {
                    sendMessage(copyFiles(commandArray[1], commandArray[2]).concat("\n\r"), selector);
                }
            } else if (commandArray.length > 1) {

                if ("touch".equals(commandArray[0])) {
                    sendMessage(touchFiles(commandArray[1]).concat("\n\r"), selector);
                } else if ("mkdir".equals(commandArray[0])) {
                    sendMessage(mkDir(commandArray[1]).concat("\n\r"), selector);
                } else if ("cd".equals(commandArray[0])) {
                    sendMessage(cdPath(commandArray[1]).concat("\n\r"), selector);
                } else if ("rm".equals(commandArray[0])) {
                    sendMessage(rmFiles(commandArray[1]).concat("\n\r"), selector);
                } else if ("cat".equals(commandArray[0])) {
                    sendMessage(catFiles(commandArray[1], selector).concat("\n\r"), selector);
                }

            }

        }
        sendName(channel);
    }

    private String copyFiles(String fileSrc, String fileDst) {
        Path pathSrc = Path.of(currentPath.toString() + File.separator + fileSrc);
        Path pathDst = Path.of(currentPath.toString() + File.separator + fileDst);
        if (Files.notExists(pathSrc)) return "File doesn't exists";
        try {
            Files.copy(pathSrc, pathDst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return "Something wrong";
        }

        return "Successfully";
    }

    private String catFiles(String fileName, Selector selector) {
        Path filePath = Path.of(currentPath.toString() + File.separator + fileName);
        if (Files.notExists(filePath) || Files.isDirectory(filePath)) return "File doesn't exists";
        try {
            Files.lines(filePath).forEach(line -> {
                try {
                    sendMessage(line + "\n\r", selector);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            return "Something wrong";
        }

        return "";
    }

    private String rmFiles(String fileName) {
        Path filePath = Path.of(currentPath.toString() + File.separator + fileName);
        if (Files.notExists(filePath)) return "File doesn't exists";
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            return "Something wrong";
        }

        return "Deletes successfully";
    }

    private String cdPath(String newPathName) {
        Path newPath = Path.of(ROOT_DIR + File.separator + newPathName);
        if (Files.notExists(newPath)) return "Directory doesn't exists";
        currentPath = newPath;
        return "";
    }

    private String mkDir(String dirName) {

        Path dirPath = Path.of(currentPath.toString() + File.separator + dirName);
        if (Files.exists(dirPath)) return "Directory already exists";
        try {
            Files.createDirectory(dirPath);
        } catch (IOException e) {
            return "Something wrong";
        }

        return "Directory created successfully";
    }

    private String touchFiles(String fileName) {
        Path filePath = Path.of(currentPath.toString() + File.separator + fileName);
        if (Files.exists(filePath)) return "File already exists";

        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            return "Something wrong";
        }

        return "File created successfully";
    }

    private void sendName(SocketChannel channel) throws IOException {
        channel.write(
                ByteBuffer.wrap(channel
                        .getRemoteAddress().toString()
                        .concat(" / " + currentPath.toString())
                        .concat(">: ")
                        .getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    private String getFilesList() throws IOException {
        StringBuilder fileList = new StringBuilder();
        Files.list(currentPath).forEach(name -> fileList.append(name).append("\n\r"));
        return fileList.toString();
    }

    private void sendMessage(String message, Selector selector) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                ((SocketChannel) key.channel())
                        .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "some attach");
        channel.write(ByteBuffer.wrap("Hello user!\n\r".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n\r".getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}