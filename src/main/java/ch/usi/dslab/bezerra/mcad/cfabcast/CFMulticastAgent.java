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
import ch.usi.dslab.bezerra.mcad.ClientMessage;

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
  CFDummyGroup localGroup = null;

  public static Props props(ActorRef clusterClient, boolean isServer) {
    return Props.create(CFMulticastAgent.class, clusterClient, isServer);
  }

  public CFMulticastAgent(ActorRef clusterClient, boolean isServer) {
    log = Logging.getLogger(getContext().system(), this);
    this.clusterClient = clusterClient;
    this.serializer = new CFABCastSerializer((ExtendedActorSystem) getContext().system());
    if (isServer) {
      clusterClient.tell(new ClusterClient.Send("/user/node", new RegisterServer(getSelf()), true), getSelf());
    } 
  }

  // unused
  @Override
  public void multicast(Group single_destination, Message message) {
    CFDummyGroup g = (CFDummyGroup) single_destination;
    //TODO include group in cfabcast message
    Broadcast broadcastMessage = new Broadcast(serializer.toBinary(message));

    //for(ActorRef protocolAgents : g.membersRefList)
    proposer.tell(broadcastMessage, getSelf()); 
  }
 
  // unused
  @Override
  public void multicast(List<Group> destinations, Message message) {
    //TODO Pass a List with a single group
    for(Group g : destinations)
      multicast(g, message);
  }

  public void broadcast(List<ActorRef> destinations, Message message) {
    ClientMessage msg = (ClientMessage) message;

    int clientId = msg.getSourceClientId();
    long msgSeq = msg.getMessageSequence();
    byte[] payload = new byte[msg.getByteArraysAggregatedLength()];
    ProposalMessage pmsg = new ProposalMessage(clientId, msgSeq, payload);

    Broadcast broadcastMessage = new Broadcast(serializer.toBinary(pmsg));
    proposer.tell(broadcastMessage, getSelf()); 
  }
  
  //unused
  public Message deliverMessage() {
    //TODO
    return null;
  }

  //unused
  public void notifyCheckpointMade(DeliveryMetadata deliveryToKeep) {
    //TODO
  }

  //unused
  public boolean hasWholeDeliveryPreffix() {
    //TODO
    return true;
  }

  @Override
  public Group getLocalGroup() {
    return this.localGroup;
  }

  public void setLocalGroup(Group g) {
    this.localGroup = (CFDummyGroup) g;
  }

  @Override
  public void onReceive(Object message) {
    if(message instanceof RegisterMessage) {
      clusterClient.tell(new ClusterClient.Send("/user/node", new RegisterClient(getSelf()) , true), getSelf());
     
    } else if(message instanceof ClientRegistered) {
      ClientRegistered c = (ClientRegistered) message;
      proposer = c.getProposer();
      Set<ActorRef> groupSet = c.getGroup();
      Group.changeGroupImplementationClass(CFDummyGroup.class);
      // FIXME Pass groupId in configuration
      CFDummyGroup group = (CFDummyGroup) Group.getOrCreateGroup((int) 1);
      for(ActorRef member : groupSet)
        group.addMember(member);
      log.debug("MEMBERS OF GROUP [{}] : {}", group.getId(), group.getClusterMembers());
      setLocalGroup(group);

      getContext().parent().tell(new AckMessage(), getSelf());

    } else if(message instanceof CFMulticastMessage) {
      CFMulticastMessage msg = (CFMulticastMessage) message;
      log.debug("Agent {} BROADCAST Message to PROPOSER: {}", getSelf(), proposer);
      // Send to all destinations, ignore msg.getDestinations()
      broadcast(null, msg.getMessage());

    } else if(message instanceof Delivery) {
      Delivery response = (Delivery) message;
      //Message msg = (Message) serializer.fromBinary(response.getData());
      ProposalMessage msg = (ProposalMessage) serializer.fromBinary(response.getData());

      ClientMessage cmsg = new ClientMessage(msg.getMessagePayload());
      cmsg.setMessageSequence(msg.getMessageSequence());
      cmsg.setSourceClientId(msg.getSourceClientId());

      log.debug("Agent {} - Receive response: {} from {} ", getSelf(), cmsg, getSender());
      getContext().parent().tell(cmsg, getSelf());

    } else {
      log.warning("Agent {} receive unknown message: {} from {}", getSelf(), message, getSender());
      unhandled(message);
    }
  }
}
