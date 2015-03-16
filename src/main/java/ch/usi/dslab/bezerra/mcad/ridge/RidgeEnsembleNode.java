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

import ch.usi.dslab.bezerra.ridge.Coordinator;
import ch.usi.dslab.bezerra.ridge.DeliverInterface;
import ch.usi.dslab.bezerra.ridge.Learner;
import ch.usi.dslab.bezerra.ridge.RidgeMessage;

public class RidgeEnsembleNode {
   
   public static class PrintDeliverInterface implements DeliverInterface {

      @Override
      public void deliverConservatively(RidgeMessage message) {
         System.out.println(String.format("PrintDeliverInterface: Delivered message %s conservatively", message.getId()));
      }

      @Override
      public void deliverOptimistically(RidgeMessage message) {
         System.out.println(String.format("PrintDeliverInterface: Delivered message %s optmistically", message.getId()));         
      }

      @Override
      public void deliverFast(RidgeMessage message) {
         System.out.println(String.format("PrintDeliverInterface: Delivered message %s fast", message.getId()));
      }
      
   }
   
   ch.usi.dslab.bezerra.ridge.Process ridgeNode;
   int pid;
   
   public RidgeEnsembleNode(String configFile, int pid) {
      this.pid = pid;
      RidgeMulticastAgent.loadRidgeAgentConfig(configFile);
      ridgeNode = ch.usi.dslab.bezerra.ridge.Process.getProcess(pid);
   }
   
   public void startRunning() {
      if (ridgeNode instanceof Coordinator)
         System.out.println(String.format("Coordinator %d starting...", pid));
      else if (ridgeNode instanceof Learner)
         ((Learner) ridgeNode).setDeliverInterface(new PrintDeliverInterface());
      ridgeNode.startRunning();
   }
   
   public static void main(String[] args) {
      String configFile = args[0];
      int    pid        = Integer.parseInt(args[1]);
      RidgeEnsembleNode reNode = new RidgeEnsembleNode(configFile, pid);
      reNode.startRunning();
   }
}
