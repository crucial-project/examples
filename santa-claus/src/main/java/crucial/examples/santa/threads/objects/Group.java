package crucial.examples.santa.threads.objects;

import static crucial.examples.santa.threads.objects.SantaClaus.santasAttention;

class Group {
    private int size, available;
    private WorkRoom workRoom;
    private boolean disbanded = false;

    Group(int size) {
        workRoom = new WorkRoom(size);
        this.size = size;
        this.available = size;
    }

    synchronized WorkRoom awaitGroup() {
//        System.out.println("[Group] Main thread waits for group. Available: "+available);
        if (available > 0) {
            return null;
        }
//        System.out.println("[Group] Main thread resets the group.");
        WorkRoom ret = workRoom;
        available = size;
        workRoom = new WorkRoom(size);
        this.notifyAll();
        return ret;
    }

    synchronized WorkRoom joinGroup() throws InterruptedException {
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

    synchronized WorkRoom disband() {
        disbanded = true;
        this.notifyAll();
        return workRoom;
    }
}
