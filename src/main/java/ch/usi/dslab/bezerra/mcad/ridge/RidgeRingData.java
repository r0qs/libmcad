package ch.usi.dslab.bezerra.mcad.ridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class URPRingData {
   static ArrayList<URPRingData> ringsList;
   private final static Logger log = Logger.getLogger(URPRingData.class);
   
   ArrayList<URPGroup> destinationGroups;
   
   int    ringId;
   String coordinatorAddress;
   int    coordinatorPort;
   
   SocketChannel coordinatorConnection;
   
   static {
      ringsList = new ArrayList<URPRingData>();
   }
   
   static URPRingData getById(int id) {
      for (URPRingData urd : ringsList)
         if (urd.getId() == id)
            return urd;
      return null;
   }
   
   public URPRingData (int ringId) {
      this.ringId = ringId;
      ringsList.add(this);
      destinationGroups = new ArrayList<URPGroup>();
   }
   
   public void setCoordinator(String address, int port) {
      coordinatorAddress = address;
      coordinatorPort    = port;
   }
   
   public void connectToCoordinator() {
      try {
         coordinatorConnection = SocketChannel.open();
         coordinatorConnection.connect(new InetSocketAddress(coordinatorAddress, coordinatorPort));
      } catch (IOException e) {
         log.fatal(" !!! Couldn't connect to ring " + this.ringId);
         e.printStackTrace();
         System.exit(1);
      }
      
   }
   
   public int getId() {
      return ringId;
   }
   
   public String getProposerAddress() {
      return coordinatorAddress;
   }
   
   public int getProposerPort() {
      return coordinatorPort;
   }
   
   public void addDestinationGroup(URPGroup g) {
      if (destinationGroups.contains(g) == false)
         destinationGroups.add(g);
   }

}
