package ch.usi.dslab.bezerra.mcad;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

@SuppressWarnings("rawtypes")
public class Group {
   static Class groupImplementationClass = Group.class;
   
   static HashMap<Integer, Group> groupMap;
   
   static {
      groupMap = new HashMap<Integer, Group>();
   }
   
   public static void changeGroupImplementationClass(Class groupImplementation) {
      Group.groupImplementationClass = groupImplementation;
   }
   
   @SuppressWarnings("unchecked")
   public static Group getOrCreateGroup(int id) {
      Group g = groupMap.get(id);
      
      if (g == null) {
         try {            
            g = (Group) groupImplementationClass.getConstructor(Integer.TYPE).newInstance(id);
         } catch (InstantiationException | 
                  IllegalAccessException |
                  NoSuchMethodException  |
                  InvocationTargetException e) {
            e.printStackTrace();
         } 
      }
      else {
         System.out.println("Group " + id + " already exists");
      }
            
      return g;
   }

   int groupId;
   
   public Group(int id) {
      groupId = id;
      if (groupMap.get(id) == null)
         groupMap.put(id, this);
   }
   
   public int getId() {
      return groupId;
   }
   
   public void setId(int groupId) {
      this.groupId = groupId;
   }
}
