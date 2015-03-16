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

package ch.usi.dslab.bezerra.mcad;

import java.util.concurrent.atomic.AtomicLong;

import ch.usi.dslab.bezerra.netwrapper.Message;

public class ClientMessage extends Message {
   private static final long serialVersionUID = 1L;
   private static AtomicLong nextSeq = new AtomicLong(0);
   private static int globalClientId = -1;

   public static void setGlobalClientId(int id) {
      globalClientId = id;
   }
   
   public static int getGlobalClientId() {
      return globalClientId;
   }
   
   int clientId;
   long msgSeq;
   
   public int getSourceClientId() {
      return clientId;
   }
   
   public void setSourceClientId(int id) {
      this.clientId = id;
   }

   public void setMessageSequence(long seq) {
      this.msgSeq = seq;
   }
   
   public long getMessageSequence() {
      return this.msgSeq;
   }
   
   public ClientMessage() {
      super();
      clientId = globalClientId;
      msgSeq   = nextSeq.incrementAndGet();
   }
   
   public ClientMessage(Object... objs) {
      super(objs);
      clientId = globalClientId;
      msgSeq   = nextSeq.incrementAndGet();
   }
   
}
