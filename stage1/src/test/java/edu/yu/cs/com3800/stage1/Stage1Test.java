package edu.yu.cs.com3800.stage1;

import java.io.IOException;

import org.junit.Test;

public class Stage1Test {

	public void print(String exp, String act) {
		System.out.println("Expected response:");
		System.out.println(exp);
		System.out.println("Actual response:");
		System.out.println(act);
	}

	@Test
	public void serverRunningTest() throws IOException {
		SimpleServerImpl server = new SimpleServerImpl(9000);
		server.start();
		server.stop();
		System.out.println("Server ran successful!");
	}

	@Test
	public void clientToServerTest() throws IOException {
		SimpleServerImpl server = new SimpleServerImpl(9000);
		server.start();
		ClientImpl client = new ClientImpl("localhost", 9000);
		server.stop();
		System.out.println("Client connenction was successful!");
	}

	@Test
	public void compileAndRunTest() throws IOException {
		SimpleServerImpl server = new SimpleServerImpl(9000);
		server.start();
		ClientImpl client = new ClientImpl("localhost", 9000);
		client.sendCompileAndRunRequest("public class HelloWorld {public HelloWorld(){}public String run(){return \"Hello World!\";}}");
		print("Hello World!", client.getResponse().getBody());
		server.stop();
	}

	@Test
	public void compileAndRunErrorTest() throws IOException {
		SimpleServerImpl server = new SimpleServerImpl(9000);
		server.start();
		ClientImpl client = new ClientImpl("localhost", 9000);
		client.sendCompileAndRunRequest("public HelloWorld {public HelloWorld(){}public String run(){return \"Hello World!\";}}");
		print("java.lang.IllegalArgumentException: No class name found in code", client.getResponse().getBody());
		server.stop();
	}

	@Test
	public void multipleRequestsTest() throws IOException {
		SimpleServerImpl server = new SimpleServerImpl(9000);
		server.start();
		ClientImpl client = new ClientImpl("localhost", 9000);
		client.sendCompileAndRunRequest("public class HelloWorld {public HelloWorld(){}public String run(){return \"Hello World\";}}");
		print("Hello World", client.getResponse().getBody());
		client.sendCompileAndRunRequest("public class HelloWorld {public HelloWorld(){}public String run(){return \"Hello World 2.0!!!!!!\";}}");
		print("Hello World 2.0!!!!!!", client.getResponse().getBody());
		server.stop();
	}

	@Test
	public void multipleClientsTest() throws IOException {
		SimpleServerImpl server = new SimpleServerImpl(9000);
		server.start();
		ClientImpl clientOne = new ClientImpl("localhost", 9000);
		ClientImpl clientTwo = new ClientImpl("localhost", 9000);
		clientOne.sendCompileAndRunRequest("public class HelloWorld {public HelloWorld(){}public String run(){return \"Hello World\";}}");
		print("Hello World", clientOne.getResponse().getBody());
		clientTwo.sendCompileAndRunRequest("public class HelloWorld {public HelloWorld(){}public String run(){return \"Hello World 2.0!!!!!!\";}}");
		print("Hello World 2.0!!!!!!", clientTwo.getResponse().getBody());
		server.stop();
	}

	@Test // null return value
	public void compileAndRunWithNullTest() throws IOException {
		SimpleServerImpl server = new SimpleServerImpl(9000);
		server.start();
		ClientImpl client = new ClientImpl("localhost", 9000);
		client.sendCompileAndRunRequest("");
		print("null", client.getResponse().getBody());
		server.stop();
	}

	@Test
	public void compileAndRunErrorTwoTest() throws IOException {
		SimpleServerImpl server = new SimpleServerImpl(9000);
		server.start();
		ClientImpl client = new ClientImpl("localhost", 9000);
		client.sendCompileAndRunRequest("public class HelloWorld {public HelloWorld(){}}");
		print("java.lang.ReflectiveOperationException: Could not create and run instance of class", client.getResponse().getBody());
		server.stop();
	}
	
}