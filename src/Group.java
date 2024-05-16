import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Group {
    private String name;
    private String owner;
    private ArrayList<String> users;
    private MessageManager messages;
    protected JPanel panel;
    protected JScrollPane pane;
    protected JScrollBar scroll;
    public Group() {
        name = "";
        owner = "";
        users = new ArrayList<>();
        messages = new MessageManager();
    }
    public Group(String n, String o, ArrayList<String> u, MessageManager mg) {
        name = n;
        owner = o;
        users = u;
        messages = mg;
    }
    public String getName() {
        return name;
    }
    public String getOwner() { return owner; }
    public ArrayList<String> getUsers() { return users; }
    public MessageManager getMessages() {
        return messages;
    }
    public int getMessageSize() {
        return messages.getSize();
    }
    public boolean verifyUser(String user) {
        return users.contains(user);
    }
    public void addMessage(Message m) {
        messages.addMessage(m);
    }
    public String toString() {
        String result = "$groupMsgData$";
        result += name + "$$$";
        result += owner + "$$$";
        result += String.join(";;;", users) + "$$$";
        result += messages.toString();
        return result;
    }
    public void fromString(String received) {
        String split = received.split("\\$groupMsgData\\$")[1];
        String[] data = split.split("\\$\\$\\$");
        name = data[0];
        owner = data[1];
        users = new ArrayList<String>(List.of(data[2].split(";;;")));
        MessageManager mg = new MessageManager();
        if (data.length > 3) mg.fromString(data[3]);
        messages = mg;
    }
    public void saveToFile(String path) throws IOException {
        messages.saveToFile(path);
    }
}
