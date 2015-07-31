/*

 Libmcad - A multicast adaptor library
 Copyright (C) 2015, University of Lugano
 
 This file is part of Libmcad.
 
 Libmcad is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libmcad is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 
*/

/**
 * @author Daniel Cason - daniel.cason@usi.ch
 */

package ch.usi.dslab.bezerra.mcad.spread;

import java.io.FileReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import spread.MembershipInfo;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;
import ch.usi.dslab.bezerra.mcad.DeliveryMetadata;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastCheckpoint;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.netwrapper.tcp.TCPConnection;

public class SpreadMulticastAgent implements MulticastAgent {

	public static final int DEFAULT_DAEMON_PORT	= 4803;
	public static final int DEFAULT_SERVER_PORT	= 8888;

	private final int processId;
	private ArrayList<ProcessInfo> processes;
	private HashMap<Integer, ProcessInfo> serversPerGroup;

	public SpreadMulticastAgent(String configFile, boolean isReceiver,
			int processId) throws SpreadException, IOException {
		this.processId = processId;

		processes = new ArrayList<ProcessInfo>();
		serversPerGroup = new HashMap<Integer, ProcessInfo>();

		System.out.println("Init: agent with id: " + processId + ", "
				+ (isReceiver?"receiver":"sender"));
		loadProcessesAndGroups(configFile);

		if (isReceiver) {
			// FIXME: when called by MulticastClientServerFactory the
			// group id can not be passed to this constructor...
			int groupId = groupIdForProcess(processId);
			Group group = Group.getGroup(groupId);
			if (! group.getMembers().contains(processId)) {
				for (groupId = 0; groupId < nGroups; groupId++) {
					group = Group.getGroup(groupId);
					if (group.getMembers().contains(processId)) {
						break;
					}
				}
			}
			connectToDaemonAsReceiver(getProcessInfo(processId), groupId);
		}
	}

	/////////////////////// RECEIVER METHODS ///////////////////////

	private SpreadConnection receiverConnection;
	private SpreadMGroup localGroup;

	private void connectToDaemonAsReceiver(ProcessInfo process, int groupId)
			throws SpreadException, IOException {
		receiverConnection = new SpreadConnection();
		receiverConnection.connect(InetAddress.getByName(process.getHostName()),
				process.getDaemonPort(), "R" + processId, false, true);

		//System.out.println("Agent: connected to " + process.getHostName() +
		//		":" + process.getDaemonPort() + ", private group = " +
		//		receiverConnection.getPrivateGroup().toString());

		SpreadGroup spreadGroup = new SpreadGroup();
		localGroup = (SpreadMGroup) Group.getGroup(groupId);
		String spreadGroupName = SpreadMGroup.getSpreadName(localGroup);
		spreadGroup.join(receiverConnection, spreadGroupName);

		System.out.println("Agent [" + processId + "]: joined to group " +
				localGroup.getId() + " (" + spreadGroup.toString() + ")");
	}

	@Override
	public Group getLocalGroup() {
		return localGroup;
	}

	@Override
	public Message deliverMessage() {
		while (true) {
			try {
				SpreadMessage message = receiverConnection.receive();
				if (message.isMembership()) {
					MembershipInfo info = message.getMembershipInfo();
					if (info.isRegularMembership()) {
						String str = new String();
						for (SpreadGroup group : info.getMembers()) {
							if (! str.isEmpty()) {
								str += ", ";
							}
							str += group.toString();
						}
						System.out.println("Agent: group " + info.getGroup()
								+ " membership changed to [" + str + "]");
					}
				} else if (message.isRegular() && message.isSafe()) {
					return (Message) message.getObject();
				}
			} catch (InterruptedIOException | SpreadException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	/////////////////////// MULTICASTER METHODS ///////////////////////

	private int connectedMulticasters = 0;

	public void connectToDaemonAsMulticaster(ProcessInfo process)
			throws IOException, SpreadException {
		String privateName = new String("C" + processId);
		privateName += "-" + (++connectedMulticasters);

		SpreadConnection connection = new SpreadConnection();
		connection.connect(InetAddress.getByName(process.getHostName()),
				process.getDaemonPort(), privateName, false, false);
		process.setSpreadConnection(connection);

		//System.out.println("Agent [" + processId + "]: connected to " +
		//		process.getHostName() + ":" + process.getDaemonPort());
	}

	public void setServerForGroup(int groupId, int serverId) {
		ProcessInfo server = getProcessInfo(serverId);
		serversPerGroup.put(groupId, server);
		//System.out.println("Agent [" + processId + "]: primary server for"
		//		+" group " + groupId + " set to process " + serverId);
	}

	@Override
	public void multicast(Group single_destination, Message message) {
		List<Group> dests = new ArrayList<Group>(1);
		dests.add(single_destination);
		multicast(dests, message);
	}

	@Override
	public void multicast(List<Group> destinations, Message message) {
		try {
			SpreadMessage spreadMessage = new SpreadMessage();
			ProcessInfo destination = null;
			for (Group group : destinations) {
				if (destination == null) {
					destination = serversPerGroup.get(group.getId());
				}
				spreadMessage.addGroup(SpreadMGroup.getSpreadName(group));
			}
			spreadMessage.setSafe();
			spreadMessage.setObject(message);

			if (destination.getSpreadConnection() == null) {
				connectToDaemonAsMulticaster(destination);
			}

			// FIXME: it is a blocking call: a Multicaster thread is needed?
			destination.getSpreadConnection().multicast(spreadMessage);
		} catch (SpreadException | IOException e) {
			e.printStackTrace();
		}

	}

	/////////////////////// CONFIGURATION METHODS ///////////////////////

	private int nProcesses = 0;
	private int nGroups = 1;

	@SuppressWarnings("unchecked")
	private void loadProcessesAndGroups(String filename) {
		try {
			JSONParser parser = new JSONParser();
			Object nodeObj = parser.parse(new FileReader(filename));
			JSONObject config = (JSONObject) nodeObj;
		
			// Only first process prints the configuration
			boolean verbose = (processId == 0);

			if (config.containsKey("number_of_groups")) {
				nGroups = ((Long) config.get("number_of_groups")).intValue();
			}

			ArrayList<SpreadMGroup> groups = new ArrayList<SpreadMGroup>(nGroups);
			groups.add(null); // groupId should start from 1
			for (int groupId = 1; groupId <= nGroups; groupId++) {
				groups.add(groupId, new SpreadMGroup(groupId));
			}
			if (verbose)
				System.out.println("Config:\tcreated " + nGroups + " groups.");

			ArrayList<Integer> processesWithoutGroup = new ArrayList<Integer>();

			JSONArray processesArray = (JSONArray) config.get("processes");
			Iterator<Object> it_process = processesArray.iterator();
			while (it_process.hasNext()) {
				JSONObject process = (JSONObject) it_process.next();
				int processId 		= ((Long) process.get("pid")).intValue();
				String hostName 	= (String) process.get("host");
				int serverPort		= DEFAULT_SERVER_PORT;
				if (process.containsKey("sport")) {
					serverPort = ((Long) process.get("sport")).intValue();
				}
				int daemonPort		= DEFAULT_DAEMON_PORT;
				if (process.containsKey("dport")) {
					daemonPort = ((Long) process.get("dport")).intValue();
				}

				if (verbose)
					System.out.println("Config:\tfound process " + processId +
							" at " + hostName + ":" + serverPort);

				ProcessInfo processInfo = getProcessInfo(processId);
				if (processInfo != null) {
					System.err.println("Ouch!! Duplicated process: " + processId);
				} else {
					while (processes.size() < processId) {
						processes.add(null);
					}
					nProcesses += 1;
				}
				processInfo = new ProcessInfo(hostName, serverPort, daemonPort);
				processes.add(processId, processInfo);

				if (process.containsKey("group")) {
					int groupId = ((Long) process.get("group")).intValue();
					SpreadMGroup group = groups.get(groupId);
					group.addProcess(processId);
					if (verbose)
						System.out.println("Config:\tprocess " + processId +
								" manually assigned to group " + groupId);

					// Primary server: First process assigned to a group
					if (! serversPerGroup.containsKey(groupId)) {
						setServerForGroup(group.getId(), processId);
					}
				} else {
					processesWithoutGroup.add(processId);
				}
			}

			while (processesWithoutGroup.size() > 0) {
				int processId = processesWithoutGroup.remove(0);
				int groupId = groupIdForProcess(processId);
				SpreadMGroup group = groups.get(groupId);
				group.addProcess(processId);
				if (verbose)
					System.out.println("Config:\tprocess " + processId +
							" assigned to group " + groupId);

				// Primary server: First process assigned to a group
				if (! serversPerGroup.containsKey(group.getId())) {
					setServerForGroup(group.getId(), processId);
				}
			}

			if (verbose)
				System.out.println("Init: loaded " + nProcesses + " processes.");
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// assert(nProcess >= nGroups)
	private int groupIdForProcess(int processId) {
		int processesPerGroup = nProcesses / nGroups;
		int groupId = Math.min(nGroups - 1, processId / processesPerGroup);
		return groupId + 1;
	}

	public ProcessInfo getProcessInfo(int processId) {
		ProcessInfo process = null;
		if (processId >= 0 && processId < processes.size()) {
			process = processes.get(processId);
		}
		return process;
	}

	public class ProcessInfo {

		private final int severPort;
		private final int daemonPort;
		private final String hostName;
		private TCPConnection serverConnection;
		private SpreadConnection spreadConnection;

		public ProcessInfo(String hostName, int serverPort, int daemonPort) {
			this.hostName = hostName;
			this.severPort = serverPort;
			this.daemonPort = daemonPort;

			serverConnection = null;
			spreadConnection = null;
		}

		public String getHostName() {
			return hostName;
		}

		public int getServerPort() {
			return severPort;
		}

		public int getDaemonPort() {
			return daemonPort;
		}

		public TCPConnection getServerConnection() {
			return serverConnection;
		}

		public SpreadConnection getSpreadConnection() {
			return spreadConnection;
		}

		public void setServerConnection(TCPConnection connection) {
			serverConnection = connection;
		}

		public void setSpreadConnection(SpreadConnection connection) {
			spreadConnection = connection;
		}

	}

   @Override
   public boolean hasWholeDeliveryPreffix() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void notifyCheckpointMade(DeliveryMetadata deliveryToKeep) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean provideMulticastCheckpoint(MulticastCheckpoint checkpoint) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public MulticastCheckpoint createMulticastCheckpoint(
         DeliveryMetadata lastDelivery) {
      // TODO Auto-generated method stub
      return null;
   }

}
