package crucial.examples.santa.threads.objectsCr;

import org.infinispan.crucial.CSemaphore;

import java.io.Serializable;

import static crucial.examples.santa.threads.objectsCr.SantaClaus.cc;

public class Group implements Serializable {
    private String name;

    private int size, available, iters;
    private WorkRoom workRoom;
    private boolean disbanded = false;
    private CSemaphore santasAttention = cc.getSemaphore("santa");

    public Group() {}

    public void setUp(String id, int size) {
//        System.out.println("Group " + id + " created.");
        name = id;
        iters = 0;
        this.size = size;
        this.available = size;
        workRoom = new WorkRoom(name + iters + "-gate", size);
        disbanded = false;
    }

    public synchronized WorkRoom awaitGroup() {
//        System.out.println("[Group] Main thread waits for group. Available: "+available);
        if (available > 0) {
            return null;
        }
//        System.out.println("[Group] Main thread restarts the group.");
        WorkRoom ret = workRoom;
        available = size;
        workRoom = new WorkRoom(name + ++iters + "-gate", size);
        this.notifyAll();
        return ret;
    }

    public synchronized WorkRoom joinGroup() throws InterruptedException {
//        System.out.println("[Group] A thread tries to join. Available: "+available);
        while (available == 0) {
//            System.out.println("[Group] The thread waits.");
            if (disbanded) return null;
            this.wait();
//            System.out.println("[Group] A thread tries to join again. Available: "+available);
        }
        available--;
//        System.out.println("[Group] Available decreases to "+available);
        if (available == 0) santasAttention.release(); // this.notifyAll();
        return workRoom;
    }


    public synchronized WorkRoom disband() {
        disbanded = true;
        this.notifyAll();
        return workRoom;
    }
}
