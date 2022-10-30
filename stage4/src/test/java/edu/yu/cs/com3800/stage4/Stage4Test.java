package edu.yu.cs.com3800.stage4;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.*;
import static org.junit.Assert.assertEquals;

import edu.yu.cs.com3800.*;

public class Stage4Test {

    GatewayServer gs;
    GatewayPeerServerImpl gpsi;

    @Before
    public void before() throws InterruptedException {
        System.out.println();
        System.out.println("New Test:");
        System.out.println();
        Thread.sleep(5000);
	}

    @Test
    public void test1() throws Exception {
        this.outgoingMessages = new LinkedBlockingQueue<>();
        createServers();
        printLeaders();
        GatewayPeerServerImpl gServer = this.gpsi;
        GatewayServer gs = new GatewayServer(9000, gServer);
        this.gs = gs;
        gs.start();
        ClientImpl client = new ClientImpl("localhost", 9000);
        client.sendCompileAndRunRequest(validClass);
        client.sendCompileAndRunRequest(validClass);
        client.sendCompileAndRunRequest(validClass);
        System.out.println(client.getResponse().getBody());
        assertEquals("Hello world!", client.getResponse().getBody());
        stopServers();
    }

    // tests can only be run one at a time
    // @Test
    public void test2() throws Exception {
        // this.ports = new int[]{8110, 8220, 8330, 8440, 8550, 8660, 8770, 8880, 8990, 8999, 8811, 8222};
        // this.myPort = 9912;
        // this.myAddress = new InetSocketAddress("localhost", this.myPort);
        // this.leaderPort = this.ports[this.ports.length - 1];

        this.outgoingMessages = new LinkedBlockingQueue<>();
        createServers();
        printLeaders();
        GatewayPeerServerImpl gServer = this.gpsi;
        GatewayServer gs = new GatewayServer(9001, gServer);
        this.gs = gs;
        gs.start();
        ClientImpl client = new ClientImpl("localhost", 9001);
        client.sendCompileAndRunRequest(validClass);
        client.sendCompileAndRunRequest(validClass);
        client.sendCompileAndRunRequest(validClass);
        System.out.println(client.getResponse().getBody());
        assertEquals("Hello world!", client.getResponse().getBody());

        ClientImpl client2 = new ClientImpl("localhost", 9001);
        client2.sendCompileAndRunRequest(validClass);
        client2.sendCompileAndRunRequest(validClass);
        client2.sendCompileAndRunRequest(validClass);
        assertEquals("Hello world!", client2.getResponse().getBody());

        ClientImpl client3 = new ClientImpl("localhost", 9001);
        client3.sendCompileAndRunRequest(validClass);
        client3.sendCompileAndRunRequest(validClass);
        client3.sendCompileAndRunRequest(validClass);
        assertEquals("Hello world!", client3.getResponse().getBody());

        stopServers();
    }

    public void print(String exp, String act) {
		System.out.println("Expected response:");
		System.out.println(exp);
		System.out.println("Actual response:");
		System.out.println(act);
	}



	private String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world!\";\n    }\n}\n";

    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private int[] ports = {8010, 8020, 8030, 8040, 8050, 8060, 8070, 8080};
    //private int[] ports = {8010, 8020};
    private int leaderPort = this.ports[this.ports.length - 1];
    private int myPort = 9999;
    private InetSocketAddress myAddress = new InetSocketAddress("localhost", this.myPort);
    private ArrayList<ZooKeeperPeerServer> servers;

    public void startDefTest() throws Exception {
        //step 1: create sender & sending queue
        this.outgoingMessages = new LinkedBlockingQueue<>();
        UDPMessageSender sender = new UDPMessageSender(this.outgoingMessages, this.myPort);
        //step 2: create servers
        createServers();
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(3000);
        }
        catch (Exception e) {
        }
        printLeaders();
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        for (int i = 0; i < this.ports.length; i++) {
            String code = this.validClass.replace("world!", "world! from code version " + i);
            sendMessage(code);
        }
        Util.startAsDaemon(sender, "Sender thread");
        this.incomingMessages = new LinkedBlockingQueue<>();
        UDPMessageReceiver receiver = new UDPMessageReceiver(this.incomingMessages, this.myAddress, this.myPort, null);
        Util.startAsDaemon(receiver, "Receiver thread");
        //step 4: validate responses from leader

        printResponses();

        //step 5: stop servers
        stopServers();
    }

    private void printLeaders() {
        for (ZooKeeperPeerServer server : this.servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            }
        }
    }

    private void stopServers() {
        for (ZooKeeperPeerServer server : this.servers) {
            server.shutdown();
        }
    }

    private void printResponses() throws Exception {
        String completeResponse = "";
        for (int i = 0; i < this.ports.length; i++) {
            Message msg = this.incomingMessages.take();
            String response = new String(msg.getMessageContents());
            completeResponse += "Response #" + i + ":\n" + response + "\n";
        }
        System.out.println(completeResponse);
    }

    private void sendMessage(String code) throws InterruptedException {
        Message msg = new Message(Message.MessageType.WORK, code.getBytes(), this.myAddress.getHostString(), this.myPort, "localhost", this.leaderPort);
        this.outgoingMessages.put(msg);
    }

    private void createServers() {
        //create IDs and addresses
        GatewayPeerServerImpl observer = null;
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(8);
        for (int i = 0; i < this.ports.length; i++) {
            peerIDtoAddress.put(Integer.valueOf(i).longValue(), new InetSocketAddress("localhost", this.ports[i]));
            this.ports[i] += 1;
        }
        //create servers
        this.servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            if (entry.getKey() == (long) 1) {
                observer = new GatewayPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
                this.servers.add(observer);
                new Thread((Runnable) observer, "Server on port " + observer.getAddress().getPort()).start();
            }
            else {
                ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
                this.servers.add(server);
                new Thread(server, "Server on port " + server.getAddress().getPort()).start();
            }
        }
        this.gpsi = observer;
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }







    class ClientImpl {
        private String hostName;
        private int hostPort;
        private Response response;
    
        private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    
        public ClientImpl(String hostName, int hostPort) throws MalformedURLException {
            this.hostName = hostName;
            this.hostPort = hostPort;
        }
    
        public void sendCompileAndRunRequest(String src) throws IOException {
            if (src == null) {
                throw new IllegalArgumentException("Error: null input!");
            }
    
            HttpRequest request = HttpRequest.newBuilder() // builds new request
                    .POST(HttpRequest.BodyPublishers.ofString(src)) // formating to type of request: POST
                    .uri(URI.create("http://"+this.hostName+":"+this.hostPort+"/compileandrun")) // supplying server address
                    // .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                    .setHeader("Content-Type", "text/x-java-source")
                    .build();
    
            try {
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // sent to server
                this.response = new Response(response.statusCode(), response.body());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    
        public Response getResponse() throws IOException {
            return this.response;
        }
    
        class Response {
            private int code;
            private String body;
    
            public Response(int code, String body) {
                this.code = code;
                this.body = body;
            }
    
            public int getCode() {
                return this.code;
            }
    
            public String getBody() {
                return this.body;
            }
        }
    
    }
	
}