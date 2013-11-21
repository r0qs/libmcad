package ch.usi.dslab.bezerra.mcad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Message implements Serializable {
   private static final long serialVersionUID = 4104839889665917909L;
   static Map<Long,Kryo> threadKryos = new ConcurrentHashMap<Long,Kryo>();

   // *******************************************
   // LATENCY TIMELINE
   // *******************************************

   public long t_client_send;
   public long t_batch_ready;
   public long t_learner_delivered;
   public long t_learner_deserialized;
   public long t_command_enqueued;
   public long t_ssmr_dequeued;
   public long t_execution_start;
   public long t_server_send;
   public long t_client_receive;
   
   public long piggyback_proposer_serialstart;
   public long piggyback_proposer_serialend;

   // *******************************************
   // *******************************************

   ArrayList<Object> contents;
   int next = 0;
   
   int byteArraysAggregatedLength = 0;
   
   public Message() {
      contents = new ArrayList<Object>();
   }
   
   public Message(Object... objs) {
      contents = new ArrayList<Object>(objs.length);
      addItems(objs);
   }
   
   public void addItems(Object... objs) {
      contents.ensureCapacity(contents.size() + objs.length);
      for (Object o : objs) {
         if (o instanceof Object[])
            addItems((Object[]) o);
         else
            contents.add(o);
         
         // counting the total size of added arrays
         if (o instanceof byte[]) {
            byteArraysAggregatedLength += ((byte[]) o).length;
            
         }
      }
   }
   
   public void pushFront(Object... objs) {
      contents.ensureCapacity(contents.size() + objs.length);
      int pos = 0;
      for (Object o : objs) {
            contents.add(pos++, o);

         // counting the total size of added arrays
         if (o instanceof byte[]) {
            byteArraysAggregatedLength += ((byte[]) o).length;
         }
      }
   }
   
   public void rewind() {
      next = 0;
   }
   
   public boolean hasNext() {
      return next < contents.size();
   }

   public Object getNext() {
      return get(next++);
   }
   
   public Object peekNext() {
      return get(next);
   }

   public Object get(int index) {
      if (index >= contents.size())
         return null;
      return contents.get(index);
   }
   
   public int count() {
      return contents.size();
   }
   
   public int getByteArraysAggregatedLength() {
      return byteArraysAggregatedLength;
   }
   
   public int getSerializedLength() {
      return getBytes().length;
   }
   
   static Kryo getCurrentThreadKryo() {
      long tid = Thread.currentThread().getId();
      Kryo kryo = threadKryos.get(tid);
      if (kryo == null) {
         kryo = new Kryo();
         kryo.register(Message.class);
         threadKryos.put(tid, kryo);
      }
      return kryo;
   }

   public byte[] getBytes() {
      Kryo kryo = getCurrentThreadKryo();      
      
      byte[] bytes = null;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Output out = new Output(bos);
      try {
         kryo.writeObject(out, this);
         out.flush();
         bytes = bos.toByteArray();
         bos.close();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         try {
            bos.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return bytes;
   }

   public static Message createFromBytes(byte[] bytes) {
      Kryo kryo = getCurrentThreadKryo();
      
      Message msg = null;
      Input in = new Input(bytes);
//      System.out.println("Creating message from a " + bytes.length + " bytes long array.");
      msg = kryo.readObject(in, Message.class);
      in.close();
      msg.rewind();
      return msg;
   }
}
