package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

   @Override
   public List<Integer> getMembers() {
      Set<Integer> members = new HashSet<Integer>();
      for (URPRingData rd : associatedRings) {
         members.addAll(rd.getLearners());
      }
      List<Integer> membersList = new ArrayList<Integer>(members);
      Collections.sort(membersList);
      return membersList;
   }
}
