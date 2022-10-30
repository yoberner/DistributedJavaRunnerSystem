package edu.yu.cs.com3800.stage1;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.Calendar;
import java.util.logging.*;

public class ClientImpl implements Client {
	private String hostName;
	private int hostPort;
	private Response response;
	private static Logger logger = Logger.getLogger(ClientImpl.class.getName());
	private static FileHandler fh;
	// one instance, reuse
    private final HttpClient httpClient = HttpClient.newBuilder()
		.version(HttpClient.Version.HTTP_2)
		.build();

	public ClientImpl(String hostName, int hostPort) throws MalformedURLException {
		this.hostName = hostName;
		this.hostPort = hostPort;

		try {
			fh = new FileHandler("ClientLogger- " + Calendar.getInstance().getTime().toString().replaceAll(":", "-") + ".log");
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		logger.addHandler(fh);
		SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
	}

	// takes in java code to send to server
	@Override
	public void sendCompileAndRunRequest(String src) throws IOException {
		if (src == null) {
			throw new IllegalArgumentException("Error: null input!");
		}
		logger.info("- client called");

		HttpRequest request = HttpRequest.newBuilder() // builds new request
                .POST(HttpRequest.BodyPublishers.ofString(src)) // formating to type of request: POST
                .uri(URI.create("http://"+this.hostName+":"+this.hostPort+"/compileandrun")) // supplying server address
                // .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .header("Content-Type", "text/x-java-source")
                .build();

		try {
			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // sent to server
			this.response = new Response(response.statusCode(), response.body());

			logger.info("Client received response from server:\nResponse status code: " + Integer.toString(response.statusCode()));
			logger.info("Response status body: " + response.body());
		} catch (InterruptedException e) {
			logger.info("Error: client request failed to send to server");
			e.printStackTrace();
		}
	}

	@Override
	public Response getResponse() throws IOException {
		return this.response;
	}

	// public static void main(String[] args) throws IOException {
	// 	ClientImpl client = new ClientImpl("localhost", 9000);
    //     client.sendCompileAndRunRequest("public class HelloWorld {public HelloWorld(){}public String run(){return \"Hello World!\";}}");
	// }

}