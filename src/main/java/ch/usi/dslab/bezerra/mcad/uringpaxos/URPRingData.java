package ch.usi.dslab.bezerra.mcad.uringpaxos;

import java.util.ArrayList;

public class URPRingData {
   static ArrayList<URPRingData> ringsList;
   int arlistpos;
   int groupFirst, pivotValue, groupLast;
   boolean multigroup;
   
   int    ringId;
   String proposerAddress;
   int    proposerPort;   
   
   static {
      ringsList = new ArrayList<URPRingData>();
   }
   
   static URPRingData getById(int id) {
      for (URPRingData urd : ringsList)
         if (urd.getId() == id)
            return urd;
      return null;
   }
   
   public URPRingData (int ringId) {
      this.ringId = ringId;
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
         
         if (groupLast < groupFirst) {
            System.out.println("ERROR :: groupLast ("  + groupLast  + ")" +
            		                   " < groupFirst (" + groupFirst + ") !!!");
            System.exit(-1);
         }
         
      }
      
      multigroup = groupFirst != groupLast;
      
      if (multigroup)
         pivotValue = (groupLast + groupFirst + 1) / 2;
      else
         pivotValue = groupFirst;            
   }
   
   public void setProposerHelper(String address, int port) {
      proposerAddress = address;
      proposerPort    = port;
   }
   
   public int getId() {
      return ringId;
   }
   
   public String getProposerAddress() {
      return proposerAddress;
   }
   
   public int getProposerPort() {
      return proposerPort;
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
