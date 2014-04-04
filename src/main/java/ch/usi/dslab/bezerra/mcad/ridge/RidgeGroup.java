package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.ArrayList;

import ch.usi.dslab.bezerra.mcad.Group;

public class RidgeGroup extends Group {
   static int maxGroupId;
   
   static {
      maxGroupId = -1;
   }
   
   ArrayList<RidgeEnsembleData> associatedEnsembles;
   
   static int getMaxGroupId() {
      return maxGroupId;
   }
   
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
   
   ArrayList<RidgeEnsembleData> getCorrespondingRings() {
      return associatedEnsembles;
   }
}
