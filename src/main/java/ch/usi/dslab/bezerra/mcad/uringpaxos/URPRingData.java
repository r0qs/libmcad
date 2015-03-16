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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class URPRingData {
   static List<URPRingData> ringsList;

   private final static Logger log;
   
   static {
      ringsList = new ArrayList<URPRingData>();
      log = Logger.getLogger(URPRingData.class);
   }
   
   public static List<URPRingData>getRingsList() {
      return new ArrayList<URPRingData>(ringsList);
   }
   
   public static URPRingData getById(int id) {
      for (URPRingData urd : ringsList)
         if (urd.getId() == id)
            return urd;
      return null;
   }
   
   ArrayList<URPGroup> destinationGroups;
   URPRingWatcher ringWatcher;
   
   int    ringId;
   String coordinatorAddress;
   int    coordinatorPort;
   
   SocketChannel coordinatorConnection;
   
   public URPRingData (int ringId) {
      this.ringId = ringId;
      ringsList.add(this);
      destinationGroups = new ArrayList<URPGroup>();
   }
   
   public void setCoordinator(String address, int port) {
      coordinatorAddress = address;
      coordinatorPort    = port;
   }
   
   public void connectToCoordinator() {
      try {
         coordinatorConnection = SocketChannel.open();
         coordinatorConnection.connect(new InetSocketAddress(coordinatorAddress, coordinatorPort));
      } catch (IOException e) {
         log.fatal(" !!! Couldn't connect to ring " + this.ringId);
         e.printStackTrace();
         System.exit(1);
      }
      
   }
   
   public int getId() {
      return ringId;
   }
   
   public String getProposerAddress() {
      return coordinatorAddress;
   }
   
   public int getProposerPort() {
      return coordinatorPort;
   }
   
   public void setWatcher(URPRingWatcher watcher) {
      ringWatcher = watcher;
   }
   
   List<Integer> getLearners() {
      return ringWatcher.getLearners();
   }
   
   public void addDestinationGroup(URPGroup g) {
      if (destinationGroups.contains(g) == false)
         destinationGroups.add(g);
   }

}
