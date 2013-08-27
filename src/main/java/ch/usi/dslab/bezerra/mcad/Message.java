package ch.usi.dslab.bezerra.mcad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Message {
   ArrayList<Object> contents;

   int next = 0;
   
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

   public byte[] getBytes() {
      byte[] bytes = null;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutput out = null;
      try {
         out = new ObjectOutputStream(bos);
         out.writeObject(this);
         bytes = bos.toByteArray();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         try {
            out.close();
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
      ObjectInput in = null;
      try {
         in = new ObjectInputStream(bis);
         msg = (Message) in.readObject();
      } catch (IOException | ClassNotFoundException e) {
         e.printStackTrace();
      } finally {
         try {
            bis.close();
            in.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return msg;
   }
}
