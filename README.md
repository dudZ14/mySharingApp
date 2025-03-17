# mySharingApp

**Security and Reliability Project 1, Phase 1**

## Group 9:
- **Duarte Carvalho** (fc59801) - 33.3%
- **Rodrigo Freitas** (fc59868) - 33.3%
- **Tiago Lourenço** (fc59877) - 33.3%

---

## Compilation

To compile the project, run the following commands:

- javac mySharingServer.java
- javac mySharingClient.java
- javac Workspace.java
- jar cfe mySharingServer.jar mySharingServer mySharingServer.class mySharingClient.class Workspace.class mySharingServer$ServerThread.class
- jar cfe mySharingClient.jar mySharingClient mySharingClient.class mySharingServer.class Workspace.class

---

## Execution 
#### The port is optional. By default, port 12345 will be used for both clients and the server.
- java -jar mySharingServer.jar [port]
- java -jar mySharingClient.jar [serverAddress] [user-id] [password] , where serverAddress = IP[:port] (the port is optional and defaults to 12345).

---

## Limitations
#### This application simulates a client-server model, but it is not secure (improvements will be made in Phase 2). Some of the limitations include:

- Passwords are not encrypted/decrypted.
- Files are not encrypted/decrypted.
- Clients can use weak passwords.
- No limit on login attempts.
- Passwords are not hashed when stored on the server.
- No multi-factor authentication.



