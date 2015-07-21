package ch.usi.dslab.bezerra.mcad;

public abstract class DeliveryMetadata implements Comparable<DeliveryMetadata> {
   
   public abstract boolean precedes(DeliveryMetadata other);
   public abstract boolean equals(Object other);
   public abstract int     hashCode();

}
