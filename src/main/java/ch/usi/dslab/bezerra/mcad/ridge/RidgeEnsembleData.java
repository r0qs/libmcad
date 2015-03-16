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

package ch.usi.dslab.bezerra.mcad.ridge;

import java.util.ArrayList;

//import org.apache.log4j.Logger;

import ch.usi.dslab.bezerra.ridge.Ensemble;

public class RidgeEnsembleData {
   static ArrayList<RidgeEnsembleData> ensemblesList;
//   private final static Logger log = Logger.getLogger(RidgeEnsembleData.class);

   int ensembleId;
   ArrayList<RidgeGroup> destinationGroups;
   
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

   public RidgeEnsembleData (int ensembleId, Ensemble ensemble) {
      this.ensembleId = ensembleId;
      this.ensemble   = ensemble;
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
