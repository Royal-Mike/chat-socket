import java.util.ArrayList;

public class ClientManager {
    private static final ArrayList<ClientInfo> clients = new ArrayList<>();
    public void addClient(ClientInfo ci) {
        clients.add(ci);
    }
    public ArrayList<ClientInfo> getClients() {
        return clients;
    }
    public ClientInfo findClient(String name) {
        for (ClientInfo client : clients) {
            if (name.equalsIgnoreCase(client.name))
                return client;
        }
        return null;
    }
}
