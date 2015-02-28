package ch.usi.dslab.bezerra.mcad;

import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import spread.SpreadException;

import ch.usi.dslab.bezerra.mcad.minimal.MinimalMcastAgent;
import ch.usi.dslab.bezerra.mcad.ridge.RidgeMulticastAgent;
import ch.usi.dslab.bezerra.mcad.spread.SpreadMulticastAgent;
import ch.usi.dslab.bezerra.mcad.uringpaxos.URPMcastAgent;

public class MulticastAgentFactory {
   public static final Logger log = Logger.getLogger(MulticastAgentFactory.class);
   /* 
      
      The following method loads a multicast agent, with all its required
      internals, from a .json configuration file. The field "agent_class"
      informs this factory which specific agent implementation to build.
      The format of the .json file is the following:

      {
        "agent_class" : "MinimalMcastAgent" ,  
        (implementation specific data)
      }

    */
   public static MulticastAgent createMulticastAgent(String configFile, boolean isInGroup, int... ids) {
      try {
         log.setLevel(Level.OFF);
         
         log.info("Parsing the mcagent config file");
         
         JSONParser parser = new JSONParser();
         
         Object obj = parser.parse(new FileReader(configFile));         
         JSONObject config = (JSONObject) obj;         
         String agent_type = (String) config.get("agent_class");
         
         log.info("Agent Type: " + agent_type);
         
         if (agent_type.equals("MinimalMcastAgent")) {
            log.info("Creating MinimalMcastAgent");
            return new MinimalMcastAgent(configFile);
         }
         else if (agent_type.equals("URPMcastAgent")) {
            log.info("Creating URPMcastAgent");
            return new URPMcastAgent(configFile, isInGroup, ids);
         }
         else if (agent_type.equals("RidgeMulticastAgent")) {
            log.info("Creating RidgeMulticastAgent");
            return new RidgeMulticastAgent(configFile, ids[1], isInGroup);
         }
         else if (agent_type.equals("SpreadMulticastAgent")) {
             log.info("Creating SpreadMulticastAgent");
             return new SpreadMulticastAgent(configFile, isInGroup, ids[1]);
         }
         else {
            log.error("agent_type field in " + configFile + " didn't match any known MulticastAgent type");
         }
         
      } catch (IOException e) {
         e.printStackTrace();
      } catch (ParseException e) {
         e.printStackTrace();
      } catch (SpreadException e) {
		e.printStackTrace();
	}
      
      
      return null;
   }
}
