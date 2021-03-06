package socotra;

import socotra.common.ChatSession;
import socotra.common.ConnectionData;
import socotra.common.User;
import socotra.jdbc.JdbcUtil;
import socotra.jdbc.TwoTuple;
import socotra.service.OutputHandler;
import socotra.service.ServerThread;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This file is entry of server, used to accept clients.
 */

public class Server {

    /**
     * All connected clients's username and their ObjectOutputStream.
     */
    private static HashMap<User, OutputHandler> clients = new HashMap<>();
    /**
     * SSL server socket.
     */
    private static SSLServerSocket serverSocket;

    private static HashMap<User, ArrayList<ConnectionData>> depositPairwiseData = new HashMap<>();
    private static HashMap<User, ArrayList<ConnectionData>> depositSenderKeyData = new HashMap<>();
    private static HashMap<User, ArrayList<ConnectionData>> depositGroupData = new HashMap<>();
    private static HashMap<User, ArrayList<ConnectionData>> depositSwitchData = new HashMap<>();
    private static HashMap<User, TwoTuple<User, HashMap<ChatSession, ArrayList<ConnectionData>>>> backUpMessages = new HashMap<>();

    private static Set<User> users = new HashSet<>();

    /**
     * Initialize TLS before creating the SSL server socket.
     *
     * @throws Exception The Exception when initializing.
     */
    private static void initTLS() throws Exception {
        String SERVER_KEY_STORE = "src/main/resources/socotra_server_ks";
        String SERVER_KEY_STORE_PASSWORD = "socotra";
        System.setProperty("javax.net.ssl.trustStore", SERVER_KEY_STORE);
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(SERVER_KEY_STORE), SERVER_KEY_STORE_PASSWORD.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, SERVER_KEY_STORE_PASSWORD.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext context = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = tmf.getTrustManagers();
        context.init(kmf.getKeyManagers(), trustManagers, null);

        ServerSocketFactory factory = context.getServerSocketFactory();
        serverSocket = (SSLServerSocket) factory.createServerSocket(50443);
        serverSocket.setNeedClientAuth(false);
    }

    /**
     * Start server.
     *
     * @param args
     */
    public static void main(String[] args) {
        // Open a server socket:
        try {
            initTLS();
            System.out.println("Server bound.");
            JdbcUtil.init();
            JdbcUtil.connect();
            // TODO: load user info
            users = JdbcUtil.loadUsers();

        } catch (IOException e) {
            System.err.println("Couldn't listen on port: 50443.");
            e.printStackTrace();
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Listen to the socket, accepting connections from new clients, and running a new thread to serve each new client:
        try {
            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                System.out.println("Got a connection.");
                ServerThread s = new ServerThread(clientSocket);
                s.start();
            }
        } catch (Exception e) {
            System.out.println("Exception occurs.");
            e.printStackTrace();
            try {
                serverSocket.close();
            } catch (IOException io) {
                System.err.println("Couldn't close server socket" + io.getMessage());
            }
        } finally {
            try {
                JdbcUtil.end();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static Set<User> getUsers() {
        return Server.users;
    }

    public synchronized static void appendUsers(User user) {
        Server.users.add(user);
    }

    /**
     * Add new client to all clients hash map.
     *
     * @param user     The user information of client.
     * @param toClient The ObjectOutputStream of client.
     */
    public synchronized static void addClient(User user, OutputHandler toClient) {
        Server.clients.put(user, toClient);
    }

    /**
     * Getter for all connected clients.
     *
     * @return All connected clients.
     */
    public synchronized static HashMap<User, OutputHandler> getClients() {
        return Server.clients;
    }

    /**
     * Remove client from all connected clients use username and ObjectOutputStream.
     *
     * @param user     The user information of removed client.
     * @param toClient The ObjectOutputStream of removed client.
     */
    public synchronized static void removeClient(User user, OutputHandler toClient) {
        Server.clients.remove(user, toClient);
    }

    /**
     * Remove client from all connected clients use username.
     *
     * @param user The user information of removed client.
     */
    public synchronized static void removeClient(User user) {
        Server.clients.remove(user);
    }

    public synchronized static void storePairwiseData(User receiver, ConnectionData pairwiseData) {
        System.out.println("Store pairwise data.");
        ChatSession chatSession = pairwiseData.getChatSession();
        if (pairwiseData.getType() != 7 && chatSession.getSessionType() != ChatSession.PAIRWISE) {
            throw new IllegalStateException("Bad pairwiseData.");
        }
        ArrayList<ConnectionData> des = Server.depositPairwiseData.getOrDefault(receiver, new ArrayList<>());
        des.add(pairwiseData);
        Server.depositPairwiseData.put(receiver, des);
    }

    public synchronized static void storeSenderKeyData(User receiver, ConnectionData senderKeyData) {
        System.out.println("Store senderKey data.");
        if (senderKeyData.getType() != 8) {
            throw new IllegalStateException("Bad senderKeyData.");
        }
        ArrayList<ConnectionData> des = Server.depositSenderKeyData.getOrDefault(receiver, new ArrayList<>());
        des.add(senderKeyData);
        Server.depositSenderKeyData.put(receiver, des);
    }

    public synchronized static void storeGroupData(User receiver, ConnectionData groupData) {
        System.out.println("Store group data.");
        ChatSession chatSession = groupData.getChatSession();
        if (groupData.getType() != 7 && chatSession.getSessionType() != ChatSession.GROUP) {
            throw new IllegalStateException("Bad groupData.");
        }
        ArrayList<ConnectionData> des = Server.depositGroupData.getOrDefault(receiver, new ArrayList<>());
        des.add(groupData);
        Server.depositGroupData.put(receiver, des);
    }

    public synchronized static void storeSwitchData(User receiver, ConnectionData switchData) {
//        System.out.println("Store switch data to " + receiver);
        ArrayList<ConnectionData> des = Server.depositSwitchData.getOrDefault(receiver, new ArrayList<>());
        des.add(switchData);
        Server.depositSwitchData.put(receiver, des);
    }

    public synchronized static void storeBackUpMessages(User receiver, HashMap<ChatSession, ArrayList<ConnectionData>> backUpMessages, User sender) {
        Server.backUpMessages.put(receiver, new TwoTuple<>(sender, backUpMessages));
    }


    public synchronized static ArrayList<ConnectionData> loadPairwiseData(User user) {
        ArrayList<ConnectionData> result = Server.depositPairwiseData.get(user);
        Server.depositPairwiseData.remove(user);
        return result;
    }

    public synchronized static ArrayList<ConnectionData> loadSenderKeyData(User user) {
        ArrayList<ConnectionData> result = Server.depositSenderKeyData.get(user);
        Server.depositSenderKeyData.remove(user);
        return result;
    }

    public synchronized static ArrayList<ConnectionData> loadGroupData(User user) {
        ArrayList<ConnectionData> result = Server.depositGroupData.get(user);
        Server.depositGroupData.remove(user);
        return result;
    }

    public synchronized static ArrayList<ConnectionData> loadSwitchData(User user) {
        ArrayList<ConnectionData> result = Server.depositSwitchData.get(user);
        if (result != null) {
            System.out.println("Load switch data to " + user);
            result.forEach(n -> {
                System.out.println("    " + n.getUserSignature() + " 's switch data.");
            });
        }
        Server.depositSwitchData.remove(user);
        return result;
    }

    public synchronized static TwoTuple<User, HashMap<ChatSession, ArrayList<ConnectionData>>> loadBackUpMessages(User user) {
        TwoTuple<User, HashMap<ChatSession, ArrayList<ConnectionData>>> result = Server.backUpMessages.get(user);
        Server.backUpMessages.remove(user);
        return result;
    }
}
