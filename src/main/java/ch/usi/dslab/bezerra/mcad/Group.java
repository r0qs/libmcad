package ch.usi.dslab.bezerra.mcad;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

@SuppressWarnings("rawtypes")
public abstract class Group {
   public static Logger log = Logger.getLogger(Group.class);
   
   static Class groupImplementationClass = Group.class;
   
   static HashMap<Integer, Group> groupMap;
   
   static {
      groupMap = new HashMap<Integer, Group>();
   }
   
   public static void changeGroupImplementationClass(Class groupImplementation) {
      Group.groupImplementationClass = groupImplementation;
   }
   
   public static List<Group> getAllGroups() {
      ArrayList<Group> allGroups = new ArrayList<Group>();
      
      for (Entry<Integer, Group> entry : groupMap.entrySet())
          allGroups.add(entry.getValue());
      
      return allGroups;
   }
   
   public static Group getGroup(int id) {
      return groupMap.get(id);
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
         log.info("Group " + id + " already exists");
      }
            
      return g;
   }

   int groupId;

   public Group(int id) {
      log.setLevel(Level.OFF);
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
   
   public abstract List<Integer> getMembers();
   
}
