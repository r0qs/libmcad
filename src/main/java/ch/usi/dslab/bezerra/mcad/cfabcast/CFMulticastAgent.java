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
import ch.usi.dslab.bezerra.mcad.cfabcast.CFDummyGroup;
import ch.usi.dslab.bezerra.netwrapper.Message;

import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class CFMulticastAgent extends UntypedActor implements MulticastAgent {
  LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  Cluster cluster = Cluster.get(getContext().system());

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

  public void multicast(Group single_destination, Message message) {
    CFDummyGroup g = (CFDummyGroup) single_destination;
    for(ActorRef ref : g.membersRefList)
      ref.tell(message, getSelf()); 
  }
  public void multicast(List<Group> destinations, Message message) {
    for(Group g : destinations)
      multicast(g, message);
  }
 
  public byte[] deliver() {
    //TODO
    return null;
  }

  public Message deliverMessage() {
    //TODO
    return null;
  }
  
  public Group getLocalGroup() {
    //TODO
    return null;
  }

  @Override
  public void onReceive(Object message) {
    // Cluster member events 
    if(message instanceof MemberUp) {
      MemberUp mUp = (MemberUp) message;

      log.info("Member is Up: {}", mUp.member());
    
    } else if(message instanceof UnreachableMember) {
      UnreachableMember mUnreachable = (UnreachableMember) message;
      log.info("Member detected as unreachable: {}", mUnreachable.member());
    
    } else if(message instanceof MemberRemoved) {
      MemberRemoved mRemoved = (MemberRemoved) message;
      log.info("Member is Removed: {}", mRemoved.member());
    
    } else if(message instanceof MemberEvent) {
      // ignore
    } else if(message instanceof Message) {
      System.out.println("MULTICAST MESSAGE RECEIVED: " + message);

      //TODO serialize message (clientMessage), creating a new CFMessage(getSender(), Message) and send this to protocol actors in this agent Group
    } else if(message instanceof CFMessage) {
      System.out.println("DELIVERY MESSAGE RECEIVED: " + message);
      //TODO Deserialize here! Forward response msg or let the protocol actors respond directly
      // send the sender (Client) in msg
      // send the response (a Message object) to him.
      //getContext().parent.tell(...)
    } else {
      unhandled(message);
    }
     
  }
}
