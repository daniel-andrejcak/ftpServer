import java.io.*;
import java.net.*;
import java.util.Arrays;

public class FTPServer {
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java SimpleFTPServer <port> <base_directory>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String baseDirectory = args[1];

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                // Handle each client in a separate thread
                Thread clientThread = new Thread(() -> handleClient(clientSocket, baseDirectory));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket, String baseDirectory) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String request = reader.readLine();
            if (request != null) {
                String[] tokens = request.split(" ", 2);
                String command = tokens[0];
                String path = tokens.length > 1 ? tokens[1] : "";
                path = baseDirectory + path;
                // Concatenate paths and check for traversal attempts
                /*String fullPath = new File(baseDirectory, path).getCanonicalPath();
                if (!fullPath.startsWith(baseDirectory)) {
                    writer.write("bad request\n");
                    writer.flush();
                    return;
                }*/

                switch (command) {
                    case "get":
                        sendFile(path, writer);
                        break;
                    case "list":
                        listDirectory(path, writer);
                        break;
                    case "tree":
                        listTree(path, writer, "");
                        break;
                    default:
                        writer.write("bad request\n");
                        writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendFile(String filePath, BufferedWriter writer) throws IOException {
        try (BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fileStream.read(buffer)) != -1) {
                writer.write(new String(Arrays.copyOf(buffer, bytesRead), "UTF-8"));
            }

            writer.flush();
        } catch (FileNotFoundException e) {
            writer.write("bad request\n");
            writer.flush();
        }
    }

    private static void listDirectory(String directoryPath, BufferedWriter writer) throws IOException {
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] contents = directory.listFiles();
            if (contents != null) {
                for (File item : contents) {
                    String itemType = item.isDirectory() ? "d" : "f";
                    writer.write(String.format("%s %s %d\n", itemType, item.getName(), item.length()));
                }
            }
        }

        writer.flush();
    }

    private static void listTree(String directoryPath, BufferedWriter writer, String prefix) throws IOException {
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] contents = directory.listFiles();
            if (contents != null) {
                for (File item : contents) {
                    String itemType = item.isDirectory() ? "d" : "f";
                    writer.write(String.format("%s%s\n", prefix, item.getName()));

                    if (item.isDirectory()) {
                        listTree(item.getAbsolutePath(), writer, prefix + "    ");
                    }
                }
            }
        }

        writer.flush();
    }
}
