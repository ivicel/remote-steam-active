package info.ivicel.steam.slowdowng;

/**
 * Created by sedny on 26/08/2017.
 */

public class Server {
    private String name;
    private String address;
    private String id;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return "[" + this.getName() + "] " + this.getAddress();
    }
}
