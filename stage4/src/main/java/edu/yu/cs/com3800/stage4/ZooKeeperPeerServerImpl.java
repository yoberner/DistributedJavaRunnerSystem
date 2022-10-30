package edu.yu.cs.com3800.stage4;

import edu.yu.cs.com3800.*;
import edu.yu.cs.com3800.Message.MessageType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

// A number of instances of this class running simultaneously will form a ZooKeeper cluster.
public class ZooKeeperPeerServerImpl extends Thread implements ZooKeeperPeerServer {
	private final InetSocketAddress myAddress;
	private final int myPort;
	private ServerState state;
	private volatile boolean shutdown;
	private LinkedBlockingQueue<Message> outgoingMessages;
	private LinkedBlockingQueue<Message> incomingMessages;
	private Long id;
	private long peerEpoch;
	private volatile Vote currentLeader;
	private Map<Long, InetSocketAddress> peerIDtoAddress;
	private boolean leaderOrFollowerExists;

	private UDPMessageSender senderWorker;
	private UDPMessageReceiver receiverWorker;

	private Logger logger;
	private RoundRobinLeader leader;
	private JavaRunnerFollower follower;

	public ZooKeeperPeerServerImpl(int myPort, long peerEpoch, Long id, Map<Long, InetSocketAddress> peerIDtoAddress) {
		this.myPort = myPort;
		this.peerEpoch = peerEpoch;
		this.id = id;
		this.peerIDtoAddress = peerIDtoAddress;
		this.myAddress = new InetSocketAddress("localhost", myPort);
		this.state = ServerState.LOOKING;
		this.shutdown = false;
		this.outgoingMessages = new LinkedBlockingQueue<Message>();
		this.incomingMessages = new LinkedBlockingQueue<Message>();
		this.currentLeader = new Vote(id, peerEpoch);
		// this.peerIDtoAddress.remove(id);
		this.logger = initializeLogging(ZooKeeperPeerServerImpl.class.getCanonicalName() + "-port#-" + this.myPort);
		this.leaderOrFollowerExists = false;
	}

	@Override
	public void shutdown() {
		this.shutdown = true;
		this.senderWorker.shutdown();
		this.receiverWorker.shutdown();
		if (this.leader != null) this.leader.shutdown();
		if (this.follower != null) this.follower.shutdown();
		this.logger.info("Server Threads Shutdown");
	}

	@Override
	public void run() {
		// step 1: create and run thread that sends broadcast messages
		this.senderWorker = new UDPMessageSender(this.outgoingMessages, this.myPort);
		this.senderWorker.start();
		// step 2: create and run thread that listens for messages sent to this server
		try {
			this.receiverWorker = new UDPMessageReceiver(this.incomingMessages, this.myAddress, this.myPort, this);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		this.receiverWorker.start();
		// step 3: main server loop
		try {
			while (!this.shutdown) {
				switch (getPeerState()) {
				case LOOKING:
					// start leader election, set leader to the election winner
					ZooKeeperLeaderElection zle = new ZooKeeperLeaderElection(this, this.incomingMessages);
					this.logger.info("Looking for leader...");
					setCurrentLeader(zle.lookForLeader());
					this.logger.info("Current Leader Set: " + this.currentLeader);
					break;
				case LEADING:
					if (this.leaderOrFollowerExists == true) {
						continue;
					}
					this.leaderOrFollowerExists = true;
					LinkedBlockingQueue<InetSocketAddress> queue = new LinkedBlockingQueue<>(this.peerIDtoAddress.values());
					RoundRobinLeader rrLeader = new RoundRobinLeader(
						this.incomingMessages,
						this.outgoingMessages,
						this.myAddress,
						queue
					);
					rrLeader.setDaemon(true);
					rrLeader.start();
					this.leader = rrLeader;
					break;
				case FOLLOWING:
					if (this.leaderOrFollowerExists == true) {
						continue;
					}
					this.leaderOrFollowerExists = true;
					JavaRunnerFollower jrFollower = new JavaRunnerFollower(
						this.incomingMessages,
						this.outgoingMessages,
						this.myAddress,
						this.peerIDtoAddress.get(this.getCurrentLeader().getProposedLeaderID())
					);
					jrFollower.setDaemon(true);
					jrFollower.start();
					this.follower = jrFollower;
					break;
				case OBSERVER:
					if (this.currentLeader.getProposedLeaderID() == this.id) {
						ZooKeeperLeaderElection zoole = new ZooKeeperLeaderElection(this, this.incomingMessages);
						setCurrentLeader(zoole.lookForLeader()); // run leader election
					}
					else {
						// dont change state, leader exists
					}
				default:
					break;
				}
			}
		} catch (Exception e) {
			// code...
		}
	}

	@Override
	public void setCurrentLeader(Vote v) throws IOException {
		this.currentLeader = v;
	}

	@Override
	public Vote getCurrentLeader() {
		return this.currentLeader;
	}

	@Override
	public void sendMessage(MessageType type, byte[] messageContents, InetSocketAddress target) throws IllegalArgumentException {
		Message message = new Message(
			type,
			messageContents,
			this.myAddress.getHostString(),
			this.myPort,
			target.getHostString(),
			target.getPort()
		);
		this.outgoingMessages.offer(message);
	}

	@Override
	public void sendBroadcast(MessageType type, byte[] messageContents) {
		for (InetSocketAddress isa : this.peerIDtoAddress.values()) {
			if (isa.equals(this.myAddress)) {
				continue;
			}
			sendMessage(type, messageContents, isa);
		}
	}

	@Override
	public ServerState getPeerState() {
		return this.state;
	}

	@Override
	public void setPeerState(ServerState newState) {
		this.state = newState;
	}

	@Override
	public Long getServerId() {
		return this.id;
	}

	@Override
	public long getPeerEpoch() {
		return this.peerEpoch;
	}

	@Override
	public InetSocketAddress getAddress() {
		return this.myAddress;
	}

	@Override
	public int getUdpPort() {
		return this.myPort;
	}

	@Override
	public InetSocketAddress getPeerByID(long peerId) {
		return this.peerIDtoAddress.get(peerId);
	}

	@Override
	public int getQuorumSize() {
		return (this.peerIDtoAddress.size() + 1) / 2 + 1;
	}

}