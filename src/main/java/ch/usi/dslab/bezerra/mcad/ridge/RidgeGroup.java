package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.ArrayList;

import ch.usi.dslab.bezerra.mcad.Group;

public class RidgeGroup extends Group {
   static int maxGroupId;
   
   static {
      maxGroupId = -1;
   }
   
   ArrayList<RidgeEnsembleData> associatedRings;
   
   static int getMaxGroupId() {
      return maxGroupId;
   }
   
   public RidgeGroup(int id) {
      super(id);
      if (id > maxGroupId)
         maxGroupId = id;
      associatedRings = new ArrayList<RidgeEnsembleData>();
   }
   
   void addAssociatedRing(RidgeEnsembleData r) {
      if (associatedRings.contains(r) == false)
         associatedRings.add(r);
   }
   
   ArrayList<RidgeEnsembleData> getCorrespondingRings() {
      return associatedRings;
   }
}
