package crucial.examples.santa.threads.objectsCr;


import java.io.Serializable;

public class WorkRoom implements Serializable{
    private Gate in, out;

    public WorkRoom(String name, int capacity){
        in = new Gate(name+"in");
        in.set(capacity);
        out = new Gate(name+"out");
        out.set(capacity);
    }

    public Gate inGate(){
        return in;
    }
    public Gate outGate(){
        return out;
    }
}
