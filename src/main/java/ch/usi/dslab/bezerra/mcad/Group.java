/*

 Libmcad - A multicast adaptor library
 Copyright (C) 2015, University of Lugano
 
 This file is part of Libmcad.
 
 Libmcad is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libmcad is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 
*/

/**
 * @author Eduardo Bezerra - eduardo.bezerra@usi.ch
 */

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
   
   @Override
   public boolean equals(Object other) {
      return this.getId() == ((Group) other).getId();
   }
   
   @Override
   public int hashCode() {
      return this.getId();
   }
   
   public abstract List<Integer> getMembers();
   
}
