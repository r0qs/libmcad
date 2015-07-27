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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.usi.dslab.bezerra.mcad.Group;

public class URPGroup extends Group {
   static int maxGroupId;
   
   static {
      maxGroupId = -1;
   }
   
   ArrayList<URPRingData> associatedRings;
   ArrayList<Integer>     members;
   
   static int getMaxGroupId() {
      return maxGroupId;
   }
   
   public URPGroup(int id) {
      super(id);
      if (id > maxGroupId)
         maxGroupId = id;
      associatedRings = new ArrayList<URPRingData>();
      members = new ArrayList<Integer>();
   }
   
   void addAssociatedRing(URPRingData r) {
      if (associatedRings.contains(r) == false)
         associatedRings.add(r);
   }
   
   ArrayList<URPRingData> getAssociatedRings() {
      return associatedRings;
   }
   
   void addMember(int memberId) {
      if (members.contains(memberId) == false)
         members.add(memberId);
   }

   @Override
   public List<Integer> getMembers() {
      List<Integer> membersList = new ArrayList<Integer>(members);
      Collections.sort(membersList);
      return membersList;
   }
}
