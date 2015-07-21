package ch.usi.dslab.bezerra.mcad.uringpaxos;

import ch.usi.dslab.bezerra.mcad.DeliveryMetadata;

public class URPDeliveryMetadata extends DeliveryMetadata {
   int  ringId;
   long instanceId;
   static int M = 1;

//   public URPDeliveryMetadata(int ringId, long instanceId) {
//      this.ringId     = ringId;
//      this.instanceId = instanceId;
//      this.batchPos   = -1;
//   }
   
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
   
   public static int getMultiRingM() {
      return M;
   }

   public static void setMultiRingM(int m) {
      M = m;
   }

   @Override
   public boolean precedes(DeliveryMetadata other) {
      return this.compareTo(other) < 0;
   }

   @Override
   public int compareTo(DeliveryMetadata o) {
      URPDeliveryMetadata other = (URPDeliveryMetadata) o;
      
      // both deliveries came from the same ring
      if (this.ringId == other.ringId) {
         if (this.instanceId < other.instanceId) return -1;
         else if (this.instanceId == other.instanceId) return 0;
         else return 1; 
      }
      
      // deliveries came from different rings
      else { // this.ringId != other.ringId
         long mergeroundthis  = this .instanceId;
         long mergeroundother = other.instanceId;
         if (M != 1) {
            mergeroundthis  /= M;
            mergeroundother /= M;
         }
         if (mergeroundthis == mergeroundother) return this.ringId - other.ringId;
         else if (mergeroundthis < mergeroundother) return -1;
         else return 1; 
      }
      
   }

   @Override
   public boolean equals(Object o) {
      URPDeliveryMetadata other = (URPDeliveryMetadata) o;
      return this.ringId == other.ringId && this.instanceId == other.instanceId;
   }

   @Override
   public int hashCode() {
      return ((int) instanceId) ^ ringId;
   }

}
