package edu.yu.cs.com3800.stage1;

import java.io.*;
import java.net.*;
import java.util.Calendar;
import java.util.logging.*;
import com.sun.net.httpserver.*;
import edu.yu.cs.com3800.*;

public class SimpleServerImpl implements SimpleServer {

	private JavaRunner javaRunner;
	private HttpServer server;
	private int port;
	private static Logger logger = Logger.getLogger(SimpleServerImpl.class.getName());
	private static FileHandler fh;

	public SimpleServerImpl(int port) throws IOException {
		this.javaRunner = new JavaRunner();
		this.port = port;

		fh = new FileHandler("ServerLogger- " + Calendar.getInstance().getTime().toString().replaceAll(":", "-") + ".log");
		logger.addHandler(fh);
		SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
	}

	public static void main(String[] args) {
		int port = 9000;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		SimpleServer myserver = null;
		try {
			myserver = new SimpleServerImpl(port);
			myserver.start();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			myserver.stop();
		}
	}

	class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			logger.info("- handler called");

			if (!t.getRequestMethod().equals("POST")) {
				logger.info("Error: incorrect request type\nResponse status code: 405");
				t.sendResponseHeaders(405, -1);
				t.getRequestBody().readAllBytes();
				OutputStream os = t.getResponseBody();
				os.close();
				return;
			}
			if (!t.getRequestHeaders().getFirst("Content-Type").equals("text/x-java-source")) {
				t.sendResponseHeaders(400, -1);
				logger.info("Error: incorrect code format\nResponse status code: 400");
				t.getRequestBody().readAllBytes();
				OutputStream os = t.getResponseBody();
				os.close();
				return;
			}

			InputStream is = t.getRequestBody(); // getting POST request from the client. ie getting java code as input stream
			byte[] byteArray = is.readAllBytes();
			InputStream iStream = new ByteArrayInputStream(byteArray);
			String output = "";
			logger.info("INPUT:\n" + new String(byteArray));
			try {
				output = javaRunner.compileAndRun(iStream);
				if (output == null) {
					output = "null";
				}
				t.sendResponseHeaders(200, output.length());
				logger.info("Successfully compiled and ran code!\nResponse status code: 200");
			} catch (IllegalArgumentException | ReflectiveOperationException e) {
				// if code does not work:
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				e.printStackTrace(new PrintStream(baos));
				String stackTrace = baos.toString();
				output = e.getMessage() + "\n" + stackTrace;
				t.sendResponseHeaders(400, output.length());
				logger.info("Error: code failed to compile and run on server\nResponse status code: 400");
				baos.close();
			}
			OutputStream os = t.getResponseBody();
			os.write(output.getBytes());
			logger.info("OUTPUT:\n" + output);
			os.close();
		}
	}

	@Override
	public void start() {
		try {
			logger.info("Server starting...");
			this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
			this.server.createContext("/compileandrun", new MyHandler());
   			this.server.setExecutor(null); // creates a default executor
   			this.server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		logger.info("Server stopping...");
		this.server.stop(0);
	}

}