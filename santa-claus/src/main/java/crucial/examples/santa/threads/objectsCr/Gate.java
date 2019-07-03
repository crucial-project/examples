package crucial.examples.santa.threads.objectsCr;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class Gate implements Serializable {
    @Id
    String name;
    private int capacity, current;
    private boolean disbanded = false;

    public Gate() {}

    public Gate(String id) {
        name = id;
    }

    public void set(int capacity) {
//        System.out.println("Gate " + name + " created.");
        this.capacity = capacity;
        this.current = 0;
        disbanded = false;
    }

    public synchronized boolean passGate() throws InterruptedException {
//        System.out.println("[Gate] A thread attempts to pass the gate. Current: "+current);
        while (current == 0) {
//            System.out.println("[Gate] The thread waits.");
            if (disbanded) return false;
            this.wait();
        }
        current--;
//        System.out.println("[Gate] The thread passes the gate. current: "+current);
        if (current == 0) this.notify();
        return true;
    }

    public synchronized void operateGate() throws InterruptedException {
//        System.out.println("[Gate] Manager thread operates the gate.");
        current = capacity;
        this.notifyAll();
        while (current != 0) this.wait();
//        System.out.println("[Gate] Manager thread knows everyone has passed.");
    }

    public synchronized void disband() {
        disbanded = true;
        this.notifyAll();
    }
}
