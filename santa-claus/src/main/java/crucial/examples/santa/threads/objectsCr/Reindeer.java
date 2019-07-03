package crucial.examples.santa.threads.objectsCr;


import org.infinispan.crucial.CAtomicBoolean;
import org.infinispan.crucial.CAtomicInt;
import org.infinispan.crucial.Shared;

import java.util.Random;

import static crucial.examples.santa.threads.objectsCr.SantaClaus.cc;
import static crucial.examples.santa.threads.objectsCr.SantaClaus.log;

public class Reindeer implements Runnable {
    private static Random generator = new Random();
    @Shared(key = "reinsGroup")
    public Group waitsForSanta = new Group();
    private CAtomicBoolean kidsStillBelieveInSanta = cc.getBoolean("kidsBelief");
    private CAtomicInt year = cc.getAtomicInt("year");
    private int name;

    public Reindeer(int id) {
        name = id;
    }

    /**
     * Logic of the reindeer entities
     */
    public void run() {
        WorkRoom santasStable;
        while (kidsStillBelieveInSanta.get()) {
            try {
                // Wait for Christmas in a paradise island
                Thread.sleep(900 + generator.nextInt(200));

                // Join the group to take Santa on delivery
                santasStable = waitsForSanta.joinGroup();
                if (santasStable == null)
                    continue; // Reindeer fired for faith disappearance

                // Now all reindeer are ready for delivery
                if (name == 0)
                    log.print("=== Reindeer reunion for Christmas " + year.get() + " ===");

                // Santa harnesses the reindeer
                if (!santasStable.inGate().passGate())
                    continue;  // Reindeer fired for faith disappearance

                Thread.sleep(generator.nextInt(20));
                log.print("Reindeer " + name + " delivers toys.");

                // unharness the reindeer
                santasStable.outGate().passGate();

                // All reindeer go back on vacation until next Christmas
            } catch (InterruptedException e) {
                // thread interruption
            }
        }
        log.print("Reindeer " + name + " retires");
    }
}
