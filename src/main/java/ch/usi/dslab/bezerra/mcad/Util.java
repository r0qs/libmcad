package ch.usi.dslab.bezerra.mcad;

public class Util {
   public static Object[] mergeArrays(Object o, Object... arr) {
      Object[] newArray = new Object[arr.length + 1];
      newArray[0] = o;
      System.arraycopy(arr, 0, newArray, 1, arr.length);
      return newArray;
  }
}
