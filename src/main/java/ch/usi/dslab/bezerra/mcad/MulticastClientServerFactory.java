package ch.usi.dslab.bezerra.mcad;

import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ch.usi.dslab.bezerra.mcad.minimal.MinimalMulticastClient;
import ch.usi.dslab.bezerra.mcad.ridge.RidgeMulticastAgent;
import ch.usi.dslab.bezerra.mcad.uringpaxos.URPMcastAgent;
import ch.usi.dslab.bezerra.mcad.uringpaxos.URPMulticastClient;


public class MulticastClientServerFactory {
   public static final Logger logger = LogManager.getLogger(MulticastAgentFactory.class);
   
   public static MulticastClient getClient(int clientId, String configFile) {
      MulticastClient client = null;
      
      try {
      
         JSONParser parser = new JSONParser();
         
         Object obj = parser.parse(new FileReader(configFile));         
         JSONObject config = (JSONObject) obj;         
         String agent_type = (String) config.get("agent_class");
         
         logger.info("Agent Type: " + agent_type);
         
         if (agent_type.equals("MinimalMcastAgent")) {
            logger.info("Creating MinimalMcastAgent");
            return new MinimalMulticastClient(clientId, configFile);
         }
         else if (agent_type.equals("URPMcastAgent")) {
            logger.info("Creating URPMcastAgent");
            return new URPMulticastClient(clientId, configFile);
         }
         else if (agent_type.equals("RidgeMulticastAgent")) {
            logger.info("Creating RidgeMulticastAgent");
            RidgeMulticastAgent rcmagent = new RidgeMulticastAgent(configFile, clientId, false);
            return rcmagent.getClient();
         }
         else {
            logger.error("agent_type field in " + configFile + " didn't match any known MulticastAgentOld type");
         }
      }
      catch(ParseException | IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      return client;
   }

   public static MulticastServer getServer(int serverId, String configFile) {
      MulticastServer server = null;
      
      try {
      
         JSONParser parser = new JSONParser();
         
         Object obj = parser.parse(new FileReader(configFile));         
         JSONObject config = (JSONObject) obj;         
         String agent_type = (String) config.get("agent_class");
         
         logger.info("Agent Type: " + agent_type);
         
         if (agent_type.equals("MinimalMcastAgent")) {
            // TODO
         }
         else if (agent_type.equals("URPMcastAgent")) {
            int groupId = Util.getJSInt(config, "localnode_group_id");
            String common_config_file = (String) config.get("common_config_file");
            URPMcastAgent urpmagent = new URPMcastAgent(common_config_file, true, groupId, serverId);
            return urpmagent.getMulticastServer();
         }
         else if (agent_type.equals("RidgeMulticastAgent")) {
            logger.info("Creating RidgeMulticastServer");
            RidgeMulticastAgent rcmagent = new RidgeMulticastAgent(configFile, serverId, true);
            return rcmagent.getServer();
         }
         else {
            logger.error("agent_type field in " + configFile + " didn't match any known MulticastAgent type");
         }
      }
      catch(ParseException | IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      return server;
   }
}
