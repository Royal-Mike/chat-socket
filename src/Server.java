import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Server
{
    static ClientManager clientManager;
    static UserManager userManager;
    static MessageManager globalMsgManager;
    static ArrayList<GroupManager> groupManagers;
    static Uploads uploads;
    private static GroupManager getGroupManager(String owner) {
        for (GroupManager gm : groupManagers) {
            if (Objects.equals(gm.getOwner(), owner)) {
                return gm;
            }
        }
        return null;
    }
    static class SendMsgThread extends Thread {
        BufferedWriter bw;
        String message = "";
        public SendMsgThread(BufferedWriter ibw, String msg) {
            bw = ibw;
            message = msg;
        }
        public void run() {
            try {
                bw.write(message);
                bw.newLine();
                bw.flush();
            }
            catch (IOException e)
            {
                System.out.println("IOException");
            }
        }
    }
    static class ReceiveThread extends Thread {
        private final ClientInfo ci;
        private static String received;
        private boolean upload;
        private String uploadTarget;
        private String uploadExt;
        public ReceiveThread(ClientInfo ici) {
            ci = ici;
            upload = false;
        }
        private void ReadGroupChatData(String name, boolean send) throws IOException {
            GroupManager gm = new GroupManager(name);
            String path = "./data/groups/" + name;
            BufferedReader br = new BufferedReader(new FileReader(path + "/$data.txt"));

            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\$\\$");
                String groupName = split[0];
                ArrayList<String> users = new ArrayList<>(List.of(split[1].split(";;;")));

                BufferedReader brg = new BufferedReader(new FileReader(path + "/" + groupName + ".txt"));
                MessageManager m = new MessageManager(brg);
                Group g = new Group(groupName, name, users, m);
                gm.addGroup(g);

                if (send) {
                    SendMsgThread smt = new SendMsgThread(ci.bw, g.toString());
                    smt.start();
                }
            }

            groupManagers.add(gm);
        }
        private void SendGroupChatData(Group g) {
            SendMsgThread smt = new SendMsgThread(ci.bw, g.toString());
            smt.start();
        }
        private void SendGlobalMsg() throws InterruptedException {
            String data = "$globalData$" + globalMsgManager.toString();
            SendMsgThread smt = new SendMsgThread(ci.bw, data);
            smt.start();
            smt.join();
        }
        private void ValidateLogin() throws IOException, InterruptedException {
            String data = received.split("\\$login:")[1];
            String us = data.split(",")[0];
            String pw = data.split(",")[1];

            String result = userManager.validateUser(new User(us, pw)) ? "$login:pass" : "$login:fail";
            SendMsgThread smt = new SendMsgThread(ci.bw, result);
            smt.start();
            smt.join();

            if (result.equals("$login:pass")) {
                ci.updateName(us);
                clientManager.addClient(ci);

                // Get global chat
                SendGlobalMsg();

                // Get groups owned by this user
                GroupManager gmo = getGroupManager(us);
                if (gmo == null) {
                    Path path = Path.of("./data/groups/" + us);
                    if (Files.exists(path)) {
                        ReadGroupChatData(us, true);
                    }
                }
                else {
                    ArrayList<Group> gs = gmo.getGroups();
                    for (Group g : gs) {
                        SendGroupChatData(g);
                    }
                }

                // Get groups this user are in
                BufferedReader br = new BufferedReader(new FileReader("./data/groups/$data.txt"));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] split = line.split("\\$\\$");
                    if (Objects.equals(split[0], us)) {
                        String[] groups = split[1].split(";;;");
                        for (String s : groups) {
                            String[] groupData = s.split("/");
                            String owner = groupData[0];
                            String name = groupData[1];

                            if (Objects.equals(us, owner)) continue;

                            GroupManager gm = getGroupManager(owner);
                            if (gm == null) {
                                ReadGroupChatData(owner, false);
                                gm = getGroupManager(owner);
                            }

                            assert gm != null;
                            Group g = gm.getGroup(name, owner);
                            SendMsgThread smtg = new SendMsgThread(ci.bw, g.toString());
                            smtg.start();
                            smtg.join();
                        }
                    }
                }

                // Get history
                User u = userManager.getUser(us);
                MessageManager mh = u.getHistory();
                SendMsgThread smth = new SendMsgThread(ci.bw, "$historyData$" + mh.toString());
                smth.start();
                smth.join();
            }
        }
        private void ValidateSignup() throws IOException {
            String data = received.split("\\$signup:")[1];
            String us = data.split(",")[0];
            String pw = data.split(",")[1];
            String result = userManager.checkUsername(us) ? "$signup:fail" : "$signup:pass";
            SendMsgThread smt = new SendMsgThread(ci.bw, result);
            smt.start();
            if (result.equals("$signup:pass")) {
                userManager.addUser(new User(us, pw, false));
                userManager.saveToFile();
            }
        }
        private void ProcessMsgInput() throws IOException {
            String[] split = received.split("\\$message:")[1].split(">");
            String content = split[0].trim();
            String target = split[1].trim();

            ArrayList<ClientInfo> clients = clientManager.getClients();
            User u = userManager.getUser(ci.name);

            // Global message
            if (target.equals("global")) {
                globalMsgManager.addMessage(new Message(ci.name, content));
                globalMsgManager.saveToFile("./data/global.txt");

                for (ClientInfo c : clients) {
                    if (!Objects.equals(c.name, ci.name)) {
                        SendMsgThread smt = new SendMsgThread(c.bw, "$global:" + ci.name + ";;;" + content);
                        smt.start();
                    }
                }

                u.addHistory(new Message("Global Chat", content));
                u.saveHistoryToFile();
            }

            // Group message
            else if (target.startsWith("group")) {
                Group g = null;

                String[] splitGroup = target.split("\\$\\$");
                String targetGroup = splitGroup[1];
                String targetOwner = splitGroup[2];

                for (GroupManager gm : groupManagers) {
                    if (Objects.equals(targetOwner, gm.getOwner())) {
                        gm.addMessage(targetGroup, new Message(ci.name, content));
                        g = gm.getGroup(targetGroup, targetOwner);
                        g.saveToFile("./data/groups/" + targetOwner + "/" + targetGroup + ".txt");
                        break;
                    }
                }

                assert g != null;
                ArrayList<String> users = g.getUsers();
                users.add(targetOwner);

                for (ClientInfo c : clients) {
                    for (String s : users) {
                        if (Objects.equals(c.name, s)) {
                            SendMsgThread smt = new SendMsgThread(c.bw, "$group:" + targetGroup
                                    + ";;;" + targetOwner + ";;;" + ci.name + ";;;" + content);
                            smt.start();
                            break;
                        }
                    }
                }

                u.addHistory(new Message("Group: " + targetGroup + " (" + targetOwner + ")", content));
                u.saveHistoryToFile();
            }
        }
        private void SaveGroupRelationsToFile() throws IOException {
            BufferedWriter bw = new BufferedWriter(new FileWriter("./data/groups/$data.txt"));
            ArrayList<User> allUsers = userManager.getUsers();

            for (User u : allUsers) {
                ArrayList<Group> relations = new ArrayList<>();
                for (GroupManager gmr : groupManagers) {
                    ArrayList<Group> alg = gmr.getRelations(u.getUsername());
                    relations.addAll(alg);
                }

                if (!relations.isEmpty()) {
                    bw.write(u.getUsername() + "$$");
                    for (int i = 0; i < relations.size(); i++) {
                        Group g = relations.get(i);
                        bw.write(g.getOwner() + "/" + g.getName());
                        if (i != relations.size() - 1) bw.write(";;;");
                    }
                    bw.newLine();
                }
            }

            bw.close();
        }
        private void ProcessCreateGroup() throws IOException {
            String[] split = received.split("\\$createGroup:")[1].split(">");
            String owner = split[0].trim();
            String[] groupData = split[1].trim().split("\\$\\$");
            String groupName = groupData[0];
            ArrayList<String> users = new ArrayList<>(List.of(groupData[1].split(";;;")));

            for (int i = users.size() - 1; i > -1; i--) {
                String s = users.get(i);
                if (!userManager.checkUsername(s) || Objects.equals(s, owner)) {
                    users.remove(i);
                }
            }

            if (users.isEmpty()) {
                SendMsgThread smt = new SendMsgThread(ci.bw, "$createGroupRes:fail");
                smt.start();
                return;
            }

            Group g = new Group(groupName, owner, users, new MessageManager());
            GroupManager gm = getGroupManager(owner);

            if (gm == null) {
                gm = new GroupManager(owner);
                groupManagers.add(gm);
            }

            gm.addGroup(g);

            File file = new File("./data/groups/" + owner + "/$data.txt");
            file.getParentFile().mkdirs();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            gm.saveToFile(bw);

            bw = new BufferedWriter(new FileWriter("./data/groups/" + owner + "/" + groupName + ".txt"));
            bw.write("Username;;;Content");
            bw.close();

            String groupStr = groupName + "$$" + owner + "$$" + String.join(";;;", users);
            SendMsgThread smt = new SendMsgThread(ci.bw, "$createGroupRes:" + groupStr);
            smt.start();

            ArrayList<ClientInfo> clients = clientManager.getClients();
            for (ClientInfo c : clients) {
                if (users.contains(c.name)) {
                    SendMsgThread smtj = new SendMsgThread(c.bw, "$createGroupJoin:" + groupStr);
                    smtj.start();
                }
            }

            SaveGroupRelationsToFile();
        }
        private void DeleteHistory() throws IOException {
            String user = received.split("\\$deleteHistory:")[1];
            User u = userManager.getUser(user);
            u.deleteHistory();
        }
        private void StartSaveFile() {
            String[] data = received.split("\\$uploadFile:")[1].split(">");
            uploadExt = data[0];

            if (data[1].startsWith("group$$")) {
                uploadTarget = data[1].split("group\\$\\$")[1];
            }
            else {
                uploadTarget = data[1];
            }

            upload = true;
        }
        private void ProcessUploadFile() throws IOException {
            String filename = uploads.saveFile(ci.ss, uploadExt);
            ArrayList<ClientInfo> clients = clientManager.getClients();
            User u = userManager.getUser(ci.name);

            // Global file
            if (Objects.equals(uploadTarget, "global")) {
                String sendThread = "$global:" + ci.name + ";;;$file$" + filename;

                SendMsgThread smt = new SendMsgThread(ci.bw, sendThread);
                smt.start();

                for (ClientInfo c : clients) {
                    if (!Objects.equals(c.name, ci.name)) {
                        SendMsgThread smtg = new SendMsgThread(c.bw, sendThread);
                        smtg.start();
                    }
                }

                globalMsgManager.addMessage(new Message(ci.name, "$file$" + filename));
                globalMsgManager.saveToFile("./data/global.txt");

                u.addHistory(new Message("Global Chat", "$file$" + filename));
                u.saveHistoryToFile();
            }

            // Group file
            else {
                Group g = null;

                String[] splitGroup = uploadTarget.split("\\$\\$");
                String targetGroup = splitGroup[0];
                String targetOwner = splitGroup[1];

                for (GroupManager gm : groupManagers) {
                    if (Objects.equals(targetOwner, gm.getOwner())) {
                        gm.addMessage(targetGroup, new Message(ci.name, "$file$" + filename));
                        g = gm.getGroup(targetGroup, targetOwner);
                        g.saveToFile("./data/groups/" + targetOwner + "/" + targetGroup + ".txt");
                        break;
                    }
                }

                assert g != null;
                ArrayList<String> users = g.getUsers();
                users.add(targetOwner);

                for (ClientInfo c : clients) {
                    for (String s : users) {
                        if (Objects.equals(c.name, s)) {
                            SendMsgThread smt = new SendMsgThread(c.bw, "$group:" + targetGroup
                                    + ";;;" + targetOwner + ";;;" + ci.name + ";;;$file$" + filename);
                            smt.start();
                            break;
                        }
                    }
                }

                u.addHistory(new Message("Group: " + targetGroup + " (" + targetOwner + ")", "$file$" + filename));
                u.saveHistoryToFile();
            }

            upload = false;
        }
        private void ProcessDownloadFile() throws IOException {
            String filename = received.split("\\$downloadFile:")[1];
            SendMsgThread smt = new SendMsgThread(ci.bw, "$downloadFile$");
            smt.start();
            DataOutputStream dos = new DataOutputStream(ci.ss.getOutputStream());
            uploads.downloadFile(filename, dos);
        }
        public void run() {
            do
            {
                try {
                    // Read file
                    if (upload) ProcessUploadFile();
                    // Read string
                    else {
                        received = ci.br.readLine();
                        System.out.println("Client " + ci.name + ": " + received);
                        if (received.startsWith("$uploadFile:")) StartSaveFile();
                        else {
                            if (received.startsWith("$login:")) ValidateLogin();
                            else if (received.startsWith("$signup:")) ValidateSignup();
                            else if (received.startsWith("$message:")) ProcessMsgInput();
                            else if (received.startsWith("$createGroup:")) ProcessCreateGroup();
                            else if (received.startsWith("$deleteHistory:")) DeleteHistory();
                            else if (received.startsWith("$downloadFile:")) ProcessDownloadFile();
                        }
                    }
                }
                catch (IOException | InterruptedException e)
                {
                    System.out.println(e.getMessage());
                    System.out.println("Client " + ci.name + " has left!");
                    try {
                        ci.br.close();
                        ci.bw.close();
                    } catch (IOException ex) {
                        System.out.println("IOException");
                    }
                    break;
                }
            }
            while (true);
        }
    }
    public static void main(String[] arg) throws IOException {
        ServerSocket s = new ServerSocket(3200);
        clientManager = new ClientManager();

        BufferedReader bru = new BufferedReader(new FileReader("./data/users.txt"));
        userManager = new UserManager(bru);

        BufferedReader brg = new BufferedReader(new FileReader("./data/global.txt"));
        globalMsgManager = new MessageManager(brg);

        groupManagers = new ArrayList<>();
        uploads = new Uploads();

        int count = 0;
        do
        {
            System.out.println("Waiting for a Client");
            Socket ss = s.accept();

            InputStream is = ss.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            OutputStream os = ss.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));

            ClientInfo ci = new ClientInfo(ss, "anon" + count, br, bw);

            ReceiveThread tt = new ReceiveThread(ci);
            tt.start();

            count++;
        }
        while (true);
    }
}