import java.io.*;
import java.net.*;
import java.util.*;

/**                                                           
 * Grupo 9
 * @author Duarte Carvalho fc59801 (33.3%)  
 * @author Rodrigo Freitas fc59868 (33.3%)
 * @author Tiago Lourenço fc59877 (33.3%)
 */
public class mySharingServer {

    static int SIZE = 1024;
    static String USERS_FILE = "users.txt";
    static String WORKSPACES_FILE = "workspaces.txt";
    private ServerSocket sSoc;
    private static List<Workspace> workspaces;

    public static void main(String[] args) {
        if (args.length > 1) {
            System.out.println("Uso: java mySharingServer [port]");
            return;
        }
        int port = (args.length == 1) ? Integer.parseInt(args[0]) : 12345;
        System.out.println("Executando o servidor...");
        loadWorkspaces(); //carregar as workspaces do ficheiro para a variavel "workpaces"
        mySharingServer server = new mySharingServer();
        server.startServer(port);
    }

    private static synchronized void loadWorkspaces() {
        File file = new File(WORKSPACES_FILE);
        workspaces = new ArrayList<>();
    
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":");
                        String name = parts[0].trim();
                        String owner = parts[1].trim();
                        
                        // linha dos users
                        String usersLine = br.readLine();
                        List<String> users = new ArrayList<>(Arrays.asList(usersLine.substring(2).split(",")));

                        workspaces.add(new Workspace(name, owner, users));
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void startServer(int port) {

        //Lidar com ctrl+c
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (sSoc != null) {
                    sSoc.close();
                    System.out.println("\nEncerrando o servidor ....");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        
        try {
            sSoc = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
            System.exit(1);
        }

        while (true) {
            try {
                Socket inSoc = sSoc.accept();
                ServerThread newServerThread = new ServerThread(inSoc);
                newServerThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ServerThread extends Thread {

        private Socket socket;

        ServerThread(Socket inSoc) {
            socket = inSoc;
        }

        public void run() {
            try {
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                String user = (String) inStream.readObject();
                String password = (String) inStream.readObject();
                System.out.println("Recebido user: " + user + ", password: " + password);

                String response = authenticateUser(user, password);
                outStream.writeObject(response);
                outStream.flush();

                if (response.equals("WRONG-PWD")) {
                    System.out.println("Autenticação falhou!");
                    closeResources(outStream, inStream, socket);
                    return;
                }

                while (true) {
                    String command = (String) inStream.readObject();
                    response = processCommand(inStream,outStream,user, command);
                    outStream.writeObject(response);
                    outStream.flush();
                }
            }
            catch (EOFException e) {
                    System.out.println("Cliente desconectado (Ctrl+C).");
                    
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private String processCommand(ObjectInputStream inStream,ObjectOutputStream outStream, String user, String command) throws ClassNotFoundException, IOException {
            String[] parts = command.split(" ");
            String action = parts[0];

            switch (action) {
                case "CREATE": 
                    if (parts.length != 2 ) return "Uso: CREATE <nome_workspace>";
                    return createWorkspace(user, parts[1]);
                case "ADD": 
                    if (parts.length != 3 ) return "Uso: ADD <user> <ws>";
                    return addUser(user,parts[1],parts[2]);
                case "LW": 
                    if (parts.length != 1 ) return "Uso: LW";
                    return mostraWorkspaces(user);
                case "UP":
                    if (parts.length < 3 ) return "Uso: UP <ws> <file1> ... <filen>";
                    return handleUpload(inStream, outStream, user, parts[1],parts);
                case "DW":
                    if (parts.length < 3 ) return "Uso: DW <ws> <file1> ... <filen>";
                    return handleDownload(inStream, outStream, user, parts[1],parts);
                case "RM":
                    if (parts.length < 3 ) return "Uso: RM <ws> <file1> ... <filen>";
                    return handleRemove(user, parts[1],parts); 
                case "LS":
                    if (parts.length != 2 ) return "Uso: LS <ws>";
                    return handleLS(user, parts[1],parts); 
                default:
                    return "Comando inválido";
            }
        }

        private synchronized String handleUpload(ObjectInputStream inStream,ObjectOutputStream outStream, String user, String ws,String[] parts) throws IOException, ClassNotFoundException {
            Workspace workspace = getWorkspace(ws);
            if (workspace == null) return "NOWS";
            if (!workspace.getUsers().contains(user)) return "NOPERM";

            outStream.writeObject("");
            outStream.flush();
            StringBuilder res = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                String fileName = (String) inStream.readObject();
                if (fileName.equals("NAO_EXISTE")){
                    String ficheiroInexistente = (String) inStream.readObject();
                    res.append(ficheiroInexistente + ": nao existe\n");
                    continue;
                }
                else{
                    File file = new File(ws + "/" + fileName);
                    serverReceiveFile(inStream, file);
                    res.append(fileName + ": OK\n");
                }
            }
            return res.toString();
        }

        private synchronized String handleDownload(ObjectInputStream inStream,ObjectOutputStream outStream, String user, String ws,String[] parts) throws IOException, ClassNotFoundException {
            Workspace workspace = getWorkspace(ws);
            if (workspace == null) return "NOWS";
            if (!workspace.getUsers().contains(user)) return "NOPERM";

            outStream.writeObject("");
            outStream.flush();

            StringBuilder res = new StringBuilder();

            for (int i = 2; i < parts.length; i++) {
                String fileName = parts[i];
                File file = new File(ws + "/" + fileName);

                if (!file.exists()) {
                    outStream.writeObject("NAO_EXISTE");
                    res.append(fileName + ": Não existe no workspace indicado\n");
                } else {
                    outStream.writeObject("OK");
                    serverSendFile(outStream, file);
                    res.append(fileName + ": #ficheiro transferido\n");
                }
                outStream.flush();
            }

            return res.toString();
        }
        
        private synchronized String handleRemove(String user, String ws,String[] parts) throws IOException, ClassNotFoundException {
            Workspace workspace = getWorkspace(ws);
            if (workspace == null) return "NOWS";
            if (!workspace.getUsers().contains(user)) return "NOPERM";

            StringBuilder response = new StringBuilder("\n");

            for (int i = 2; i < parts.length; i++) {
                String fileName = parts[i];
                File file = new File(ws + "/" + fileName);

                if (!file.exists()) {
                    response.append("O ficheiro ").append(fileName).append(" não existe no workspace indicado\n");
                } else {
                    if (file.delete()) {
                        response.append(fileName).append(": APAGADO\n");
                    } else {
                        response.append("Erro ao apagar ").append(fileName).append("\n");
                    }
                }
                
            }

            return response.toString();
        }

        private synchronized String handleLS(String user, String ws,String[] parts) throws IOException, ClassNotFoundException {
            Workspace workspace = getWorkspace(ws);
            if (workspace == null) return "NOWS";
            if (!workspace.getUsers().contains(user)) return "NOPERM";

            File workspaceDir = new File(ws);
            File[] files = workspaceDir.listFiles();
        
            StringBuilder response = new StringBuilder("\n");
            response.append("{ ");

            for (File file : files) {
                if (file.isFile()) {
                    response.append(file.getName()).append(", ");
                }
            }
            if(response.indexOf(",") != -1){ // se tiver virgula
                response.replace(response.length()-2, response.length() -1 , ""); //tira ultima virgula
            }
            
            response.append("}");
            return response.toString();
        }


        private synchronized String mostraWorkspaces(String user){
            StringBuilder sb = new StringBuilder("{ ");
            for (Workspace ws:workspaces){
                if(ws.getUsers().contains(user)) sb.append(ws.getName()+", ");
            }

            sb.replace(sb.length()-2, sb.length() -1 , ""); // tira ultima virgula

            sb.append("}");
            return sb.toString();
        }

        private synchronized String addUser(String userLogado, String user, String workspace_name){
            if (!this.workspaceExists(workspace_name)) {
                return "NOWS";
            }
            Workspace ws= getWorkspace(workspace_name);
            if (!ws.getOwner().equals(userLogado)){
                return "NOPERM";
            }
            if(!userExists(user)){
                return "NOUSER";
            }
            
            ws.addUser2(user);
            
            try {
                File file = new File(WORKSPACES_FILE);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder fileContent = new StringBuilder();
                String line;
                boolean foundWorkspace = false; 
    
                while ((line = reader.readLine()) != null) {                
                    if (foundWorkspace) {
                        line = line + "," + user; 
                        foundWorkspace = false;
                    }

                    fileContent.append(line).append("\n");
                    
                    if (line.startsWith(workspace_name)) {
                        foundWorkspace = true; 
                    }
                }
    
                reader.close();
    
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(fileContent.toString());
                writer.close();
    
            } catch (IOException e) {
                e.printStackTrace();
            }

           return "OK";
        }

        //verifica se o "user" existe
        private synchronized boolean userExists(String user){
            for (Workspace ws : workspaces) {
                if (ws.getUsers().contains(user)) {
                    return true;
                }
            }
            return false;
        }

        //verifica se a workspace com o nome "name" ja existe
        private synchronized Workspace getWorkspace(String name) {
            for (Workspace ws : workspaces) {
                if (ws.getName().equals(name)) {
                    return ws;
                }
            }
            return null;
        }

        //verifica se a workspace com o nome "name" ja existe
        private synchronized boolean workspaceExists(String name) {
            for (Workspace ws : workspaces) {
                if (ws.getName().equals(name)) {
                    return true;
                }
            }
            return false;
        }

        private synchronized String createWorkspace(String user, String ws) {

            File file = new File(WORKSPACES_FILE);
            if (file.exists()) {
                if (this.workspaceExists(ws)) {
                    return "NOK";
                }
            }

            File workspaceFolder = new File(ws);
            if (!workspaceFolder.exists()) {
                if (workspaceFolder.mkdir()) {
                    System.out.println("Workspace criado: " + ws);
            } else {
                    System.out.println("Erro ao criar o workspace: " + ws);
                    return "ERROR";
            }
        }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write(ws + ":" + user + "\n"); // escrever dono
                bw.write("> "+user+"\n"); // escrever users
                bw.flush();
                ArrayList<String> users = new ArrayList<>();
                users.add(user);
                workspaces.add(new Workspace(ws, user, users));
                return "OK";

            } catch (IOException e) {
                e.printStackTrace();
                return "ERROR";
            }
        }


        private synchronized String authenticateUser(String username, String password) {
            File file = new File(USERS_FILE);
            File workspacesFile = new File(WORKSPACES_FILE);
            Map<String, String> users = new HashMap<>();
        
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            users.put(parts[0], parts[1]);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        
            // verifica se user existe
            if (users.containsKey(username)) {
                //verifica senha combina
                if (users.get(username).equals(password)){
                    return "OK-USER";
                }
                else{
                    return "WRONG-PWD";
                }
            } else {
                //escreve user e password no users.txt
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                    bw.write(username + ":" + password + "\n");
                    bw.flush(); 
                } catch (IOException e) {
                    e.printStackTrace();
                    return "erro";
                }
                //adiciona workspace no workspaces.txt
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(workspacesFile, true))) {
                    String nome_workspace = "ws";
                    if(workspaces.size() + 1 < 10){
                        nome_workspace = nome_workspace + "00" + (workspaces.size() + 1) ;
                    }
                    else if (workspaces.size() + 1 < 100){
                        nome_workspace = nome_workspace + "0" + (workspaces.size() + 1) ;
                    }
                    else{
                        nome_workspace = nome_workspace + (workspaces.size() + 1) ;
                    }

                    bw.write(nome_workspace + ":" + username + "\n"); 
                    bw.write("> "+username +"\n"); 
                    bw.flush();

                    ArrayList<String> utilizadores = new ArrayList<>();
                    utilizadores.add(username);
                    workspaces.add(new Workspace(nome_workspace, username, utilizadores));
                
                    // Cria workspace/pasta
                    File workspaceFolder = new File(nome_workspace);
                    if (!workspaceFolder.exists()) {
                        if (workspaceFolder.mkdir()) {
                            System.out.println("Pasta do workspace criada com sucesso: " + nome_workspace);
                        }
                    } else {
                            System.out.println("Falha ao criar a pasta do workspace: " + nome_workspace);
                            return "ERROR";
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return "ERROR";
                }
                return "OK-NEW-USER";
            }
        }


        private synchronized void serverSendFile(ObjectOutputStream outStream, File file) throws IOException {
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

        private synchronized void serverReceiveFile(ObjectInputStream inStream, File file) throws IOException {
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

        private synchronized void closeResources(ObjectOutputStream outStream, ObjectInputStream inStream, Socket socket) {
            try {
                if (outStream != null) outStream.close();
                if (inStream != null) inStream.close();
                if (socket != null) socket.close();
                System.out.println("Recursos fechados com sucesso.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}