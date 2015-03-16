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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.usi.dslab.bezerra.mcad.Group;
import ch.usi.dslab.bezerra.ridge.Learner;

public class RidgeGroup extends Group {
   static int maxGroupId;
   
   static {
      maxGroupId = -1;
   }
   
   static int getMaxGroupId() {
      return maxGroupId;
   }
   
   ArrayList<RidgeEnsembleData> associatedEnsembles;
   List<Integer> membersCache = null;
   
   public RidgeGroup(int id) {
      super(id);
      if (id > maxGroupId)
         maxGroupId = id;
      associatedEnsembles = new ArrayList<RidgeEnsembleData>();
   }
   
   void addAssociatedEnsemble(RidgeEnsembleData e) {
      if (associatedEnsembles.contains(e) == false)
         associatedEnsembles.add(e);
   }
   
   ArrayList<RidgeEnsembleData> getCorrespondingEnsembles() {
      return associatedEnsembles;
   }

   @Override
   public List<Integer> getMembers() {
//      if (membersCache == null) {
         Set<Learner> associatedLearners = new HashSet<Learner>(associatedEnsembles.get(0).ensemble.getLearners());
         for (RidgeEnsembleData ensembleData : associatedEnsembles)
            associatedLearners.retainAll(ensembleData.ensemble.getLearners());
         List<Integer> members = new ArrayList<Integer>(associatedLearners.size());
         for (Learner learner : associatedLearners)
            members.add(learner.getPid());
         Collections.sort(members);
         this.membersCache = members;
         
//      }
     
//      for (int id : membersCache) {
//         System.out.println(String.format("Group %d has member %d", this.getId(), id));
//      }
      return membersCache;
   }
}
