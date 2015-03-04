package ch.usi.dslab.bezerra.mcad.benchmarks;

import java.io.Serializable;
import java.util.PriorityQueue;

public class BenchmarkEventList implements Serializable {
   
   public static interface EventInfo extends Serializable, Comparable<EventInfo> {
      public static final byte       MESSAGES_EVENT = 0;
      public static final byte        PERMIT_EVENT = 1;
      public static final byte GLOBAL_PERMIT_EVENT = 2;
      byte getType();
      long getTimestamp();
   }
   
   public static class MessageCountEvent implements EventInfo {
      private static final long serialVersionUID = 1L;
      long timestamp;
      double averageLatency;
      long messageCount;
      public MessageCountEvent(long ts, double latAvg, long mc) {
         timestamp = ts;
         averageLatency = latAvg;
         messageCount = mc;
      }
      public byte getType() {
         return MESSAGES_EVENT;
      }
      @Override
      public long getTimestamp() {
         return timestamp;
      }
      public long getMessageCount() {
         return messageCount;
      }
      @Override
      public int compareTo(EventInfo o) {
         if (this.timestamp < o.getTimestamp())
            return -1;
         else if (this.timestamp > o.getTimestamp())
            return 1;
         else
            return 0;
      }
      @Override
      public String toString() {
         return String.format("%d M %f %d", timestamp, averageLatency, messageCount);
      }
   }
   
   public static class PermitEvent implements EventInfo {
      private static final long serialVersionUID = 1L;
      long timestamp;
      int clientId;
      int newNumberOfPermits;
      public PermitEvent(){}
      public PermitEvent(long ts, int cid, int newNumPerms) {
         timestamp = ts;
         clientId = cid;
         newNumberOfPermits = newNumPerms;
      }
      public byte getType() {
         return PERMIT_EVENT;
      }
      @Override
      public long getTimestamp() {
         return timestamp;
      }
      @Override
      public int compareTo(EventInfo o) {
         if (this.timestamp < o.getTimestamp())
            return -1;
         else if (this.timestamp > o.getTimestamp())
            return 1;
         else
            return 0;
      }
      @Override
      public String toString() {
         return String.format("%d P %d %d", timestamp, clientId, newNumberOfPermits);
      }
   }
   
   public static class GlobalPermitEvent extends PermitEvent {
      private static final long serialVersionUID = 1L;
      int allPermits;
      public GlobalPermitEvent(long ts, int cid, int newNumPerms, int globalPermits) {
         super(ts, cid, newNumPerms);
         allPermits = globalPermits;
      }
      public byte getType() {
         return GLOBAL_PERMIT_EVENT;
      }
      @Override
      public String toString() {
         return String.format("%d GP %d %d %d", timestamp, clientId, newNumberOfPermits, allPermits);
      }
   }
   
   
   private static final long serialVersionUID = 1L;
   PriorityQueue<EventInfo> sequence;
   boolean active = true;
   
   public BenchmarkEventList() {
      this(new PriorityQueue<EventInfo>());
   }
   
   public BenchmarkEventList(PriorityQueue<EventInfo> eventList) {
      this.sequence = eventList;
   }
   
   public BenchmarkEventList(BenchmarkEventList other) {
      this.sequence = new PriorityQueue<EventInfo>(other.sequence);
   }
   
   public synchronized void stopLogging() {
      active = false;
   }
   
   public synchronized boolean isEmpty() {
      return sequence.isEmpty();
   }
   
   public synchronized void addEvent(EventInfo ev) {
      if (active) sequence.add(ev);
   }
   
   public synchronized EventInfo takeNextEvent() {
      return sequence.remove();
   }
   
   public synchronized PriorityQueue<EventInfo> getEventSequence() {
      return sequence;
   }
   
   public synchronized void Merge(BenchmarkEventList other) {
      sequence.addAll(other.sequence);
   }
   
}
