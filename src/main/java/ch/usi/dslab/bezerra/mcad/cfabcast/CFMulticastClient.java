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
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.netwrapper.Message;

import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static java.util.concurrent.TimeUnit.SECONDS;

import akka.actor.*;
import akka.contrib.pattern.ClusterClient;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import cfabcast.messages.*;

public class CFMulticastClient implements MulticastClient {
  
  private static Integer clientId;
  private final ActorRef multicaster;
  private static Config config;
  private static ActorSystem system;
  private static BlockingQueue<Message> receivedReplies;

  public CFMulticastClient(int clientId) {
    this.clientId = clientId;
    this.receivedReplies = new LinkedBlockingQueue<Message>();
    this.config = ConfigFactory.load(); 
    this.system = ActorSystem.create("BenchClient", config);

    Set<ActorSelection> initialContacts = new HashSet<ActorSelection>();
    //TODO Round Robin on cfabcast
    List<String> contactList = config.getStringList("contact-points");
    int chosenContact = clientId % contactList.size();
    System.out.println(String.format("CONTACTS: MyId: %d, Chosen: %d, Size: %d addr: %s", clientId, chosenContact, contactList.size(), contactList.get(chosenContact)));

    initialContacts.add(system.actorSelection(contactList.get(chosenContact) + "/user/receptionist"));
    final ActorRef clusterClient = system.actorOf(ClusterClient.defaultProps(initialContacts), "clusterClient");
    this.multicaster = system.actorOf(Multicaster.props(clusterClient, clientId), String.format("client-%d", clientId)); 
  }

  // TODO: use MulticastClient as TypedActor
  static public class Multicaster extends UntypedActor {
  //private final String agentId = UUID.randomUUID().toString();
    private final ActorRef clusterClient;
    private final Integer cid;
    private final String serverPath;
    private final ActorRef mcAgent;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props props(ActorRef clusterClient, int cid) {
      return Props.create(Multicaster.class, clusterClient, cid);
    }

    public Multicaster(ActorRef clusterClient, int cid) {
      this.clusterClient = clusterClient;
      this.cid = cid;
      this.mcAgent = getContext().watch(getContext().actorOf(CFMulticastAgent.props(clusterClient, false), "multicastAgent"));
      
      Config appConfig = ConfigFactory.load().getConfig("app");
      String serverHost = appConfig.getString("server_host");
      int serverPort = appConfig.getInt("server_port"); 

//      int serverPort = ConfigFactory.load("server").getConfig("akka.remote.netty.tcp").getInt("port");
//      String serverHost = ConfigFactory.load("server").getConfig("akka.remote.netty.tcp").getString("hostname");
      // Find a server
      this.serverPath = String.format("akka.tcp://BenchServer@%s:%d/user/server*", serverHost, serverPort);
    }

    private void sendIdentifyRequest(int id, String path) {
      getContext().actorSelection(path).tell(new Identify(id), getSelf());
      getContext()
        .system()
        .scheduler()
        .scheduleOnce(Duration.create(3, SECONDS), getSelf(),
            ReceiveTimeout.getInstance(), getContext().dispatcher(), getSelf());
    }

    @Override
    public void onReceive(Object message) {
      if (message instanceof ActorIdentity) {
        ActorIdentity identity = (ActorIdentity) message;
        ActorRef server = identity.getRef();
        int serverId = (int) identity.correlationId();
        if (server == null) {
          log.error("Server not available on: {}", serverPath);
        } else {
          log.info("Client {} receive Identify from {} ID: {}", cid, server, serverId);
          server.tell(new RegisterMessage(cid), getSelf());
          getContext().become(active, true);
        }

      } else if(message instanceof RegisterMessage) {
        // Register Agent to cluster
        mcAgent.tell(message, getSelf());
      
      } else if(message instanceof AckMessage) {
        synchronized(clientId) {
          clientId.notify();
          // Register this client with some listener server
          sendIdentifyRequest(cid, serverPath);
          log.info("Multicast Client UP: id={} - {}", cid, getSelf());
        }
      
      } else if (message instanceof ReceiveTimeout) {
        log.info("Timeout expired, retrying identify server on: {}", serverPath);
        sendIdentifyRequest(cid, serverPath);

      } else {
        log.info("Client {} not ready yet! Received: {}", cid, message);
      }
    }

    Procedure<Object> active = new Procedure<Object>() {
      @Override
      public void apply(Object message) {
        if (message instanceof ActorIdentity) {
          ActorIdentity identity = (ActorIdentity) message;
          ActorRef server = identity.getRef();
          int serverId = (int) identity.correlationId();
          log.info("Client {} already registred with some server but receive Identify from {} ID: {}. Ignoring...", cid, server, serverId);
        
        // Reply received from server
        } else if(message instanceof ClientMessage) {
          ClientMessage clientResponse = (ClientMessage) message;
          log.debug("Client {} receive RESPONSE from Server {}", getSelf(), getSender());
          receivedReplies.add(clientResponse);

        // Message to multicast
        } else if(message instanceof CFMulticastMessage) {
          CFMulticastMessage cfmessage = (CFMulticastMessage) message;
          log.debug("Client {} receive MULTICAST REQUEST from {}", getSelf(), getSender());
          mcAgent.tell(cfmessage, getSelf());

        } else if (message instanceof ReceiveTimeout) {
          // ignore

        // Reply received from server
        // FIXME This message is very generic, perhaps the best
        // is encapsulates it on the server in method sendReply
        } else if(message instanceof Message) {
          Message clientResponse = (Message) message;
          log.debug("Client {} receive RESPONSE:{} from Server {}", getSelf(), clientResponse, getSender());
          receivedReplies.add(clientResponse);

        } else {
          log.warning("Client receive unknown message {} from {}", message, getSender());
          unhandled(message);
        }  
      }
    };
  }

  //unused
	@Override
  public void connectToServer(int serverId) {
    //TODO
  }

  @Override
  public void connectToOneServerPerPartition() {
    synchronized(clientId) {
      multicaster.tell(new RegisterMessage(clientId), null);
      try {
        clientId.wait();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
	public void multicast(List<Group> destinations, ClientMessage clientMessage) {
    //FIXME Not ignore destinations! Implement broadcast using multicast
    // Encapsulate on a Multicast Message: Multicast(destinations, clientMessage)
    for(Group g : destinations) {
      CFDummyGroup group = (CFDummyGroup) g;
      List<ActorRef> groupMembers = group.getClusterMembers();
      CFMulticastMessage message = new CFMulticastMessage(groupMembers, clientMessage);
      multicaster.tell(message, null);
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
