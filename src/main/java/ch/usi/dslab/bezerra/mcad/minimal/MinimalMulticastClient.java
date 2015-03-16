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

package ch.usi.dslab.bezerra.mcad.minimal;

import java.util.List;

import ch.usi.dslab.bezerra.mcad.ClientMessage;
import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.mcad.MulticastClient;
import ch.usi.dslab.bezerra.netwrapper.Message;

public class MinimalMulticastClient implements MulticastClient {

   public MinimalMulticastClient(int clientId, String configFile) {
      
   }
   
   @Override
   public void connectToServer(int serverId) {
      // TODO Auto-generated method stub
   }
   
   @Override
   public Message deliverReply() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void multicast(List<Group> destinations, ClientMessage message) {
      // TODO Auto-generated method stub
   }

   @Override
   public void connectToOneServerPerPartition() {
      // TODO Auto-generated method stub
      
   }

}
