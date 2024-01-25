import java.io.*;
import java.net.*;
import java.util.Arrays;

public class FTPServer {
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String baseDirectory = args[1];

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Request from " + clientSocket.getInetAddress());

                //kazdy client je v separate thread a pre kazdeho sa spusti novy socket
                Thread clientThread = new Thread(() -> handleClient(clientSocket, baseDirectory));
                clientThread.start();
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    //reader precita to co socket dostal, writer napise to co ma socket poslat naspat
    private static void handleClient(Socket clientSocket, String baseDirectory) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String request = reader.readLine();

            if (request != null) {
                String[] tokens = request.split(" ");
                String command = tokens[0];

                String path;

                if (tokens.length > 1) {
                    //zbavi sa spaces ktore su v path
                    path = String.join("", Arrays.copyOfRange(tokens, 1, tokens.length));
                }
                else{
                    path = "";
                }

                path = baseDirectory + path;

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
        //try osetri, ked client chce nieco ine ako get file (napriklad get dir je bad request)
        try (BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            //cita subor po chunkoch velkosti BUFFER_SIZE, writer ich zapise
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                writer.write(new String(Arrays.copyOf(buffer, bytesRead), "UTF-8"));
            }

            writer.flush();
        } 
        catch (FileNotFoundException e) {
            writer.write("bad request\n");
            writer.flush();
        }
    }

    private static void listDirectory(String directoryPath, BufferedWriter writer) throws IOException {
        File directory = new File(directoryPath);

        //kontrola ci taky dir existuje
        if (directory.exists() && directory.isDirectory()) {

            //.listFiles() vrati array suborov a dir, ktore sa nachadzaju v 'directory'
            File[] contents = directory.listFiles();
            if (contents != null) {

                for (File item : contents) {
                    if (item.isDirectory()) {
                        writer.write(String.format("%s %s\n", "d", item.getName()));
                    }
                    else {
                        writer.write(String.format("%s %s %d\n", "f", item.getName(), item.length()));
                    }
                }
            }
        }
        else{
            writer.write("bad request\n");
        }

        writer.flush();
    }

    //rovnaky postup ako pri listDirectory, az na to, ze ak narazi na dir, tak rekurzivne ide do neho a vypise aj jeho obsah
    //prefix su len whitespaces pri vypise, aby bolo vidiet, ze co patri pod co
    private static void listTree(String directoryPath, BufferedWriter writer, String prefix) throws IOException {
        File directory = new File(directoryPath);
        
        if (directory.exists() && directory.isDirectory()) {
            File[] contents = directory.listFiles();
            
            if (contents != null) {

                for (File item : contents) {
                    writer.write(String.format("%s%s\n", prefix, item.getName()));

                    if (item.isDirectory()) {
                        listTree(item.getPath(), writer, prefix + "    ");
                    }
                }
            }
        }
        else{
            writer.write("bad request\n");
        }

        writer.flush();
    }
}
