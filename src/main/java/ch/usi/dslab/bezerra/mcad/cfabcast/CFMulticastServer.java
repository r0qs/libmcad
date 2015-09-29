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
 * @author Rodrigo Q. Saramago - rod@comp.ufu.br
 */

package ch.usi.dslab.bezerra.mcad.cfabcast;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.netwrapper.Message;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.contrib.pattern.ClusterClient;
import akka.japi.Procedure;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cfabcast.messages.*;
import cfabcast.serialization.CFABCastSerializer;

public class CFMulticastServer implements MulticastServer {
  private final ActorRef multicaster;
  private static Config config;
  private static ActorSystem system;
  private static BlockingQueue<Message> receivedReplies;
  static CFABCastSerializer serializer;
	private final int serverId;
	protected static Map<int, ActorRef> connectedClients;
	protected static Map<ActorRef, int> connectedClientsIds;

  public MulticastServer(int serverId) {
		this.serverId = serverId;
    this.receivedReplies = new LinkedBlockingQueue<Message>();
    this.connectedClients = new ConcurrentHashMap<int, ActorRef>();
    this.connectedClientsIds = new ConcurrentHashMap<ActorRef, int>();
    this.config = ConfigFactory.parseString("akka.cluster.roles = [server]")
      .withFallback(ConfigFactory.load("server"));
    this.system = ActorSystem.create("BenchServer", config);
    this.serializer = new CFABCastSerializer((ExtendedActorSystem) system);
 
    Set<ActorSelection> initialContacts = new HashSet<ActorSelection>();
    for (String contactAddress : config.getStringList("contact-points")) {
      initialContacts.add(system.actorSelection(contactAddress + "/user/receptionist"));
    }

    final ActorRef clusterClient = system.actorOf(ClusterClient.defaultProps(initialContacts), "clusterClient");
    multicaster = system.actorOf(Multicaster.props(clusterClient, serverId), String.format("server-%d", serverId));
  }

  static public class Multicaster extends UntypedActor {
    private final ActorRef clusterClient;
    private final int sid;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorRef multicastAgent;

    public static Props props(ActorRef clusterClient, int sid) {
      return Props.create(Multicaster.class, clusterClient, sid);
    }

    public Multicaster(ActorRef clusterClient, int sid) {
      this.clusterClient = clusterClient;
      this.sid = sid;
      this.multicastAgent = getContext().watch(getContext()
        .actorOf(MulticastAgent.props(clusterClient, true), "multicastAgent"));
    }

    @Override
    public void onReceive(Object message) throws Exception {
      if(message instanceof Register) {
        Register reg = (Register) message;
        int clientId = reg.getId();
        ActorRef client = getSender();
        getContext().watch(client);
        connectedClients.put(clientId, client);
        connectedClientsIds.put(client, clientId); //inverted to easily remove when terminate
        log.info("Server {} receive Register from {} ID: {}", sid, client, clientId);
        getContext().become(active, true);

      } else {
        log.info("Server {} not ready yet", sid);

      }
    }

    Procedure<Object> active = new Procedure<Object>() {
      @Override
      public void apply(Object message) {
        if(message instanceof Register) {
          Register reg = (Register) message;
          int clientId = reg.getId();
          ActorRef client = getSender();
          getContext().watch(client);
          connectedClients.put(clientId, client);
          log.info("Server {} receive Register from {} ID: {}", sid, client, clientId);

        } else if (message instanceof Terminated) {
          final Terminated t = (Terminated) message;
          ActorRef deadClient = t.getActor();
          if (connectedClientsIds.containsKey(deadClient)) {
            int id = connectedClientsIds.get(deadClient);
            connectedClients.remove(clientId);
            connectedClientsIds.remove(deadClient);
            // Stop server if there's no more client online?
            //getContext().stop(getSelf());
          } else {
            log.warning("Client {} not register on {}.", deadClient, getSelf());
          }
        
        // Message received from some learner in the cluster
        } else if(message instanceof Delivery) {
          Delivery response = (Delivery) message;
          Message msg = (Message) serializer.fromBinary(response.getData());
          log.info("Server {} receive response: {} from {} ", getSelf(), msg, getSender());
          receivedReplies.add(msg);

        } else {
          log.info("Server receive unknown message from {}", getSender());
          unhandled(message);
        } 
      }
    };
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
    ActorRef client = connectedClients.get(clientId);
    client.tell(reply, multicaster);
	}

	@Override
	public ClientMessage deliverClientMessage() {
	  try {
			return (ClientMessage) receivedReplies.take();
		} catch (InterruptedException e) {
      System.out.println("INTERRUPTED EXCEPTION CATCH!");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
}
