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
