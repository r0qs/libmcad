package ch.usi.dslab.bezerra.libmcad;

import java.util.HashMap;

public class Group {
   static HashMap<Integer, Group> groupMap;
   int groupId;
   
   public Group(int id) {
      groupId = id;
      groupMap.put(id, this);
   }
   
   public static Group getGroup(int id) {
      return groupMap.get(id);
   }
}
