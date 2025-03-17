import java.util.Scanner;
import java.io.*;
import java.net.*;

/**
* Grupo 9
* @author Duarte Carvalho fc59801 (33.3%)
* @author Rodrigo Freitas fc59868 (33.3%)
* @author Tiago Lourenço fc59877 (33.3%)
*/
public class mySharingClient {

    static int SIZE = 1024;
    private static Socket socket = null;
    private static ObjectOutputStream outStream ;
    private static ObjectInputStream inStream;
    private static Scanner sc = null;
    private static final String MENU = "\nComandos disponíveis:\nCREATE <ws>\nADD <user> <ws>\nUP <ws> <file1> ... <filen>\n" +
                                        "DW <ws> <file1> ... <filen>\nRM <ws> <file1> ... <filen>\nLW\nLS <ws>\nDigite um comando: ";

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Uso: java mySharingClient <serverAddress> <user-id> <password>");
            return;
        }
        
        String serverAddress = args[0];
        String userId = args[1];
        //fzr validacao input
        if (!userId.matches("^[a-zA-Z0-9]+$")) {
            System.out.println("Erro: username deve conter apenas caracteres alfanuméricos.");
            return;
        }

        String password = args[2]; // nao deixa por password vazia (entra no primeiro if, pois args.length == 2)
        
        String[] addressParts = serverAddress.split(":");
        String host = addressParts[0];
        int port = (addressParts.length > 1) ? Integer.parseInt(addressParts[1]) : 12345;
        

        //Lidar com ctrl+c
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nEncerrando cliente...");
            try {
                socket.close();
            } catch (IOException e) {
                
                e.printStackTrace();
            }
            return;
        }));



        try {
            
            socket = new Socket(host, port);
            outStream = new ObjectOutputStream(socket.getOutputStream());
            inStream = new ObjectInputStream(socket.getInputStream());
                        
            clientSend(outStream, userId);
            clientSend(outStream, password);
            System.out.println("Enviando credenciais...");
            
            String authResponse = (String) inStream.readObject();
            if (authResponse.equals("WRONG-PWD")) {
                System.out.println("WRONG-PWD (palavra passe incorreta)");
                return;
            } else if (authResponse.equals("OK-NEW-USER")) {
                System.out.println("OK-NEW-USER (novo utilizador criado)");
            } else if (authResponse.equals("OK-USER")) {
                System.out.println("OK-USER (autenticação bem-sucedida)");
            }

            sc = new Scanner(System.in);
            while (true) {
                System.out.print(MENU);
                String command = sc.nextLine();
                clientSend(outStream, command);
                String[] parts = command.split(" ");
                if(command.startsWith("UP")){
                    handleUpload(parts);
                }
                else if(command.startsWith("DW")){
                    handleDownload(parts);
                }
                else{
                    String response = (String) inStream.readObject();
                    System.out.println("Resposta: " + response);
                }
            }

        }
        catch (java.util.NoSuchElementException e) {
            System.out.println("Cliente desconectado (Ctrl+C).");
            return;
        }
         catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeResources(outStream, inStream, socket, sc);
        }
    }

    private static synchronized void handleUpload(String[] parts) throws IOException, ClassNotFoundException {
        String serverResponse = (String) inStream.readObject();
        System.out.println("Resposta: " + serverResponse);
        if (serverResponse.equals("NOWS") || serverResponse.equals("NOPERM") || serverResponse.equals("Uso: UP <ws> <file1> ... <filen>")) {
            return;
        }
        
        for (int i = 2; i < parts.length; i++) {
            File file = new File(parts[i]);
            if (!file.exists()) {
                clientSend(outStream, "NAO_EXISTE");
                clientSend(outStream, parts[i]);
            }
            else{
                clientSend(outStream, parts[i]);
                clientSendFile(outStream, file);
            }
        }

        
        String resposta2 = (String) inStream.readObject();
        System.out.println(resposta2);
    }

    private static synchronized void handleDownload(String[] parts) throws IOException, ClassNotFoundException {
        String serverResponse = (String) inStream.readObject();
        System.out.println("Resposta: " + serverResponse);
    
        if (serverResponse.equals("NOWS") || serverResponse.equals("NOPERM") || serverResponse.equals("Uso: DW <ws> <file1> ... <filen>")) {
            return;
        }
    
        for (int i = 2; i < parts.length; i++) {
            String fileName = parts[i];
            String fileResponse = (String) inStream.readObject();
            
            if (fileResponse.equals("NAO_EXISTE")) {
                continue;
            } else if (fileResponse.equals("OK")) {
                clientReceiveFile(inStream, new File(fileName + "_downloaded"));
            }
        }
        
        String resposta2 = (String) inStream.readObject(); 
        System.out.println(resposta2);
    }


    private static synchronized void clientSend(ObjectOutputStream outStream, String message) throws IOException {
        outStream.writeObject(message);
        outStream.flush();
    }

    private static synchronized void clientSendFile(ObjectOutputStream outStream, File file) throws IOException {
        long fileSize = file.length();
        outStream.writeLong(fileSize);
        outStream.flush();

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream input = new BufferedInputStream(fis)) {

            byte[] buffer = new byte[SIZE];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) > 0) {
                outStream.write(buffer, 0, bytesRead);
                outStream.flush();
            }
        }
    }

    private static synchronized void clientReceiveFile(ObjectInputStream inStream, File file) throws IOException {
        long fileSize = inStream.readLong();
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream output = new BufferedOutputStream(fos)) {

            byte[] buffer = new byte[SIZE];
            long totalReceived = 0;
            int bytesRead;

            while (totalReceived < fileSize && (bytesRead = inStream.read(buffer, 0, SIZE)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
                totalReceived += bytesRead;
            }
        }
    }

    private static synchronized void closeResources(ObjectOutputStream outStream, ObjectInputStream inStream, Socket socket, Scanner sc) {
        try {
            if (outStream != null) outStream.close();
            if (inStream != null) inStream.close();
            if (socket != null) socket.close();
            if (sc != null) sc.close();
            System.out.println("Recursos fechados com sucesso.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}