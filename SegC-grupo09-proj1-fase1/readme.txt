Segurança e Confiabilidade Projeto 1, fase 1

Grupo 9:
    -Duarte Carvalho fc59801 (33.3%)
    -Rodrigo Freitas fc59868 (33.3%)
    -Tiago Lourenço fc59877 (33.3%)

Compilação:
    javac mySharingServer.java 
    javac mySharingClient.java
    javac Workspace.java 
    jar cfe mySharingServer.jar mySharingServer mySharingServer.class mySharingClient.class Workspace.class mySharingServer$ServerThread.class
    jar cfe mySharingClient.jar mySharingClient mySharingClient.class mySharingServer.class Workspace.class


Execução:
    O porto é opcional, por default irá ser usado o 12345 no clientes e servidor
    Num terminal (servidor): java -jar mySharingServer.jar [port]
    Em vários terminais (vários clientes): java -jar mySharingClient.jar <serverAddress> <user-id> <password> , em que serverAddress = IP[:port]


Limitações:
    A aplicação simula o modelo cliente/servidor, contudo não é segura (melhorar na fase 2). 
    Por exemplo:
        - Password não é encriptada/desincriptada
        - Ficheiros não são encriptados/desincriptados
        - Cliente pode utilizar password fraca
        - Não existe limite de tentativas de login
        - Falta de hashing nas senhas quando armazenadas no servidor
        - Não existe multi factor authentication



