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

import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import spread.SpreadException;
import ch.usi.dslab.bezerra.mcad.minimal.MinimalMulticastClient;
import ch.usi.dslab.bezerra.mcad.ridge.RidgeMulticastAgent;
import ch.usi.dslab.bezerra.mcad.spread.SpreadMulticastAgent;
import ch.usi.dslab.bezerra.mcad.spread.SpreadMulticastClient;
import ch.usi.dslab.bezerra.mcad.spread.SpreadMulticastServer;
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
            RidgeMulticastAgent rcmagent = new RidgeMulticastAgent(configFile, false, clientId);
            return rcmagent.getClient();
         }
         else if (agent_type.equals("SpreadMulticastAgent")) {
             logger.info("Creating SpreadMulticastAgent");
             SpreadMulticastAgent spreadAgent = new SpreadMulticastAgent(configFile, false, clientId);
             System.out.println("SpreadMulticastAgent created!");
             return new SpreadMulticastClient(spreadAgent, clientId);
         }
         else {
            logger.error("agent_type field in " + configFile + " didn't match any known MulticastAgentOld type");
         }
      }
      catch(ParseException | IOException | SpreadException e) {
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
            logger.info("Creating URPMulticastServer");
            URPMcastAgent urpmagent = new URPMcastAgent(configFile, true, serverId);
            return urpmagent.getMulticastServer();
         }
         else if (agent_type.equals("RidgeMulticastAgent")) {
            logger.info("Creating RidgeMulticastServer");
            RidgeMulticastAgent rcmagent = new RidgeMulticastAgent(configFile, true, serverId);
            return rcmagent.getServer();
         }
         else if (agent_type.equals("SpreadMulticastAgent")) {
             logger.info("Creating SpreadMulticastAgent");
             SpreadMulticastAgent spreadAgent = new SpreadMulticastAgent(configFile, true, serverId);
             System.out.println("SpreadMulticastAgent created!");
             return new SpreadMulticastServer(spreadAgent, serverId);
         }
         else {
            logger.error("agent_type field in " + configFile + " didn't match any known MulticastAgent type");
         }
      }
      catch(ParseException | IOException | SpreadException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      return server;
   }
}
