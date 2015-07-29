package ch.usi.dslab.bezerra.mcad;

import java.io.Serializable;

public abstract class DeliveryMetadata implements Comparable<DeliveryMetadata>, Serializable {
   private static final long serialVersionUID = 1L;
   
   public abstract boolean precedes(DeliveryMetadata other);
   public abstract boolean equals(Object other);
   public abstract int     hashCode();

}
