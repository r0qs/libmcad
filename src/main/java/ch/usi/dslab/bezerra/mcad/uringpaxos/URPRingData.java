package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.util.ArrayList;

import sun.util.logging.resources.logging;

public class URPRingData {
   static ArrayList<URPRingData> ringsList;
   private int arlistpos;
   private int groupFirst, pivotValue, groupLast;
   private boolean multigroup;
   
   static {
      ringsList = new ArrayList<URPRingData>();
   }
   
   public URPRingData (int ringId, String proposerHelperAddress, int proposerHelperPort) {
      ringsList.add(this);
      this.arlistpos = ringsList.size() - 1;
      if (arlistpos == 0) {
         groupFirst = 0;
         groupLast  = URPGroup.getMaxGroupId();
      }
      else {
         URPRingData parent = ringsList.get((arlistpos - 1) / 2);
         if (this == parent.left()) {
            groupFirst = parent.groupFirst;
            groupLast  = parent.pivotValue - 1;            
         } else {
            groupFirst = parent.pivotValue;
            groupLast  = parent.groupLast;
         }
         
         if (groupFirst < groupLast) {
            System.out.println("ERROR :: groupFirst (" + groupFirst +
                               ") < groupLast (" + groupLast + ") !!!");
            System.exit(-1);
         }
         
      }
      
      multigroup = groupFirst != groupLast;
      
      if (multigroup)
         pivotValue = (groupLast + groupFirst + 1) / 2;
      else
         pivotValue = groupFirst;
            
   }
   
   public URPRingData left() {
      int leftPos = (2 * arlistpos) + 1;
      if (ringsList.size() <= leftPos)
         return null;
      else
         return ringsList.get(leftPos);
   }
   
   public URPRingData right() {
      int rightPos = (2 * arlistpos) + 2;
      if (ringsList.size() <= rightPos)
         return null;
      else
         return ringsList.get(rightPos);      
   }
   
   public int pivot() {
      return 0;
   }

}
