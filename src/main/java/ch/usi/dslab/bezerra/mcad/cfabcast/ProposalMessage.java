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

import java.io.Serializable;

public class ProposalMessage implements Serializable {
  int clientId;
  long msgSeq;
  byte[] payload;

  public ProposalMessage(int clientId, long msgSeq, byte[] payload) {
    this.clientId = clientId;
    this.msgSeq = msgSeq;
    this.payload = payload;
  }
   
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
  
  public void setMessagePayload(byte[] payload) {
    this.payload = payload;
  }
   
  public byte[] getMessagePayload() {
    return this.payload;
  }

  public int getMessagePayloadSize() {
    return this.payload.length;
  }

}
