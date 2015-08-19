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
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.actor.ActorIdentity;
import akka.actor.ActorSelection;
import akka.actor.Identify;
import akka.actor.Terminated;
import akka.actor.ExtendedActorSystem;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.io.Serializable;

import cfabcast.messages.*;
import cfabcast.serialization.CFABCastSerializer;

public class CFMulticastAgent extends UntypedActor implements MulticastAgent {
  LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  Cluster cluster = Cluster.get(getContext().system());

  Set<ActorRef> nodes = new HashSet<ActorRef>();
  Set<ActorRef> servers = new HashSet<ActorRef>();
  Set<ActorRef> clients = new HashSet<ActorRef>();

  CFABCastSerializer serializer = new CFABCastSerializer((ExtendedActorSystem) getContext().system());

  //TODO read config and initiate groups

  @Override
  public void preStart() {
    // subscribe
    cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
      MemberEvent.class, UnreachableMember.class);
  }
  
  //re-subscribe when restart
  @Override
  public void postStop() {
    cluster.unsubscribe(getSelf());
  }

  void register(Member member) {
    if(member.hasRole("cfabcast") || member.hasRole("client") || member.hasRole("server"))
      getContext().actorSelection(member.address() + "/user/*").tell(new Identify(member), getSelf());
  }

  @Override
  public void multicast(Group single_destination, Message message) {
    CFDummyGroup g = (CFDummyGroup) single_destination;

    Broadcast broadcastMessage = new Broadcast(serializer.toBinary(message));

    for(ActorRef protocolAgents : g.membersRefList)
      protocolAgents.tell(broadcastMessage, getSelf()); 
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
    // Cluster member events 
    if(message instanceof MemberUp) {
      MemberUp mUp = (MemberUp) message;
      register(mUp.member());
      log.info("Member is Up: {}", mUp.member());
    
    } else if(message instanceof UnreachableMember) {
      UnreachableMember mUnreachable = (UnreachableMember) message;
      log.info("Member detected as unreachable: {}", mUnreachable.member());
    
    } else if(message instanceof MemberRemoved) {
      MemberRemoved mRemoved = (MemberRemoved) message;
      log.info("Member is Removed: {}", mRemoved.member());
    
    } else if(message instanceof MemberEvent) {
      // ignore

    } else if(message instanceof ActorIdentity) {
      ActorIdentity identity = (ActorIdentity) message;
      Member member = (Member) identity.correlationId();
      ActorRef ref = identity.getRef();
      if(ref != null && ref != getSelf()) {
        log.info("Adding new member with roles: {}", member.getRoles());
        if(member.hasRole("cfabcast"))
          nodes.add(ref);
          //TODO add node to Group
        if(member.hasRole("client"))
          clients.add(ref);
        if(member.hasRole("server"))
          servers.add(ref);
        getContext().watch(ref);
      }   

     //TODO remove node from Group 
     } else if (message instanceof Terminated) {
        final Terminated term = (Terminated) message;
        ActorRef terminated = term.getActor();
        log.info("Actor: {} terminated!", terminated);
        if(nodes.contains(terminated))
          nodes.remove(terminated);
        else if(clients.contains(terminated))
          clients.remove(terminated);
        else if(servers.contains(terminated))
          servers.remove(terminated);

    } else if(message instanceof CFMulticastMessage) {
      System.out.println("MULTICAST MESSAGE RECEIVED: " + message);
      multicast(message.getDestinations(), message.getMessage());

    } else if(message instanceof Delivery) {
      Delivery response = (Delivery) message;
      Message msg = (Message) serializer.fromBinary(response.getData());
      getContext().parent().tell(msg, getSelf());

    } else {
      unhandled(message);
    }
     
  }
}
