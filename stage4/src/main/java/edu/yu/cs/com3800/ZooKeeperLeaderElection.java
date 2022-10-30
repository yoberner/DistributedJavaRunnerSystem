package edu.yu.cs.com3800;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.yu.cs.com3800.Message.MessageType;
import edu.yu.cs.com3800.ZooKeeperPeerServer.ServerState;

public class ZooKeeperLeaderElection {
    /**
     * time to wait once we believe we've reached the end of leader election.
     */
    private final static int finalizeWait = 200;
    /**
     * Upper bound on the amount of time between two consecutive notification
     * checks. This impacts the amount of time to get the system up again after long
     * partitions. Currently 60 seconds.
     */
    private final static int maxNotificationInterval = 60000;

    private int currentWait = finalizeWait;
    private LinkedBlockingQueue<Message> incomingMessages;
    private ZooKeeperPeerServer myPeerServer;
    private long proposedLeader;
    private long proposedEpoch;
    private Map<Long, ElectionNotification> idToNotification = new HashMap<Long, ElectionNotification>();

    public ZooKeeperLeaderElection(ZooKeeperPeerServer server, LinkedBlockingQueue<Message> incomingMessages) {
        this.incomingMessages = incomingMessages;
        this.myPeerServer = server;
    }

    private synchronized Vote getCurrentVote() {
        return new Vote(this.proposedLeader, this.proposedEpoch);
    }

    public synchronized Vote lookForLeader() {
        this.proposedLeader = this.myPeerServer.getServerId();
        this.proposedEpoch = this.myPeerServer.getPeerEpoch();
        //send initial notifications to other peers to get things started
        sendNotifications();
        //Loop, exchanging notifications with other servers until we find a leader
        while (this.myPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.LOOKING || this.myPeerServer.getPeerState() == ZooKeeperPeerServer.ServerState.OBSERVER) {
            
            //Remove next notification from queue, timing out after 2 times the termination time
            Message message = pollMessage();
            //if no notifications received..
            if (message == null) {
                this.currentWait = Math.min(this.currentWait * 2, maxNotificationInterval);
                //.and implement exponential back-off when notifications not received..
                //..resend notifications to prompt a reply from others..
                sendNotifications();
                continue;
            }
            //if/when we get a message and it's from a valid server and for a valid server..
            ElectionNotification notification = getNotificationFromMessage(message);
            this.idToNotification.put(notification.getSenderID(), notification);
            //switch on the state of the sender:
            switch (notification.getState()) {
                case LOOKING: //if the sender is also looking
                    //if the received message has a vote for a leader which supersedes mine, change my vote and tell all my peers what my new vote is.
                    if (supersedesCurrentVote(notification.getProposedLeaderID(), notification.getPeerEpoch())) {
                        this.proposedLeader = notification.getProposedLeaderID();
                        this.proposedEpoch = notification.getPeerEpoch();
                        sendNotifications();
                    }
                    //keep track of the votes I received and who I received them from. - done above
                    //if I have enough votes to declare my currently proposed leader as the leader:
                    if (this.haveEnoughVotes(idToNotification, this.getCurrentVote())) {
                        Message newMessage = pollMessage(); //todo finilize time
                        //first check if there are any new votes for a higher ranked possible leader before I declare a leader. If so, continue in my election loop
                        while (newMessage != null) {
                            ElectionNotification newNotification = getNotificationFromMessage(newMessage);
                            if (supersedesCurrentVote(newNotification.getProposedLeaderID(), newNotification.getPeerEpoch())) {
                                try {
                                    this.incomingMessages.put(newMessage);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            newMessage = pollMessage(); //todo finilize time
                        }
                        //If not, set my own state to either LEADING (if I won the election) or FOLLOWING (if someone lese won the election) and exit the election
                        if (newMessage == null) {
                            return acceptElectionWinner(notification);
                        }
                    }
                    break;
                case FOLLOWING:
                case LEADING: //if the sender is following a leader already or thinks it is the leader
                    //IF: see if the sender's vote allows me to reach a conclusion based on the election epoch that I'm in, i.e. it gives the majority to the vote of the FOLLOWING or LEADING peer whose vote I just received.
                    if (this.haveEnoughVotes(this.idToNotification, notification)) {
                        //if so, accept the election winner.
                        return acceptElectionWinner(notification);
                    }
                    // TODO: implement:
                        //As, once someone declares a winner, we are done. We are not worried about / accounting for misbehaving peers.
                     //ELSE: if n is from a LATER election epoch
                        //IF a quorum from that epoch are voting for the same peer as the vote of the FOLLOWING or LEADING peer whose vote I just received.
                           //THEN accept their leader, and update my epoch to be their epoch
                        //ELSE:
                            //keep looping on the election loop.    
                    break;
                case OBSERVER:
                    continue;
            default:
                break;
            }
        }
        return null;
    }

    private Message pollMessage() {
        Message message = null;
        try {
			message = this.incomingMessages.poll(this.currentWait, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        return message;
    }

    private void sendNotifications() {
        ElectionNotification notification = new ElectionNotification(this.proposedLeader, this.myPeerServer.getPeerState(), this.myPeerServer.getServerId(), this.proposedEpoch);
        this.myPeerServer.sendBroadcast(MessageType.ELECTION, buildMsgContent(notification));
    }

    private Vote acceptElectionWinner(ElectionNotification n) {
        // set my state to either LEADING or FOLLOWING
        if (n.getProposedLeaderID() == this.myPeerServer.getServerId()) {
            this.myPeerServer.setPeerState(ServerState.LEADING);
        }
        else if (this.myPeerServer.getPeerState() == ServerState.OBSERVER) {
            // dont change state
        }
        else {
            this.myPeerServer.setPeerState(ServerState.FOLLOWING);
        }
        this.proposedLeader = n.getProposedLeaderID();
        // clear out the incoming queue before returning
        this.incomingMessages.clear();
        return n;
    }

    /**
     * We return true if one of the following three cases hold: 1- New epoch is
     * higher 2- New epoch is the same as current epoch, but server id is higher.
     */
    protected boolean supersedesCurrentVote(long newId, long newEpoch) {
        if (this.myPeerServer.getPeerState() == ServerState.OBSERVER && this.proposedLeader == this.myPeerServer.getServerId()) {
            return true;
        }
        return (newEpoch > this.proposedEpoch) || ((newEpoch == this.proposedEpoch) && (newId > this.proposedLeader));
    }

    /**
     * Termination predicate. Given a set of votes, determines if have sufficient
     * support for the proposal to declare the end of the election round. Who voted
     * for who isn't relevant, we only care that each server has one current vote
     */
    protected boolean haveEnoughVotes(Map<Long, ElectionNotification> votes, Vote proposal) {
        // is the number of votes for the proposal > the size of my peer server’s quorum?
        int counter = 1;
        for (ElectionNotification n : this.idToNotification.values()) {
            if (n.getProposedLeaderID() == proposal.getProposedLeaderID() && n.getState() != ServerState.OBSERVER) {
                counter++;
            }
        }
        return counter >= this.myPeerServer.getQuorumSize();
    }

    public static byte[] buildMsgContent(ElectionNotification notification) {
        ByteBuffer bf = ByteBuffer.allocate(Long.BYTES*3 + Character.BYTES);
        bf.putLong(notification.getProposedLeaderID());
        bf.putChar(notification.getState().getChar());
        bf.putLong(notification.getSenderID());
        bf.putLong(notification.getPeerEpoch());
        return bf.array();
    }

    public static ElectionNotification getNotificationFromMessage(Message received) {
        ByteBuffer msgBytes = ByteBuffer.wrap(received.getMessageContents());
        long leader = msgBytes.getLong();
        char stateChar = msgBytes.getChar();
        long senderID = msgBytes.getLong();
        long peerEpoch = msgBytes.getLong();
        ElectionNotification notification = new ElectionNotification(leader, ZooKeeperPeerServer.ServerState.getServerState(stateChar), senderID, peerEpoch);
        return notification;
    }

    // private ElectionNotification receiveNotification() {
    //     Message message = null;
    //     try {
	// 		message = this.incomingMessages.poll(finalizeWait, TimeUnit.MILLISECONDS);
	// 	} catch (InterruptedException e) {
	// 		e.printStackTrace();
	// 	}
    //     ElectionNotification notification = getNotificationFromMessage(message);
    //     return notification;
    // }

}