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

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;

public class CFDummyGroup extends Group {
  ArrayList<ActorRef> membersRefList;
  
  public CFDummyGroup(int id) {
    super(id);
    membersRefList = new ArrayList<ActorRef>();
  }

  public void addMember(ActorRef ref) {
    membersRefList.add(ref);
  }

  //unused
  @Override
  public List<Integer> getMembers() {
    return null;
  }

  public List<ActorRef> getClusterMembers() {
    return membersRefList;
  }

}
