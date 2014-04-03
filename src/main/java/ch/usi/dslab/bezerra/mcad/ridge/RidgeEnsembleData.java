package ch.usi.dslab.bezerra.mcad.ridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class RidgeEnsembleData {
   static ArrayList<RidgeEnsembleData> ringsList;
   private final static Logger log = Logger.getLogger(RidgeEnsembleData.class);
   
   ArrayList<RidgeGroup> destinationGroups;
   
   int    ringId;
   String coordinatorAddress;
   int    coordinatorPort;
   
   SocketChannel coordinatorConnection;
   
   static {
      ringsList = new ArrayList<RidgeEnsembleData>();
   }
   
   static RidgeEnsembleData getById(int id) {
      for (RidgeEnsembleData urd : ringsList)
         if (urd.getId() == id)
            return urd;
      return null;
   }
   
   public RidgeEnsembleData (int ringId) {
      this.ringId = ringId;
      ringsList.add(this);
      destinationGroups = new ArrayList<RidgeGroup>();
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
   
   public void addDestinationGroup(RidgeGroup g) {
      if (destinationGroups.contains(g) == false)
         destinationGroups.add(g);
   }

}
