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
