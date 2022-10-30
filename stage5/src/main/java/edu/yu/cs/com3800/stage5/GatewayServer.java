package edu.yu.cs.com3800.stage5;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;
import com.sun.net.httpserver.*;

import edu.yu.cs.com3800.*;
import edu.yu.cs.com3800.Message.MessageType;

/**
 * creates an HTTPServer on whatever port number is passed to it in its constructor.
 * keeps track of what node is currently the leader and sends it all client requests over a TCP connection.
 * is a peer in the ZooKeeper cluster, but as an OBSERVER only – it does not get a vote in leader elections,
 * rather it merely observes and watches for a winner so it knows who the leader is to which it should send client requests.
 */
public class GatewayServer implements LoggingServer {

	private HttpServer server;
	private int port;
	private Logger logger;
	private GatewayPeerServerImpl gatewayServer;

	public GatewayServer(int port, GatewayPeerServerImpl gatewayServer) {
		this.port = port;
		this.gatewayServer = gatewayServer;
		this.logger = initializeLogging(GatewayServer.class.getCanonicalName() + "-myAddress-" + this.port);
	}

	class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			logger.info("- handler called");

			if (!t.getRequestMethod().equals("POST")) { // error handeling
				logger.info("Error: incorrect request type\nResponse status code: 405");
				t.sendResponseHeaders(405, -1);
				t.getRequestBody().readAllBytes();
				OutputStream os = t.getResponseBody();
				os.close();
				return;
			}
			if (!t.getRequestHeaders().getFirst("Content-Type").equals("text/x-java-source")) { // error handeling
				t.sendResponseHeaders(400, -1);
				logger.info("Error: incorrect code format\nResponse status code: 400");
				t.getRequestBody().readAllBytes();
				OutputStream os = t.getResponseBody();
				os.close();
				return;
			}

			InetSocketAddress leaderAddress = gatewayServer.getPeerByID(gatewayServer.getCurrentLeader().getProposedLeaderID());
			
			// sending tcp to leader
			InputStream is = t.getRequestBody();
			byte[] barray = is.readAllBytes();
			Message message = new Message(MessageType.WORK, barray, "localhost", port+2, leaderAddress.getHostName(), leaderAddress.getPort()+2);
			Socket socket = new Socket("localhost", leaderAddress.getPort()+2); // connects to server socket in round robin
			// PrintWriter pw = new PrintWriter(socket.getOutputStream());
			// pw.println(message);
			logger.info("socket connection made...");
			OutputStream os = socket.getOutputStream();
			os.write(message.getNetworkPayload());

			// getting tcp response from leader
			InputStream istrm = socket.getInputStream();
			byte[] returnedData = Util.readAllBytesFromNetwork(istrm);
			// Message returnedMessage = new Message(returnedData);
			logger.info("message response received from leader: " + new String(returnedData));

			// sending http back to client
			// byte[] outputData = returnedMessage.getMessageContents();
			t.sendResponseHeaders(200, returnedData.length);
			OutputStream oStream = t.getResponseBody();
			oStream.write(returnedData);

			String output = new String(returnedData);
			logger.info("OUTPUT:\n" + output);
			logger.info("Exchange successful.");

			// oStream.close();
			socket.close();
			t.close();
		}
	}

	public void start() {
		try {
			logger.info("Server starting...");
			this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
			this.server.createContext("/compileandrun", new MyHandler());
			Executor exc = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
   			this.server.setExecutor(exc);
   			this.server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		logger.info("Server stopping...");
		this.server.stop(0);
	}
	
}