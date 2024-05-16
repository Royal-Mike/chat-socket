import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MessageManager {
    private ArrayList<Message> data;
    public MessageManager() {
        data = new ArrayList<>();
    }
    public MessageManager(BufferedReader br) throws IOException {
        data = new ArrayList<>();

        br.readLine();

        String line;
        while ((line = br.readLine()) != null) {
            String[] split = line.split(";;;");
            String user = split[0];
            String cont = split[1];
            Message m = new Message(user, cont);
            addMessage(m);
        }

        br.close();
    }
    public void addMessage(Message m) {
        data.add(m);
    }
    public int getSize() {
        return data.size();
    }
    public ArrayList<Message> getData() {
        return data;
    }
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Message m : data) {
            result.append(m.getUsername()).append(";;;").append(m.getContent()).append("$$");
        }
        return result.toString();
    }
    public void fromString(String received) {
        String[] split = received.split("\\$\\$");
        for (String s : split) {
            String[] part = s.split(";;;");
            data.add(new Message(part[0], part[1]));
        }
    }
    public void saveToFile(String path) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(path));
        bw.write("Username;;;Content");
        bw.newLine();
        for (Message m : data) {
            bw.write(m.getUsername() + ";;;" + m.getContent());
            bw.newLine();
        }
        bw.close();
    }
    public void deleteAll() {
        data.clear();
    }
}
