package edu.yu.cs.com3800.stage4;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Logger;

import edu.yu.cs.com3800.*;

// woker class
public class JavaRunnerFollower extends Thread implements LoggingServer {

	private InetSocketAddress myAddress;
	private boolean shutdown;
	private Logger logger;
	private JavaRunner jr;
	private ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	public JavaRunnerFollower(LinkedBlockingQueue<Message> inMessage, LinkedBlockingQueue<Message> outMessage, InetSocketAddress myAddress, InetSocketAddress leaderAddress) {
		this.myAddress = myAddress;
		this.shutdown = false;
		this.logger = initializeLogging(JavaRunnerFollower.class.getCanonicalName() + "-myAddress-" + this.myAddress.getPort());
		try {
			this.jr = new JavaRunner();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(this.myAddress.getPort()+2);

			while (!this.shutdown) {
				Socket socket = ss.accept();
				logger.info("socket connection made...");

				Thread thread = new Thread(() -> {
					byte[] barray = null;
					try {
						barray = Util.readAllBytesFromNetwork(socket.getInputStream());
					} catch (IOException e) {
						logger.severe("ERROR! contents failed to read");
						e.printStackTrace();
					}
					Message message = new Message(barray);
					logger.info("message received from Leader: " + message.toString());
					byte[] data = message.getMessageContents();
					InputStream is = new ByteArrayInputStream(data);
					String output = null;
					try {
						output = this.jr.compileAndRun(is);
						logger.info("successfully compiled and ran!");
					} catch (IllegalArgumentException | IOException | ReflectiveOperationException e) {
						logger.severe("ERROR! contents failed to compile or run");
						output = Util.getStackTrace(e);
					}
					OutputStream os = null;
					try {
						os = socket.getOutputStream();
						os.write(output.getBytes());
					} catch (IOException e) {
						logger.severe("ERROR! contents failed to write to leader");
						e.printStackTrace();
					}
				});

				thread.setName("JRF: " + this.myAddress + " thread on connection: " + socket.getPort());
				thread.setDaemon(true);
				tpe.execute(thread);
				logger.info("Thread: " + thread.getId() + " - " + thread.getName() + " executed.");
			}
			ss.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void shutdown() {
		this.interrupt();
		this.shutdown = true;
		tpe.shutdown();
		this.logger.info("Follower Server and Threads Shutdown");
	}

}