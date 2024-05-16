import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class UserManager {
    private ArrayList<User> users;
    public UserManager() {
        users = new ArrayList<>();
    }
    public UserManager(BufferedReader br) throws IOException {
        users = new ArrayList<>();

        br.readLine();

        String line;
        while ((line = br.readLine()) != null) {
            String[] split = line.split(";;;");
            String us = split[0];
            String pw = split[1];
            User u = new User(us, pw, true);
            addUser(u);
        }

        br.close();
    }
    public ArrayList<User> getUsers() {
        return users;
    }
    public void addUser(User u) {
        users.add(u);
    }
    public User getUser(String us) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(us)) {
                return user;
            }
        }
        return null;
    }
    public boolean validateUser(User u) {
        boolean check = false;
        for (User user : users) {
            if (user.validate(u)) {
                check = true;
                break;
            }
        }
        return check;
    }
    public boolean checkUsername(String us) {
        boolean check = false;
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(us)) {
                check = true;
                break;
            }
        }
        return check;
    }
    public void saveToFile() throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("./data/users.txt"));
        bw.write("Username;;;Password");
        bw.newLine();
        for (User u : users) {
            bw.write(u.getUsername() + ";;;" + u.getPassword());
            bw.newLine();
        }
        bw.close();
    }
}
