package ch.usi.dslab.bezerra.mcad.spread;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.mcad.spread.SpreadMulticastAgent.ProcessInfo;
import ch.usi.dslab.bezerra.mcad.uringpaxos.URPMulticastServer.MessageType;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPConnection;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPMessage;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPReceiver;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPSender;

public class SpreadMulticastServer implements MulticastServer {

	private final SpreadMulticastAgent spreadAgent;
	private final int serverId;

	protected TCPSender serverTcpSender;
	protected TCPReceiver serverTcpReceiver;
	protected Map<Integer, TCPConnection> connectedClients;

	public SpreadMulticastServer(SpreadMulticastAgent spreadAgent, int serverId) {
		this.spreadAgent = spreadAgent;
		this.serverId = serverId;

		connectedClients = new ConcurrentHashMap<Integer, TCPConnection>();
		serverTcpSender = new TCPSender();

		ProcessInfo processInfo = spreadAgent.getProcessInfo(serverId);
	    serverTcpReceiver = new TCPReceiver(processInfo.getServerPort());
	    // System.out.println("Server: listening at port " + processInfo.getServerPort());

		Thread listener = new Thread(new ConnectionListener(), "Connection Listener");
		listener.setDaemon(true);
		listener.start();
	}

	@Override
	public int getId() {
		return serverId;
	}

	@Override
	public boolean isConnectedToClient(int clientId) {
		return connectedClients.containsKey(clientId);
	}

	@Override
	public void sendReply(int clientId, Message reply) {
		TCPConnection clientConnection = connectedClients.get(clientId);
		if (clientConnection != null) {
			serverTcpSender.send(reply, clientConnection);
		}
	}

	@Override
	public MulticastAgent getMulticastAgent() {
		return spreadAgent;
	}

	@Override
	public ClientMessage deliverClientMessage() {
		Message msg = spreadAgent.deliverMessage();
		if (msg instanceof ClientMessage) {
			return (ClientMessage) msg;
		} else {
			System.err.println("msg not instance of ClientMessage");
			System.exit(1);
			return null;
		}
	}

	public class ConnectionListener implements Runnable {

		@Override
		public void run() {
			while (true) {
				TCPMessage request = serverTcpReceiver.receive(1000);
				if (request != null) {
					// System.out.println("Listener: new connection received");
					TCPConnection connection = request.getConnection();
					Message contents = request.getContents();
					contents.rewind();
					int msgType = (Integer) contents.getNext();

					if (msgType == MessageType.CLIENT_CREDENTIALS) {
						int clientId = (Integer) contents.getNext();
						// System.out.println("Listener: client credentials: " +
						// clientId);
						connectedClients.put(clientId, connection);
						Message connectedAck = new Message("CONNECTED");
		                sendReply(clientId, connectedAck);
					}	
				}
			}
		}
	}
}
