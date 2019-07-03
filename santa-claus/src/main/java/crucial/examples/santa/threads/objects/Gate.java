package crucial.examples.santa.threads.objects;

class Gate {
    private int capacity, current;
    private boolean disbanded = false;

    Gate(int capacity) {
        this.capacity = capacity;
        this.current = 0;
    }

    synchronized boolean passGate() throws InterruptedException {
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

    synchronized void operateGate() throws InterruptedException {
//        System.out.println("[Gate] Manager thread operates the gate.");
        current = capacity;
        this.notifyAll();
        while (current != 0) this.wait();
//        System.out.println("[Gate] Manager thread knows everyone has passed.");
    }

    synchronized void disband() {
        disbanded = true;
        this.notifyAll();
    }
}
