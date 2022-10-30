package edu.yu.cs.com3800.stage3;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.logging.Logger;

import edu.yu.cs.com3800.*;
import edu.yu.cs.com3800.Message.MessageType;

// woker class
public class JavaRunnerFollower extends Thread implements LoggingServer {

	private LinkedBlockingQueue<Message> incomingMessages;
	private LinkedBlockingQueue<Message> outgoingMessages;
	private InetSocketAddress myAddress;
	private InetSocketAddress leaderAddress;
	private boolean shutdown;
	private Logger logger;
	
	public JavaRunnerFollower(LinkedBlockingQueue<Message> inMessage, LinkedBlockingQueue<Message> outMessage, InetSocketAddress myAddress, InetSocketAddress leaderAddress) {
		this.incomingMessages = inMessage;
		this.outgoingMessages = outMessage;
		this.myAddress = myAddress;
		this.leaderAddress = leaderAddress;
		this.shutdown = false;
		this.logger = initializeLogging(JavaRunnerFollower.class.getCanonicalName() + "-myAddress-" + this.myAddress.getPort());
	}

	@Override
	public void run() {
		JavaRunner jr;
		while (!this.shutdown) {
			Message message = null;
			while ((message = incomingMessages.poll()) == null) {
				// while there are no messages, keep polling
				// break when there is a message
			}
			this.logger.info("Message: " + message.getRequestID() + ", Recieved from Worker Address: " + this.myAddress);
			InputStream iStream = new ByteArrayInputStream(message.getMessageContents());
			String output = null;
			try {
				jr = new JavaRunner();
				output = jr.compileAndRun(iStream);
			} catch (IllegalArgumentException | IOException | ReflectiveOperationException e) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				e.printStackTrace(new PrintStream(baos));
				String stackTrace = baos.toString();
				output = e.getMessage() + "\n" + stackTrace;
			}
			Message newMessage = new Message(
				MessageType.COMPLETED_WORK, 
				output.getBytes(),
				this.myAddress.getHostName(),
				this.myAddress.getPort(),
				this.leaderAddress.getHostName(),
				this.leaderAddress.getPort(),
				message.getRequestID()
			);
			this.outgoingMessages.offer(newMessage);
			this.logger.info("Message: " + message.getRequestID() + ", Sent from Worker Address: " + this.myAddress);
		}
	}

	void shutdown() {
		this.shutdown = true;
		this.logger.info("Server Threads Shutdown");
	}

}