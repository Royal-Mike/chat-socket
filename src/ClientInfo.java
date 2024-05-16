import java.io.*;
import java.net.*;

public class ClientInfo {
    protected Socket ss;
    protected String name;
    protected BufferedReader br;
    protected BufferedWriter bw;
    public ClientInfo(Socket is, String iname, BufferedReader ibr, BufferedWriter ibw) {
        ss = is;
        name = iname;
        br = ibr;
        bw = ibw;
    }
    public void updateName(String iname) {
        name = iname;
    }
}
