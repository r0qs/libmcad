package ch.usi.dslab.bezerra.mcad.uringpaxos;

public class URPGroup {
   static int maxGroupId;
   
   static {
      maxGroupId = -1;
   }
   
   static int getMaxGroupId() {
      return maxGroupId;
   }
}
