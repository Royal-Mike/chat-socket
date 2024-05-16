import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Client
{
    static Socket s;
    static String name;
    static BufferedReader br;
    static BufferedWriter bw;
    static DataOutputStream dos;
    static volatile String sendInput = "";
    static boolean stopped = false;
    static int countGlobal = 0;
    static int countHistory = 0;
    static GroupManager groupManager;
    static JFrame FrameSignup;
    static JFrame FrameLogin;
    static JFrame FrameHome;
    static JTabbedPane tabbedPane;
    static ChatPanelComp cpcGlobal;
    static ChatPanelComp cpcHistory;
    static JPanel panelGroupChat;
    static JComboBox panelGroupCombo;
    static boolean download = false;
    static String downloadFile;
    static class ReceiveThread extends Thread {
        private static String received;
        private static ChatPanelComp CreateMsgGroupPanel(Group g, boolean show) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            JPanel panelL = new JPanel();
            panelL.setLayout(new FlowLayout(FlowLayout.LEFT));

            JLabel usersL = new JLabel("Users in this group: " + g.getOwner() + " (owner), "
                    + String.join(", ", g.getUsers()));
            usersL.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
            panelL.add(usersL);

            JPanel panelM = new JPanel();
            panelM.setLayout(new GridBagLayout());
            panelM.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JScrollPane pane = new JScrollPane(panelM);
            pane.setMinimumSize(new Dimension(300, 300));
            pane.setPreferredSize(new Dimension(300, 300));

            JPanel panelC = new JPanel();
            panelC.setLayout(new FlowLayout());

            JTextArea inputMsg = createPanelInput();
            JScrollPane paneInput = new JScrollPane(inputMsg);

            panelC.add(paneInput);

            JPanel panelBtn = new JPanel();
            panelBtn.setLayout(new GridLayout(0, 1, 0, 5));

            JButton sendMsg = new JButton("Send");
            sendMsg.addActionListener(new FrameClientHome.SendMessage(inputMsg));
            panelBtn.add(sendMsg);

            JButton sendFile = new JButton("File");
            sendFile.addActionListener(new FrameClientHome.SendFile());
            panelBtn.add(sendFile);

            panelC.add(panelBtn);

            panel.add(panelL);
            panel.add(pane);
            panel.add(panelC);

            String label = g.getName() + " (" + g.getOwner() + ")";
            panelGroupChat.add(panel, label);
            panelGroupCombo.addItem(label);
            if (show) panelGroupCombo.setSelectedItem(label);

            g.panel = panelM;
            g.pane = pane;
            g.scroll = pane.getVerticalScrollBar();

            groupManager.addGroup(g);

            ChatPanelComp cpc = new ChatPanelComp(g.panel, g.pane, g.scroll);
            return cpc;
        }
        private static void ProcessLogin() {
            if (received.contains("pass")) {
                FrameHome.setTitle(name + "'s Chat");
                FrameLogin.setVisible(false);
                FrameHome.setVisible(true);
                groupManager = new GroupManager(name);
            }
            else {
                JOptionPane.showMessageDialog(FrameLogin, "Username or password is incorrect!",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        private static void ProcessSignup() {
            if (received.contains("pass")) {
                JOptionPane.showMessageDialog(FrameSignup, "Account registered!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                FrameLogin.setVisible(true);
                FrameSignup.setVisible(false);
            }
            else {
                JOptionPane.showMessageDialog(FrameSignup, "Username already exists!",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        private static void ProcessMsgData() {
            String[] dataSplit = received.split("\\$globalData\\$");
            if (dataSplit.length == 2) {
                String[] dataMsg = dataSplit[1].split("\\$\\$");
                for (int i = 0; i < dataMsg.length; i++) {
                    String[] split = dataMsg[i].split(";;;");
                    String username = split[0], content = split[1];

                    // File message
                    if (content.contains("$file$")) {
                        String filename = content.split("\\$file\\$")[1];
                        AddMessageFile(cpcGlobal.panelMsg, username, filename, i);
                    }
                    // Normal message
                    else {
                        AddMessage(cpcGlobal.panelMsg, username, content, i);
                    }

                    countGlobal++;
                }
                RefreshMsg(cpcGlobal.paneMsg, cpcGlobal.paneMsgScroll);
            }
        }
        private static void ProcessGroupMsgData() throws IOException, ClassNotFoundException {
            Group g = new Group();
            g.fromString(received);

            ChatPanelComp cpc = CreateMsgGroupPanel(g, false);

            MessageManager m = g.getMessages();
            ArrayList<Message> data = m.getData();
            for (int i = 0; i < data.size(); i++) {
                Message msg = data.get(i);
                // File message
                if (msg.getContent().startsWith("$file$")) {
                    String filename = msg.getContent().split("\\$file\\$")[1];
                    AddMessageFile(g.panel, msg.getUsername(), filename, i);
                }
                // Text message
                else {
                    AddMessage(cpc.panelMsg, msg.getUsername(), msg.getContent(), i);
                }
            }

            RefreshMsg(cpc.paneMsg, cpc.paneMsgScroll);
        }
        private static void ProcessHistoryData() {
            String[] dataSplit = received.split("\\$historyData\\$");
            if (dataSplit.length == 2) {
                String[] dataMsg = dataSplit[1].split("\\$\\$");
                for (int i = 0; i < dataMsg.length; i++) {
                    String[] split = dataMsg[i].split(";;;");
                    String username = split[0], content = split[1];

                    if (content.startsWith("$file$")) {
                        String filename = content.split("\\$file\\$")[1];
                        AddMessageFile(cpcHistory.panelMsg, "Global Chat", filename, i);
                    }
                    else {
                        AddMessage(cpcHistory.panelMsg, username, content, i);
                    }

                    countHistory++;
                }
                RefreshMsg(cpcHistory.paneMsg, cpcHistory.paneMsgScroll);
            }
        }
        private static void ProcessNewGlobalMsg() {
            String[] split = received.split("\\$global:")[1].split(";;;");
            String username = split[0], content = split[1];

            // File message
            if (content.contains("$file$")) {
                String filename = content.split("\\$file\\$")[1];
                AddMessageFile(cpcGlobal.panelMsg, username, filename, ++countGlobal);
                if (Objects.equals(username, name)) {
                    AddMessageFile(cpcHistory.panelMsg, "Global Chat", filename, ++countHistory);
                    RefreshMsg(cpcHistory.paneMsg, cpcHistory.paneMsgScroll);
                }
            }
            // Normal message
            else {
                AddMessage(cpcGlobal.panelMsg, username, content, ++countGlobal);
            }

            RefreshMsg(cpcGlobal.paneMsg, cpcGlobal.paneMsgScroll);
        }
        private static void ProcessNewGroupMsg() {
            String[] split = received.split("\\$group:")[1].split(";;;");
            String group = split[0], owner = split[1], sender = split[2], content = split[3];
            Group g = groupManager.getGroup(group, owner);

            // File message
            if (content.startsWith("$file$")) {
                String filename = content.split("\\$file\\$")[1];
                g.addMessage(new Message(sender, content));
                AddMessageFile(g.panel, sender, filename, g.getMessageSize());

                if (Objects.equals(sender, name)) {
                    AddMessageFile(cpcHistory.panelMsg, "Group: " + g.getName() + " (" + g.getOwner() + ")",
                            filename, ++countHistory);
                    RefreshMsg(cpcHistory.paneMsg, cpcHistory.paneMsgScroll);
                }
            }
            // Text message
            else {
                if (Objects.equals(sender, name)) return;
                g.addMessage(new Message(sender, content));
                AddMessage(g.panel, sender, content, g.getMessageSize());
            }

            RefreshMsg(g.pane, g.scroll);
        }
        private static void CreateNewGroupChat(String name, String owner, ArrayList<String> users, boolean show) {
            Group g = new Group(name, owner, users, new MessageManager());
            CreateMsgGroupPanel(g, show);
        }
        private static void ProcessGroupResponse() {
            String res = received.split("\\$createGroupRes:")[1];
            if (Objects.equals(res, "fail")) {
                JOptionPane.showMessageDialog(FrameHome, "Failed to create Group Chat because no added users exist or only owner was added!",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            else {
                String[] data = res.split("\\$\\$");
                String name = data[0];
                String owner = data[1];
                ArrayList<String> users = new ArrayList<>(List.of(data[2].split(";;;")));
                CreateNewGroupChat(name, owner, users, true);
            }
        }
        private static void ProcessGroupJoin() {
            String res = received.split("\\$createGroupJoin:")[1];
            String[] data = res.split("\\$\\$");
            String name = data[0];
            String owner = data[1];
            ArrayList<String> users = new ArrayList<>(List.of(data[2].split(";;;")));
            CreateNewGroupChat(name, owner, users, false);
        }
        private static void ProcessDownloadFile() throws IOException {
            InputStream is = s.getInputStream();
            FileOutputStream fos = new FileOutputStream("./downloads/" + downloadFile);

            byte[] buffer = new byte[8196];
            int bytes;
            while ((bytes = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytes);
                if (bytes < 8196) break;
            }

            fos.close();

            JOptionPane.showMessageDialog(FrameHome, "File saved to ./downloads!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            download = false;
        }
        public void run() {
            do
            {
                try {
                    // Read file
                    if (download) ProcessDownloadFile();
                    // Read string
                    else {
                        received = br.readLine();
                        System.out.println(received);

                        if (Objects.equals(received, "$downloadFile$")) download = true;
                        else {
                            if (received.startsWith("$login:")) ProcessLogin();
                            else if (received.startsWith("$signup:")) ProcessSignup();

                            else if (received.startsWith("$globalData$")) ProcessMsgData();
                            else if (received.startsWith("$groupMsgData$")) ProcessGroupMsgData();
                            else if (received.startsWith("$historyData$")) ProcessHistoryData();

                            else if (received.startsWith("$global:")) ProcessNewGlobalMsg();
                            else if (received.startsWith("$group:")) ProcessNewGroupMsg();

                            else if (received.startsWith("$createGroupRes:")) ProcessGroupResponse();
                            else if (received.startsWith("$createGroupJoin:")) ProcessGroupJoin();
                        }
                    }
                }
                catch (IOException | ClassNotFoundException e)
                {
                    System.out.println(e.getMessage());
                }
            }
            while (!stopped);
        }
    }
    static class SendThread extends Thread {
        public void run() {
            do
            {
                try {
                    if (!sendInput.isEmpty()) {
                        bw.write(sendInput);
                        bw.newLine();
                        bw.flush();

                        if (sendInput.equalsIgnoreCase("quit")) {
                            bw.close();
                            br.close();
                            stopped = true;
                            break;
                        }

                        sendInput = "";
                    }
                }
                catch (IOException e)
                {
                    System.out.println("IOException");
                    break;
                }
            }
            while (true);
        }
    }
    static class FrameClientSignup extends JPanel {
        JTextField inputUS;
        JTextField inputPW;
        JTextField inputPWC;
        public FrameClientSignup() {
            setLayout(new BorderLayout());

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(0, 1));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel labelUS = new JLabel("Username:");
            inputUS = new JTextField(20);
            labelUS.setLabelFor(inputUS);
            panel.add(labelUS);
            panel.add(inputUS);

            JLabel labelPW = new JLabel("Password:");
            inputPW = new JPasswordField(20);
            labelPW.setLabelFor(inputPW);
            panel.add(labelPW);
            panel.add(inputPW);

            JLabel labelPWC = new JLabel("Confirm Password:");
            inputPWC = new JPasswordField(20);
            labelPWC.setLabelFor(inputPWC);
            panel.add(labelPWC);
            panel.add(inputPWC);

            JButton signup = new JButton("Sign Up");
            signup.addActionListener(new ValidateSignup());
            panel.add(Box.createRigidArea(new Dimension(0, 5)));
            panel.add(signup);

            JLabel labelAcc = new JLabel("Already has account?");
            panel.add(Box.createRigidArea(new Dimension(0, 5)));
            panel.add(labelAcc);

            JButton login = new JButton("Login");
            login.addActionListener(new SignupToLogin());
            panel.add(login);

            add(panel, BorderLayout.PAGE_START);
        }
        class ValidateSignup implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String username = inputUS.getText();
                String password = inputPW.getText();
                String passwordc = inputPWC.getText();
                if (Objects.equals(username, "") || Objects.equals(password, "") || Objects.equals(passwordc, "")) {
                    JOptionPane.showMessageDialog(FrameSignup, "Please fill in all fields!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                else if (!Objects.equals(password, passwordc)) {
                    JOptionPane.showMessageDialog(FrameSignup, "Passwords do not match!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    sendInput = "$signup:" + username + "," + password;
                }
            }
        }
        static class SignupToLogin implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                FrameLogin.setVisible(true);
                FrameSignup.setVisible(false);
            }
        }
        public void createAndShowGUI() {
            JFrame.setDefaultLookAndFeelDecorated(true);

            FrameSignup = new JFrame("Sign Up");
            FrameSignup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JComponent content = new FrameClientSignup();
            content.setOpaque(true);
            FrameSignup.setContentPane(content);

            FrameSignup.pack();
            FrameSignup.setLocationRelativeTo(null);
        }
    }
    static class FrameClientLogin extends JPanel {
        JTextField inputUS;
        JTextField inputPW;
        public FrameClientLogin() {
            setLayout(new BorderLayout());

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(0, 1));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel labelUS = new JLabel("Username:");
            inputUS = new JTextField(20);
            labelUS.setLabelFor(inputUS);
            panel.add(labelUS);
            panel.add(inputUS);

            JLabel labelPW = new JLabel("Password:");
            inputPW = new JPasswordField(20);
            labelPW.setLabelFor(inputPW);
            panel.add(labelPW);
            panel.add(inputPW);

            JButton login = new JButton("Login");
            login.addActionListener(new ValidateLogin());
            panel.add(Box.createRigidArea(new Dimension(0, 5)));
            panel.add(login);

            JLabel labelAcc = new JLabel("No account?");
            panel.add(Box.createRigidArea(new Dimension(0, 5)));
            panel.add(labelAcc);

            JButton signup = new JButton("Sign Up");
            signup.addActionListener(new LoginToSignup());
            panel.add(signup);

            add(panel, BorderLayout.PAGE_START);
        }
        class ValidateLogin implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String username = inputUS.getText();
                String password = inputPW.getText();
                if (Objects.equals(username, "") || Objects.equals(password, "")) {
                    JOptionPane.showMessageDialog(FrameLogin, "Please fill in all fields!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    sendInput = "$login:" + username + "," + password;
                    name = username;
                }
            }
        }
        static class LoginToSignup implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                FrameLogin.setVisible(false);
                FrameSignup.setVisible(true);
            }
        }
        public void createAndShowGUI() {
            JFrame.setDefaultLookAndFeelDecorated(true);

            FrameLogin = new JFrame("Login");
            FrameLogin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JComponent content = new FrameClientLogin();
            content.setOpaque(true);
            FrameLogin.setContentPane(content);

            FrameLogin.pack();
            FrameLogin.setLocationRelativeTo(null);
            FrameLogin.setVisible(true);
        }
    }
    private static GridBagConstraints CreateGBConstraints(String username, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        if (Objects.equals(username, name)) c.insets = new Insets(5, 40, 5, 5);
        else c.insets = new Insets(5, 5, 5, 40);

        c.gridx = 0;
        c.gridy = y;

        return c;
    }
    private static void AddMessage(JPanel panel, String username, String message, int y) {
        JLabel label = new JLabel("<html><div style='width:150px'><B>" + username + "</B><br>" + message + "</div></html>");
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        label.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        GridBagConstraints c = CreateGBConstraints(username, y);
        panel.add(label, c);
    }
    private static void AddMessageFile(JPanel panel, String username, String filename, int y) {
        JPanel cont = new JPanel();
        cont.setLayout(new BorderLayout());

        JLabel label = new JLabel(username);
        label.setBorder(new EmptyBorder(0, 0, 5, 0));
        cont.add(label, BorderLayout.PAGE_START);

        JButton btn = new JButton(filename);
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN));
        btn.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        btn.addActionListener(new DownloadFile());
        cont.add(btn, BorderLayout.PAGE_END);

        GridBagConstraints c = CreateGBConstraints(username, y);
        panel.add(cont, c);
    }
    static class DownloadFile implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            downloadFile = e.getActionCommand();
            sendInput = "$downloadFile:" + downloadFile;
        }
    }
    private static void RefreshMsg(JScrollPane pane, JScrollBar scroll) {
        pane.validate();
        pane.repaint();
        scroll.setValue(scroll.getMaximum());
    }
    private static JPanel createTabPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
    private static JPanel createPanelMsg() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }
    private static JTextArea createPanelInput() {
        JTextArea input = new JTextArea(3, 20);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setBorder(new EmptyBorder(5, 5, 5, 5));
        return input;
    }
    static class FrameClientHome extends JPanel {
        private void CreateMsgPanel(String type) {
            JPanel panelMsg = createPanelMsg();
            JScrollPane paneMsg = new JScrollPane(panelMsg);
            paneMsg.setMinimumSize(new Dimension(300, 400));
            paneMsg.setPreferredSize(new Dimension(300, 400));
            JScrollBar paneMsgScroll = paneMsg.getVerticalScrollBar();

            ChatPanelComp cpc = new ChatPanelComp(panelMsg, paneMsg, paneMsgScroll);

            if (Objects.equals(type, "global")) {
                cpcGlobal = cpc;
            }
            else if (Objects.equals(type, "history")) {
                cpcHistory = cpc;
            }
        }
        private JComponent FrameGlobal() {
            JComponent panelGlobal = createTabPanel();

            CreateMsgPanel("global");

            panelGlobal.add(cpcGlobal.paneMsg);

            // Chat
            JPanel panelChat = new JPanel();
            panelChat.setLayout(new FlowLayout());

            JTextArea inputMsg = createPanelInput();
            JScrollPane paneInput = new JScrollPane(inputMsg);

            panelChat.add(paneInput);

            // Buttons
            JPanel panelBtn = new JPanel();
            panelBtn.setLayout(new GridLayout(0, 1, 0, 5));

            JButton sendMsg = new JButton("Send");
            sendMsg.addActionListener(new SendMessage(inputMsg));
            panelBtn.add(sendMsg);

            JButton sendFile = new JButton("File");
            sendFile.addActionListener(new SendFile());
            panelBtn.add(sendFile);

            panelChat.add(panelBtn);

            panelGlobal.add(panelChat);

            return panelGlobal;
        }
        private JComponent FrameGroup() {
            JComponent panelGroup = createTabPanel();

            JPanel panelGroupBtn = new JPanel();
            panelGroupBtn.setLayout(new BoxLayout(panelGroupBtn, BoxLayout.X_AXIS));
            panelGroupBtn.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

            JButton addGroup = new JButton("+ Add New Group Chat");
            addGroup.addActionListener(new CreateGroupChat());
            panelGroupBtn.add(addGroup);

            panelGroup.add(panelGroupBtn);

            JPanel panelCombo = new JPanel();

            panelGroupCombo = new JComboBox();
            panelGroupCombo.addItemListener(new SwitchGroupChat());
            panelCombo.add(panelGroupCombo);

            panelGroup.add(panelCombo);

            panelGroupChat = new JPanel();
            panelGroupChat.setLayout(new CardLayout());

            panelGroup.add(panelGroupChat);

            return panelGroup;
        }
        private JComponent FrameHistory() {
            JComponent panelHistory = createTabPanel();

            CreateMsgPanel("history");

            panelHistory.add(cpcHistory.paneMsg);

            // Button
            JPanel panelBtn = new JPanel();
            panelBtn.setLayout(new FlowLayout());

            JButton delHistory = new JButton("Delete History");
            delHistory.addActionListener(new DeleteHistory());
            panelBtn.add(delHistory);

            panelHistory.add(panelBtn);

            return panelHistory;
        }
        public FrameClientHome() throws IOException, ClassNotFoundException {
            setLayout(new BorderLayout());

            tabbedPane = new JTabbedPane();

            // ----- GLOBAL CHAT -----
            JComponent panelGlobal = FrameGlobal();
            tabbedPane.addTab("Global Chat", null, panelGlobal, "Chat with everyone");

            // ----- GROUP CHAT -----
            JComponent panelGroup = FrameGroup();
            tabbedPane.addTab("Group Chat", null, panelGroup, "Create your own group chats");

            // ----- MESSAGE HISTORY -----
            JComponent panelHistory = FrameHistory();
            tabbedPane.addTab("Message History", null, panelHistory, "View message history");

            add(tabbedPane);
            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        }
        static class SendMessage implements ActionListener {
            private final JTextArea inputMsg;
            public SendMessage(JTextArea input) {
                inputMsg = input;
            }
            public void actionPerformed(ActionEvent e) {
                String content = inputMsg.getText();
                if (!content.isEmpty()) {
                    String targetSend = "";

                    int index = tabbedPane.getSelectedIndex();
                    switch (index) {
                        case 0:
                            targetSend = "global";

                            AddMessage(cpcGlobal.panelMsg, name, content, ++countGlobal);
                            RefreshMsg(cpcGlobal.paneMsg, cpcGlobal.paneMsgScroll);

                            AddMessage(cpcHistory.panelMsg, "Global Chat", content, ++countHistory);
                            RefreshMsg(cpcHistory.paneMsg, cpcHistory.paneMsgScroll);

                            sendInput = "$message:" + content + ">" + targetSend;
                            break;
                        case 1:
                            targetSend = "group";

                            String raw = (String) panelGroupCombo.getSelectedItem();
                            assert raw != null;
                            String[] data = raw.split("\\(");
                            String targetGroup = data[0].trim();
                            String targetOwner = data[1].split("\\)")[0].trim();

                            Group g = groupManager.getGroup(targetGroup, targetOwner);

                            Message m = new Message(name, content);
                            groupManager.addMessage(targetGroup, m);

                            AddMessage(g.panel, name, content, g.getMessageSize());
                            RefreshMsg(g.pane, g.scroll);

                            AddMessage(cpcHistory.panelMsg, "Group: " + g.getName() + " (" + g.getOwner() + ")",
                                    content, ++countHistory);
                            RefreshMsg(cpcHistory.paneMsg, cpcHistory.paneMsgScroll);

                            sendInput = "$message:" + content + ">" + targetSend + "$$" + targetGroup + "$$" + g.getOwner();
                            break;
                        default: break;
                    }

                    inputMsg.setText("");
                }
            }
        }
        static class UploadFileThread extends Thread {
            private final File file;
            public UploadFileThread(File f) {
                file = f;
            }
            public void run() {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    byte[] buffer = new byte[8196];
                    int bytes;
                    while ((bytes = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytes);
                    }
                    dos.flush();
                } catch (IOException e) {
                    System.out.println("UploadFileException");
                } finally {
                    try {
                        assert fis != null;
                        fis.close();
                    } catch (IOException e) {
                        System.out.println("UploadIOException");
                    }
                }
            }
        }
        static class SendFile implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                int x = fc.showOpenDialog(null);
                if (x == JFileChooser.APPROVE_OPTION) {
                    File fileToSend = fc.getSelectedFile();

                    String filePath = fileToSend.getAbsolutePath();
                    int index = filePath.lastIndexOf('.');
                    String ext = index > 0 ? filePath.substring(index + 1) : "";

                    int itab = tabbedPane.getSelectedIndex();
                    switch (itab) {
                        case 0:
                            sendInput = "$uploadFile:" + ext + ">global";
                            break;
                        case 1:
                            String raw = (String) panelGroupCombo.getSelectedItem();
                            assert raw != null;
                            String[] data = raw.split("\\(");
                            String targetGroup = data[0].trim();
                            String targetOwner = data[1].split("\\)")[0].trim();
                            sendInput = "$uploadFile:" + ext + ">group$$" + targetGroup + "$$" + targetOwner;
                            break;
                        default: break;
                    }

                    UploadFileThread uft = new UploadFileThread(fileToSend);
                    uft.start();
                }
            }
        }
        static class SwitchGroupChat implements ItemListener {
            public void itemStateChanged(ItemEvent e) {
                CardLayout cl = (CardLayout)(panelGroupChat.getLayout());
                cl.show(panelGroupChat, (String)e.getItem());
            }
        }
        static class CreateGroupChat implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String inputName = JOptionPane.showInputDialog("Enter a name for your new Group Chat:");
                if (inputName == null) return;

                Group g = groupManager.getGroup(inputName, name);
                if (g != null) {
                    JOptionPane.showMessageDialog(FrameLogin, "You already have a Group Chat with this name!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ArrayList<String> inputUsers = new ArrayList<>();
                while (true) {
                    String inputUserCur = JOptionPane.showInputDialog("Enter a username to add to " + inputName + ".\nLeave blank or press Cancel to stop adding more.");
                    if (inputUserCur != null) inputUsers.add(inputUserCur);
                    else break;
                }

                if (inputUsers.isEmpty()) {
                    JOptionPane.showMessageDialog(FrameLogin, "No users were added!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                sendInput = "$createGroup:" + name + ">" + inputName + "$$" + String.join(";;;", inputUsers);
            }
        }
        static class DeleteHistory implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                cpcHistory.panelMsg.removeAll();
                RefreshMsg(cpcHistory.paneMsg, cpcHistory.paneMsgScroll);
                sendInput = "$deleteHistory:" + name;
            }
        }
        public void createAndShowGUI() throws IOException, ClassNotFoundException {
            JFrame.setDefaultLookAndFeelDecorated(true);

            FrameHome = new JFrame("Chat");
            FrameHome.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JComponent content = new FrameClientHome();
            content.setOpaque(true);
            FrameHome.setContentPane(content);

            FrameHome.pack();
            FrameHome.setLocationRelativeTo(null);
        }
    }
    public static void main(String[] arg) {
        SwingUtilities.invokeLater(() -> {
            FrameClientSignup fcs = new FrameClientSignup();
            FrameClientLogin fcl = new FrameClientLogin();

            fcs.createAndShowGUI();
            fcl.createAndShowGUI();

            try {
                FrameClientHome fhl = new FrameClientHome();
                fhl.createAndShowGUI();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        try
        {
            s = new Socket("localhost",3200);

            InputStream is = s.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            OutputStream os = s.getOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(os));
            dos = new DataOutputStream(os);

            SendThread st = new SendThread();
            st.start();

            ReceiveThread rt = new ReceiveThread();
            rt.start();
        }
        catch(IOException e)
        {
            System.out.println("IOException");
        }
    }
}