import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class GroupManager {
    private String owner;
    private ArrayList<Group> data;
    public GroupManager(String n) {
        owner = n;
        data = new ArrayList<>();
    }
    public String getOwner() {
        return owner;
    }
    public ArrayList<Group> getRelations(String user) {
        ArrayList<Group> alg = new ArrayList<>();
        for (Group g : data) {
            if (g.verifyUser(user)) {
                alg.add(g);
            }
        }
        return alg;
    }
    public ArrayList<Group> getGroups() {
        return data;
    }
    public Group getGroup(String groupName, String groupOwner) {
        for (Group g : data) {
            if (Objects.equals(groupName, g.getName()) && Objects.equals(groupOwner, g.getOwner())) {
                return g;
            }
        }
        return null;
    }
    public void addGroup(Group g) {
        data.add(g);
    }
    public void addMessage(String groupName, Message m) {
        for (Group g : data) {
            if (Objects.equals(groupName, g.getName())) {
                g.addMessage(m);
                break;
            }
        }
    }
    public void saveToFile(BufferedWriter bw) throws IOException {
        for (Group g : data) {
            bw.write(g.getName() + "$$" + String.join(";;;", g.getUsers()));
            bw.newLine();
        }
        bw.close();
    }
}
