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

package ch.usi.dslab.bezerra.mcad.uringpaxos;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.PaxosNode;
import ch.usi.da.paxos.message.Value;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class URPAgentLearner implements Runnable {
   public static final Logger log = Logger.getLogger(URPAgentLearner.class);
   PaxosNode paxos;
   URPMcastAgent mcAgent;
   Thread urpAgentLearnerThread;
   int learnerId;
   private final static Logger logger;
//   private final static Logger valuelogger;
   
   static {
      logger      = Logger.getLogger(URPMcastAgent.class);
//      valuelogger = Logger.getLogger(Value.class);
      log.setLevel(Level.OFF);
   }

   public URPAgentLearner(URPMcastAgent mcAgent, PaxosNode paxos, int learnerId) {
      this.mcAgent = mcAgent;
      this.paxos = paxos;
      this.learnerId = learnerId;
      urpAgentLearnerThread = new Thread(this);
      urpAgentLearnerThread.start();
   }
   
   // assume that the learner has the same ids in ALL rings   
   public int getLearnerId() {
      // TODO
      return -1;
   }

   @Override
   public void run() {      
      if (paxos.getLearner() == null) {
         log.error("EEE === Not a learner");
         return; // not a learner
      }
      while (true) {
         try {
            Value v = paxos.getLearner().getDecisions().take().getValue();
            if (!v.isSkip()) {
               byte[] rawBatch = v.getValue();

               if (rawBatch.length == 0 ) {
                  // System.err.println("0 bytes decision!");
                  // System.exit(1);
                  continue;
               }

               long t_learner_delivered = System.currentTimeMillis();
               Message batch = Message.createFromBytes(rawBatch);
                              
               while (batch.hasNext()) {
                  byte[] msg = (byte []) batch.getNext(); // cmdContainer
//                  mcAgent.checkMessageAndEnqueue(msg, batch.t_batch_ready,
//                        batch.piggyback_proposer_serialstart, batch.piggyback_proposer_serialend,
//                        t_learner_delivered);
                  mcAgent.checkMessageAndEnqueue(msg, 0, 0, 0, t_learner_delivered);
               }               
            }            
         }
         catch (InterruptedException e) {
            logger.error(e);
            System.exit(0);
         }
      }
   }

}
