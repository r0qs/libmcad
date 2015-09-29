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

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.DeliveryMetadata;
import ch.usi.dslab.bezerra.mcad.cfabcast.CFDummyGroup;
import ch.usi.dslab.bezerra.netwrapper.Message;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.ExtendedActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.contrib.pattern.ClusterClient;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.io.Serializable;

import cfabcast.messages.*;
import cfabcast.serialization.CFABCastSerializer;

public class CFMulticastAgent extends UntypedActor implements MulticastAgent {
  LoggingAdapter log;
  ActorRef proposer;
  private CFABCastSerializer serializer;
  private final ActorRef clusterClient;

  public static Props props(ActorRef clusterClient, boolean isServer) {
    return Props.create(CFMulticastAgent.class, clusterClient, isServer);
  }

  //TODO read config and initiate groups
  public CFMulticastAgent(ActorRef clusterClient) {
    log = Logging.getLogger(getContext().system(), this);
    this.clusterClient = clusterClient;
    this.serializer = new CFABCastSerializer((ExtendedActorSystem) getContext().system());
    if (isServer) {
      clusterClient.tell(new ClusterClient.Send("/user/node*", RegisterServer.instance(), true), getSelf());
    } else {
      clusterClient.tell(new ClusterClient.Send("/user/node*", RegisterClient.instance(), true), getSelf());
    }
  }

  @Override
  public void preStart() {
  }
  
  @Override
  public void postStop() {
  }

  @Override
  public void multicast(Group single_destination, Message message) {
    CFDummyGroup g = (CFDummyGroup) single_destination;

    //TODO include group in message
    Broadcast broadcastMessage = new Broadcast(serializer.toBinary(message));

//    for(ActorRef protocolAgents : g.membersRefList)
    proposer.tell(broadcastMessage, getSelf()); 
  }
  //TODO Pass a List with a single group
  public void multicast(List<Group> destinations, Message message) {
    for(Group g : destinations)
      multicast(g, message);
  }
 
  public Message deliverMessage() {
    //TODO
    return null;
  }

  public Group getLocalGroup() {
    //TODO
    return null;
  }

  public void notifyCheckpointMade(DeliveryMetadata deliveryToKeep) {
    //TODO
  }

  public boolean hasWholeDeliveryPreffix() {
    //TODO
    return true;
  }

  @Override
  public void onReceive(Object message) {
    if(message instanceof CFMulticastMessage) {
      CFMulticastMessage msg = (CFMulticastMessage) message;
      log.info("Agent {} - Sending multicast: {} to {} ", getSelf(), msg, proposer);
      multicast(msg.getDestinations(), msg.getMessage());

/*  // Replies are sent directly to server associated with some learner 
    } else if(message instanceof Delivery) {
      Delivery response = (Delivery) message;
      Message msg = (Message) serializer.fromBinary(response.getData());
      log.info("Agent {} - Receive response: {} from {} ", getSelf(), msg, getSender());
      getContext().parent().tell(msg, getSelf());
   */ 
    } else if(message instanceof ClientRegistered) {
      ClientRegistered c = (ClientRegistered) message;
      proposer = c.getProposer();
      //TODO Create a CFDummyGroup
      Set<ActorRef> group = c.getGroup();

    } else {
      log.info("Agent {} receive unknown message: {} from {}", getSelf(), message, getSender());
      unhandled(message);
    }
     
  }
}
