import java.util.List;

/**                                                           
 * Grupo 9
 * @author Duarte Carvalho fc59801 (33.3%)  
 * @author Rodrigo Freitas fc59868 (33.3%)
 * @author Tiago Lourenço fc59877 (33.3%)
 */

public class Workspace {
    private String name;
    private String owner;
    private List<String> users;

    // Construtor
    public Workspace(String name, String owner, List<String> users) {
        this.name = name;
        this.owner = owner;
        this.users = users;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public List<String> getUsers() {
        return users;
    }

    public void addUser2(String user) {
        this.users.add(user);
    }

    public String toString(){
        return "Nome:"+name+ " Dono:"+owner+ " Users:"+users;
    }
}

