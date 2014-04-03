package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import ch.usi.dslab.bezerra.ridge.Ensemble;

public class RidgeEnsembleData {
   static ArrayList<RidgeEnsembleData> ensemblesList;
   private final static Logger log = Logger.getLogger(RidgeEnsembleData.class);

   int ensembleId;
   ArrayList<RidgeGroup> destinationGroups;
   
   //TODO : set this value
   Ensemble ensemble;
   
   static {
      ensemblesList = new ArrayList<RidgeEnsembleData>();
   }

   static RidgeEnsembleData getById(int id) {
      for (RidgeEnsembleData red : ensemblesList)
         if (red.getId() == id)
            return red;
      return null;
   }

   public RidgeEnsembleData (int ensembleId) {
      this.ensembleId = ensembleId;
      ensemblesList.add(this);
      destinationGroups = new ArrayList<RidgeGroup>();
   }

   public int getId() {
      return ensembleId;
   }
   
   public void addDestinationGroup(RidgeGroup g) {
      if (destinationGroups.contains(g) == false)
         destinationGroups.add(g);
   }
   
   public Ensemble getEnsemble() {
      return ensemble;
   }

}
