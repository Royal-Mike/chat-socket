public class Message {
    private String username;
    private String content;
    public Message(String u, String c) {
        username = u;
        content = c;
    }
    public String getUsername() {
        return username;
    }
    public String getContent() {
        return content;
    }
}
