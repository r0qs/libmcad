package ch.usi.dslab.bezerra.mcad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Message implements Serializable {
   private static final long serialVersionUID = 4104839889665917909L;
   static Kryo kryo;
   
   static {
      kryo = new Kryo();
      kryo.register(Message.class);
   }
   
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

   public byte[] getBytes() {
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
      Message msg = null;
      Input in = new Input(bytes);
//      System.out.println("Creating message from a " + bytes.length + " bytes long array.");
      msg = kryo.readObject(in, Message.class);
      in.close();
      msg.rewind();
      return msg;
   }
}
