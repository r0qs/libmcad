package ch.usi.dslab.bezerra.mcad.spread;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import spread.SpreadException;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.mcad.spread.SpreadMulticastAgent.ProcessInfo;
import ch.usi.dslab.bezerra.mcad.uringpaxos.URPMulticastServer.MessageType;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPConnection;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPMessage;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPReceiver;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPSender;

public class SpreadMulticastClient implements MulticastClient, Runnable {

	private final SpreadMulticastAgent spreadAgent;
	private final int clientId;

	private TCPSender clientTCPSender;
	private TCPReceiver clientTCPReceiver;
	private BlockingQueue<Message> receivedReplies;

	public SpreadMulticastClient(SpreadMulticastAgent spreadAgent, int clientId) {
		this.spreadAgent = spreadAgent;
		this.clientId = clientId;
		
		clientTCPSender = new TCPSender();
		clientTCPReceiver = new TCPReceiver();
		receivedReplies = new LinkedBlockingQueue<Message>();

		Thread t = new Thread(this, "ClientReceiver");
		t.setDaemon(true);
		t.start();
	}

	@Override
	public void connectToServer(int serverId) {
		try {
			ProcessInfo server = spreadAgent.getProcessInfo(serverId);

			// Open connection to the daemon
			spreadAgent.connectToDaemonAsMulticaster(server);
			System.out.println("Client: trying to connecto to server at " +
					server.getHostName() + ":" + server.getServerPort());

			// Open connection with the server to receive responses
			TCPConnection serverConnection =
					new TCPConnection(server.getHostName(), server.getServerPort());
			clientTCPReceiver.addConnection(serverConnection);
			server.setServerConnection(serverConnection);

			// Rendez-vous with the server
			Message credentials = new Message(MessageType.CLIENT_CREDENTIALS, clientId);
			clientTCPSender.send(credentials, serverConnection);

			Message ack = deliverReply();
			System.out.println("Client [" + clientId + "]: " + ack.getItem(0) +
					" from " + server.getHostName() + "(pid: " + serverId + ")");
		} catch (SpreadException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connectToOneServerPerPartition() {
		List<Group> groups = Group.getAllGroups();
		for (Group group : groups) {
			List<Integer> groupMembers = group.getMembers();
			int chosenServerId = groupMembers.get(clientId % groupMembers.size());
			connectToServer(chosenServerId);
			spreadAgent.setServerForGroup(group.getId(), chosenServerId);
		}
	}

	@Override
	public void multicast(List<Group> destinations, ClientMessage clientMessage) {
		spreadAgent.multicast(destinations, clientMessage);
	}

	@Override
	public void run() {
		while(true) {
			TCPMessage tcpmsg = null;
			while (tcpmsg == null) {
				tcpmsg = clientTCPReceiver.receive(1000);
			}
	         
			Message msg = tcpmsg.getContents();
			receivedReplies.add(msg);
		}
	}

	@Override
	public Message deliverReply() {
		try {
			return receivedReplies.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

}
