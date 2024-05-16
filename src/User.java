import java.io.*;
import java.util.Objects;

public class User {
    private String username;
    private String password;
    private MessageManager history;
    public User(String us, String pw) throws IOException {
        username = us;
        password = pw;
    }
    public User(String us, String pw, boolean file) throws IOException {
        username = us;
        password = pw;
        if (file) {
            BufferedReader br = new BufferedReader(new FileReader("./data/history/" + username + ".txt"));
            history = new MessageManager(br);
        }
        else {
            BufferedWriter bw = new BufferedWriter(new FileWriter("./data/history/" + username + ".txt"));
            bw.write("Location;;;Content");
            bw.close();
            history = new MessageManager();
        }
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public MessageManager getHistory() {
        return history;
    }
    public void addHistory(Message m) {
        history.addMessage(m);
    }
    public boolean validate(User u) {
        return Objects.equals(u.getUsername(), username) && Objects.equals(u.getPassword(), password);
    }
    public void saveHistoryToFile() throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("./data/history/" + username + ".txt"));
        bw.write("Location;;;Content");
        bw.newLine();
        for (Message m : history.getData()) {
            bw.write(m.getUsername() + ";;;" + m.getContent());
            bw.newLine();
        }
        bw.close();
    }
    public void deleteHistory() throws IOException {
        history.deleteAll();
        BufferedWriter bw = new BufferedWriter(new FileWriter("./data/history/" + username + ".txt"));
        bw.write("Location;;;Content");
        bw.close();
    }
}
