package ch.usi.dslab.bezerra.mcad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import de.ruedigermoeller.serialization.FSTConfiguration;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;

public class Message implements Serializable {
   private static final long serialVersionUID = 4104839889665917909L;
   static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
   
   ArrayList<Object> contents;
   int next = 0;
   
   int byteArraysAggregatedLength = 0;
   
   public Message(Object... objs) {
      contents = new ArrayList<Object>(objs.length);
      addItems(objs);
   }
   
   public void addItems(Object... objs) {
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
   
   public boolean hasNext() {
      return next < contents.size();
   }

   public Object getNext() {
      return get(next++);
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

   public byte[] getBytes() {
      byte[] bytes = null;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      FSTObjectOutput out = null;
      try {
         out = conf.getObjectOutput(bos);
         out.writeObject(this, Message.class);
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
      Message msg = null;
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      FSTObjectInput in = null;
      try {
         in = conf.getObjectInput(bis);
         msg = (Message) in.readObject(Message.class);
         bis.close();
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         try {
            bis.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return msg;
   }
}
