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
 * @author Eduardo Bezerra - eduardo.bezerra@usi.ch
 */

package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.LinkedList;
import java.util.Queue;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.FastMulticastAgent;
import ch.usi.dslab.bezerra.mcad.FastMulticastServer;
import ch.usi.dslab.bezerra.mcad.MulticastAgent;
import ch.usi.dslab.bezerra.mcad.MulticastServer;
import ch.usi.dslab.bezerra.netwrapper.Message;
import ch.usi.dslab.bezerra.ridge.Learner;

public class RidgeMulticastServer implements MulticastServer, FastMulticastServer {
   
   FastMulticastAgent associatedMulticastAgent;
   Learner associatedLearner;
   Queue<ClientMessage> unbatchedClientMessages;
   Queue<ClientMessage> unbatchedFastClientMessages;

   public RidgeMulticastServer(FastMulticastAgent associatedAgent, Learner associatedLearner) {
      this.associatedMulticastAgent = associatedAgent;
      this.associatedLearner = associatedLearner;
      this.unbatchedClientMessages = new LinkedList<ClientMessage>();
      this.unbatchedFastClientMessages = new LinkedList<ClientMessage>();
   }
   
   @Override
   public int getId() {
      return associatedLearner.getPid();
   }
   
   @Override
   public boolean isConnectedToClient(int clientId) {
      return associatedLearner.isConnectedToClient(clientId);
   }

   @Override
   public void sendReply(int clientId, Message reply) {
      associatedLearner.sendReplyToClient(reply, clientId);
   }

   @Override
   public MulticastAgent getMulticastAgent() {
      return associatedMulticastAgent;
   }

   @Override
   public ClientMessage deliverClientMessage() {
      if (unbatchedClientMessages.isEmpty()) {
         Message nextClientRequestBatch = associatedMulticastAgent.deliverMessage();
         nextClientRequestBatch.rewind();
         while (nextClientRequestBatch.hasNext()) {
            ClientMessage clireq = (ClientMessage) nextClientRequestBatch.getNext();
            clireq.unpackContents();
            unbatchedClientMessages.add(clireq);
         }
      }

      ClientMessage climsg = unbatchedClientMessages.remove();
      return climsg;
   }

   @Override
   public ClientMessage deliverClientMessageFast() {
      if (unbatchedFastClientMessages.isEmpty()) {
         Message nextFastClientRequestBatch = associatedMulticastAgent.deliverMessageFast();
         nextFastClientRequestBatch.rewind();
         while (nextFastClientRequestBatch.hasNext()) {
            ClientMessage clireq = (ClientMessage) nextFastClientRequestBatch.getNext();
            clireq.unpackContents();
            unbatchedFastClientMessages.add(clireq);
         }
      }

      ClientMessage fastclimsg = unbatchedFastClientMessages.remove();
      return fastclimsg;
   }

}
