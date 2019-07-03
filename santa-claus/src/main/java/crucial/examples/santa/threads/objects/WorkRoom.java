package crucial.examples.santa.threads.objects;


public class WorkRoom {
    private Gate in, out;

    public WorkRoom(int capacity) {
        in = new Gate(capacity);
        out = new Gate(capacity);
    }

    Gate inGate() {
        return in;
    }

    Gate outGate() {
        return out;
    }
}
