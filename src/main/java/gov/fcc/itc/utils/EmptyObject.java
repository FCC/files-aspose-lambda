package gov.fcc.itc.utils;

public class EmptyObject implements java.io.Serializable {
  // Use id to let EmptyObjects work when serialized.
  private int id;

  public EmptyObject(int id) { this.id = id; }
  
  public boolean equals ( Object obj ) {
    return (  (obj != null) && 
              (getClass() == obj.getClass()) &&
              (this.id == ((EmptyObject)obj).id) ) ;
  }

  public String toString() { return(""); }
}
