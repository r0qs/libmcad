package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.util.ArrayList;

import ch.usi.dslab.bezerra.mcad.Group;

public class URPGroup extends Group {
   static int maxGroupId;
   
   static {
      maxGroupId = -1;
   }
   
   static URPRingData getMultigroupRing(ArrayList<URPGroup> dests) {
      
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
   
   void addAssociatedRing(int ringId) {
      URPRingData ring = URPRingData.getById(ringId);
      associatedRings.add(ring);
   }
   
   ArrayList<URPRingData> getCorrespondingRings() {
      return associatedRings;
   }
}
