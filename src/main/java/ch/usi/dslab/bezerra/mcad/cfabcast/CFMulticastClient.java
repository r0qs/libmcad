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
import ch.usi.dslab.bezerra.mcad.cfabcast.CFMulticastClient;
import ch.usi.dslab.bezerra.netwrapper.Message;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class CFMulticastClient implements MulticastClient {
  
  private final int clientId;
  private final ActorRef multicaster;
  private static Config config;
  private static ActorSystem system;
  private static BlockingQueue<Message> receivedReplies;

  public CFMulticastClient(int clientId) {
    this.clientId = clientId;
    this.receivedReplies = new LinkedBlockingQueue<Message>();
    this.config = ConfigFactory.parseString("akka.cluster.roles = [client]")
      .withFallback(ConfigFactory.load());

    this.system = ActorSystem.create("ClusterSystem", config);
    this.multicaster = system.actorOf(Props.create(Multicaster.class), "multicaster");
  }

  static public class Multicaster extends UntypedActor {
    private final ActorRef mcagent = getContext().actorOf(Props.create(CFMulticastAgent.class), "client");

    @Override
    public void onReceive(Object message) {
      if(message instanceof ClientMessage) {
        ClientMessage clientResponse = (ClientMessage) message;
        receivedReplies.add(clientResponse);

      } else if(message instanceof CFMulticastMessage) {
        CFMulticastMessage cfmessage = (CFMulticastMessage) message;
        mcagent.tell(cfmessage, getSelf());

      } else {
        unhandled(message);
      } 
    }
  }


  public void connectToOneServerPerPartition() {
    //wait for all servers up. Await a agent notification msg
    //get and set group based on agent lookup
  }

  public void connectToServer(int serverId) {

  }

  //TODO Get destinations from Group (Actors refs)
  @Override
	public void multicast(List<Group> destinations, ClientMessage clientMessage) {
    //FIXME Not ignore destinations!
    // Encapsulate on a Multicast Message: Multicast(destinations, clientMessage)
    CFMulticastMessage message = new CFMulticastMessage(destinations, clientMessage);
    multicaster.tell(message, null);
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
