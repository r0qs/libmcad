package ch.usi.dslab.bezerra.mcad.uringpaxos;

import ch.usi.dslab.bezerra.mcad.DeliveryMetadata;

public class URPDeliveryMetadata implements DeliveryMetadata {
   int  ringId;
   long instanceId;
   
   public URPDeliveryMetadata(int ringId, long instanceId) {
      this.ringId     = ringId;
      this.instanceId = instanceId;
   }
   
   public void setRingId(int ringId) {
      this.ringId = ringId;
   }
   
   public void setInstanceId(long instanceId) {
      this.instanceId = instanceId;
   }
   
   public int getRingId() {
      return ringId;
   }

   public long getInstanceId() {
      return instanceId;
   }

   @Override
   public boolean precedes(DeliveryMetadata other) {
      // TODO Auto-generated method stub
      return false;
   }

}
