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

import org.json.simple.JSONObject;

public class Util {
   public static Object[] mergeArrays(Object o, Object... arr) {
      Object[] newArray = new Object[arr.length + 1];
      newArray[0] = o;
      System.arraycopy(arr, 0, newArray, 1, arr.length);
      return newArray;
  }
   
   public static double getJSDouble(JSONObject jsobj, String fieldName) {
      return ((Number)jsobj.get(fieldName)).doubleValue();
   }
   
   public static int getJSInt(JSONObject jsobj, String fieldName) {
      return ((Long) jsobj.get(fieldName)).intValue();
   }
   
   public static int getInt(Object obj) {
      return ((Long) obj).intValue();
   }
}
