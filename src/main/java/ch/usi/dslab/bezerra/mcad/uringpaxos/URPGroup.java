package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.util.ArrayList;

import ch.usi.dslab.bezerra.mcad.Group;

public class URPGroup extends Group {
   static int maxGroupId;
   
   static {
      maxGroupId = -1;
   }
   
   ArrayList<URPRingData> associatedRings;
   
   static int getMaxGroupId() {
      return maxGroupId;
   }
   
   public URPGroup(int id) {
      super(id);
      if (id > maxGroupId)
         maxGroupId = id;
      associatedRings = new ArrayList<URPRingData>();
   }
   
   void addAssociatedRing(URPRingData r) {
      if (associatedRings.contains(r) == false)
         associatedRings.add(r);
   }
   
   ArrayList<URPRingData> getCorrespondingRings() {
      return associatedRings;
   }
}
