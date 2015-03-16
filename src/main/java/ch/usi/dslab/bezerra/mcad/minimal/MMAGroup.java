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

import java.util.ArrayList;
import java.util.List;

import ch.usi.dslab.bezerra.mcad.Group;

public class MMAGroup extends Group {
   static ArrayList<MMAGroup> groupList;
   
   static {
      groupList = new ArrayList<MMAGroup>();
   }
   
   ArrayList<MMANode> nodeList;

   public MMAGroup(int id) {
      super(id);
      nodeList = new ArrayList<MMANode>();
      groupList.add(this);
   }
   
   public void addNode(MMANode node) {
      nodeList.add(node);
   }

   @Override
   public List<Integer> getMembers() {
      // TODO Auto-generated method stub
      return null;
   }

}
