package ch.usi.dslab.bezerra.mcad.minimal;

import java.util.ArrayList;

import ch.usi.dslab.bezerra.mcad.Group;

public class MMAGroup extends Group {
   static ArrayList<MMAGroup> groupList;
   
   static {
      groupList = new ArrayList<MMAGroup>();
   }
   
   ArrayList<MMANode> nodeList;

   public MMAGroup(int id) {
      super(id);
      nodeList = new ArrayList<MMANode>();
      groupList.add(this);
   }
   
   public void addNode(MMANode node) {
      nodeList.add(node);
   }

}
