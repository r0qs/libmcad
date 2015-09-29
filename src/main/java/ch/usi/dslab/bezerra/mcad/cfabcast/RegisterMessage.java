package ch.usi.dslab.bezerra.mcad.cfabcast;

import java.io.Serializable;

public class RegisterMessage implements Serializable {
  private final int id; 

  public RegisterMessage(int id) {
    this.id = id; 
  }
     
  public int getId() {
    return this.id;
  }

}
