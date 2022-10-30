package edu.yu.cs.com3800.stage3;

import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.*;
import java.util.logging.Logger;

import edu.yu.cs.com3800.*;
import edu.yu.cs.com3800.Message.MessageType;

// leader class
public class RoundRobinLeader extends Thread implements LoggingServer {

	private LinkedBlockingQueue<Message> incomingMessages;
	private LinkedBlockingQueue<Message> outgoingMessages;
	private InetSocketAddress myAddress;
	private LinkedBlockingQueue<InetSocketAddress> workers;
	private HashMap<Long, InetSocketAddress> messageToId;
	private long messageIdCounter;
	private boolean shutdown;
	private Logger logger;

	public RoundRobinLeader(LinkedBlockingQueue<Message> inMessage, LinkedBlockingQueue<Message> outMessage, InetSocketAddress myAddress, LinkedBlockingQueue<InetSocketAddress> queue) {
		this.incomingMessages = inMessage;
		this.outgoingMessages = outMessage;
		this.myAddress = myAddress;
		this.workers = queue;
		this.messageToId = new HashMap<>();
		this.messageIdCounter = 0;
		this.shutdown = false;
		this.logger = initializeLogging(RoundRobinLeader.class.getCanonicalName() + "-myAddress-" + this.myAddress.getPort());
	}
	
	@Override
	public void run() {
		while (!this.shutdown) {
			Message message = null;
			while ((message = incomingMessages.poll()) == null) {
				// while there are no messages, keep polling
				// break when there is a message
			}
			if (message.getMessageType() == MessageType.WORK) {
				// send to worker
				InetSocketAddress workerAddress = this.workers.poll();
				long id = this.messageIdCounter;
				this.logger.info("Message: " + id + ", Recieved from Leader Address: " + this.myAddress);
				Message newMessage = new Message(
					MessageType.WORK,
					message.getMessageContents(),
					this.myAddress.getHostName(),
					this.myAddress.getPort(),
					workerAddress.getHostName(),
					workerAddress.getPort(),
					id
				);
				this.outgoingMessages.offer(newMessage);
				this.workers.offer(workerAddress);
				this.logger.info("Message: " + id + ", Sent from Leader Address: " + this.myAddress);
				// set message ID:
				InetSocketAddress isa = new InetSocketAddress(message.getSenderHost(), message.getSenderPort());
				messageToId.put(id, isa);
				this.messageIdCounter++;
			}
			else if (message.getMessageType() == MessageType.COMPLETED_WORK) {
				// send to client
				this.logger.info("Message: " + message.getRequestID() + ", Recieved from Leader Address: " + this.myAddress);
				InetSocketAddress isa = this.messageToId.get(message.getRequestID());
				Message newMessage = new Message(
					MessageType.COMPLETED_WORK,
					message.getMessageContents(),
					this.myAddress.getHostName(),
					this.myAddress.getPort(),
					isa.getHostName(),
					isa.getPort()
				);
				this.outgoingMessages.offer(newMessage);
				this.logger.info("Message: " + message.getRequestID() + ", Sent from Leader Address: " + this.myAddress);
			}
		}
	}

	void shutdown() {
		this.shutdown = true;
		this.logger.info("Server Threads Shutdown");
	}

}