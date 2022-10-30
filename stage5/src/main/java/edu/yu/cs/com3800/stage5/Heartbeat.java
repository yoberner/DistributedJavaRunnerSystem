package edu.yu.cs.com3800.stage5;

import java.util.logging.*;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import edu.yu.cs.com3800.*;
import edu.yu.cs.com3800.Message.MessageType;
import edu.yu.cs.com3800.ZooKeeperPeerServer.ServerState;

public class Heartbeat extends Thread implements LoggingServer {
	private final static int GOSSIP = 4000;
	private final static int FAIL = GOSSIP * 10;
	private final static int CLEANUP = FAIL * 2;

	private Map<Long, InetSocketAddress> peerIDtoAddress;
	private LinkedBlockingQueue<Message> incomingMessages;
	private LinkedBlockingQueue<Message> outgoingMessages;
	private ZooKeeperPeerServerImpl myPeerServer;
	private Logger logger;
	private ConcurrentHashMap<Long, HeartbeatValues> heartbeatsTable;
	private boolean shutdown = false;
	private HeartbeatValues myHeartbeat;
	private Random random;
	private Set<Long> failedPeers = ConcurrentHashMap.newKeySet();

	public Heartbeat(Map<Long,InetSocketAddress> peerIDtoAddress, LinkedBlockingQueue<Message> incomingMessages, LinkedBlockingQueue<Message> outgoingMessages, ZooKeeperPeerServerImpl myPeerServer) {
		this.peerIDtoAddress = peerIDtoAddress;
		this.incomingMessages = incomingMessages;
		this.outgoingMessages = outgoingMessages;
		this.myPeerServer = myPeerServer;
		this.logger = initializeLogging(Heartbeat.class.getCanonicalName() + "Hearbeat for Server: " + this.myPeerServer);
		this.heartbeatsTable = new ConcurrentHashMap<Long, HeartbeatValues>();
		this.random = new Random();
		// fill heartbeats table with all peers
		for (long serverID : this.peerIDtoAddress.keySet()) {
			this.heartbeatsTable.put(serverID, new HeartbeatValues(serverID, System.currentTimeMillis(), 0));
		}
	}

	private long getRandomNumber() {
		int max = this.heartbeatsTable.size();
		long randomNum;
		while ((randomNum = this.random.nextInt(max)) == this.myPeerServer.getServerId() || this.failedPeers.contains(randomNum)) {
			// keep generating random numbers until we get one that is not our id or one that has failed
		}
		return randomNum;
	}

	@Override
	public void run() {
		this.myHeartbeat = new HeartbeatValues(this.myPeerServer.getServerId(), System.currentTimeMillis(), 0);
		this.heartbeatsTable.put(this.myPeerServer.getServerId(), this.myHeartbeat);
		long id = getRandomNumber();
		this.myPeerServer.sendMessage(MessageType.GOSSIP, buildMsgContent(this.heartbeatsTable), this.peerIDtoAddress.get(id));
		logger.info("Server has sent message to server " + id);

		while (!this.shutdown) {
			if (System.currentTimeMillis() - this.myHeartbeat.lastTime >= GOSSIP) {
				this.myHeartbeat.lastTime = System.currentTimeMillis();
				this.myHeartbeat.heartbeat++;
				this.heartbeatsTable.put(this.myHeartbeat.serverId, this.myHeartbeat);
				// send message - offer to queue: (send to random server)
				long id2 = getRandomNumber();
				this.myPeerServer.sendMessage(MessageType.GOSSIP, buildMsgContent(this.heartbeatsTable), this.peerIDtoAddress.get(id2));
				logger.info("Server has sent message to server " + id2);
			}

			Message message = this.incomingMessages.poll();

			if (message != null) {

				// process message:

				// get hashmap from message then loop through my hearbeat table of values (ie iterate through my hashmap).
				// if the current value is not in the map from the message, and myMap.Val.id.getTime+FAIL > currentTime then that server failed. remove it from my table and add it to the failed list. if it was a leader then start new leader election.
				// if myPeerServer.getLeader is this server id thats how we know it was leader.
				//or if the current value is in the map: if the new maps heartbeat is greater than the hearbeat i have for this server, then update that current hearbeat value in my table as well.
				// else if its less than that value, then need to determine if failed. - same as before

				ConcurrentHashMap<Long, HeartbeatValues> receivedHeartbeats = getNotificationFromMessage(message);
				// System.out.println("Received Heartbeats: " + receivedHeartbeats.toString());
				for (HeartbeatValues heartbeatValue : this.heartbeatsTable.values()) {
					if (heartbeatValue.lastTime + FAIL < System.currentTimeMillis()) {
						this.failedPeers.add(heartbeatValue.serverId);
						this.heartbeatsTable.remove(heartbeatValue.serverId);
						if (this.myPeerServer.getCurrentLeader().getProposedLeaderID() == heartbeatValue.serverId) {
							if (this.myPeerServer.getPeerState() == ServerState.FOLLOWING) {
								this.myPeerServer.setPeerState(ServerState.LOOKING);
							}
							this.myPeerServer.incrementEpoch();
							try {
								this.myPeerServer.setCurrentLeader(new Vote(this.myPeerServer.getServerId(), this.myPeerServer.getPeerEpoch()));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						System.out.println("Server " + heartbeatValue.serverId + " has failed.");
						this.logger.info("Server " + heartbeatValue.serverId + " has failed.");
					}
					else if (heartbeatValue.lastTime + FAIL < System.currentTimeMillis()) {
						this.failedPeers.add(heartbeatValue.serverId);
						this.heartbeatsTable.remove(heartbeatValue.serverId);
						if (this.myPeerServer.getCurrentLeader().getProposedLeaderID() == heartbeatValue.serverId) {
							if (this.myPeerServer.getPeerState() == ServerState.FOLLOWING) {
								this.myPeerServer.setPeerState(ServerState.LOOKING);
							}
							this.myPeerServer.incrementEpoch();
							try {
								this.myPeerServer.setCurrentLeader(new Vote(this.myPeerServer.getServerId(), this.myPeerServer.getPeerEpoch()));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						System.out.println("Server " + heartbeatValue.serverId + " has failed.");
						logger.info("Server " + heartbeatValue.serverId + " has failed.");
					}
					else if (receivedHeartbeats.containsKey(heartbeatValue.serverId) && heartbeatValue.heartbeat < receivedHeartbeats.get(heartbeatValue.serverId).heartbeat) {
						logger.info("Server " + heartbeatValue.serverId + " has been updated.");
						// heartbeatValue.lastTime = System.currentTimeMillis();
						// heartbeatValue.heartbeat = receivedHeartbeats.get(heartbeatValue.serverId).heartbeat;
						HeartbeatValues newHeartbeat = new HeartbeatValues(heartbeatValue.serverId, System.currentTimeMillis(), receivedHeartbeats.get(heartbeatValue.serverId).heartbeat);
						this.heartbeatsTable.put(heartbeatValue.serverId, newHeartbeat);
					}
				}
			}
		}
	}

	public static byte[] buildMsgContent(ConcurrentHashMap<Long,HeartbeatValues> map) {
        ByteBuffer bf = ByteBuffer.allocate(((Long.BYTES*2) + (Integer.BYTES)) * map.size());
		for (HeartbeatValues hbv : map.values()) {
			bf.putLong(hbv.serverId);
			bf.putLong(hbv.lastTime);
			bf.putInt(hbv.heartbeat);
		}
        return bf.array();
    }

    public static ConcurrentHashMap<Long,HeartbeatValues> getNotificationFromMessage(Message received) {
		ConcurrentHashMap<Long, HeartbeatValues> map = new ConcurrentHashMap<Long, HeartbeatValues>();
        ByteBuffer msgBytes = ByteBuffer.wrap(received.getMessageContents());
		while (msgBytes.hasRemaining()) {
			long serverId = msgBytes.getLong();
			long lastTime = msgBytes.getLong();
			int heartbeat = msgBytes.getInt();
			HeartbeatValues hbv = new HeartbeatValues(serverId, lastTime, heartbeat);
			map.put(serverId, hbv);
		}
        return map;
    }

	void shutdown() {
		this.interrupt();
		this.shutdown = true;
		this.logger.info("Heartbeat Thread Shutdown");
	}

	public static class HeartbeatValues {
		public long serverId;
		public long lastTime;
		public int heartbeat;

		public HeartbeatValues(long serverId, long lastTime, int heartbeat) {
			this.serverId = serverId;
			this.lastTime = lastTime;
			this.heartbeat = heartbeat;
		}

		@Override
		public int hashCode() {
			return Objects.hash(heartbeat, lastTime, serverId);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HeartbeatValues other = (HeartbeatValues) obj;
			return heartbeat == other.heartbeat && lastTime == other.lastTime && serverId == other.serverId;
		}

		@Override
		public String toString() {
			return "HeartbeatValues [heartbeat=" + heartbeat + ", lastTime=" + lastTime + ", serverId=" + serverId + "]";
		}
	}

}