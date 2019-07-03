package crucial.examples.santa.threads.objectsCr;

import org.infinispan.crucial.CAtomicBoolean;
import org.infinispan.crucial.CAtomicInt;
import org.infinispan.crucial.CLogger;
import org.infinispan.crucial.CSemaphore;
import org.infinispan.crucial.CrucialClient;
import org.infinispan.crucial.Shared;

import java.util.ArrayList;


public class SantaClaus implements Runnable {
    static CrucialClient cc = CrucialClient.getClient(); // Default client connects to localhost

    // helper variables for program termination and output
    private final static int END_OF_FAITH = 2025;
    private static CAtomicBoolean kidsStillBelieveInSanta = cc.getBoolean("kidsBelief");
    private static CAtomicInt year = cc.getAtomicInt("year", 2018);
    private CSemaphore santasAttention = cc.getSemaphore("santa", 0);

    // Centralized logging object
    static CLogger log = cc.getLog("santa");

    // problem dimensions
    private final static int NUMBER_OF_REINDEER = 9;
    private final static int NUMBER_OF_ELVES = 10;
    private final static int ELVES_NEEDED_TO_WAKE_SANTA = 3;

    @Shared(key = "elvesGroup")
    Group elvesGroup = new Group();
    @Shared(key = "reinsGroup")
    Group reinsGroup = new Group();


    public static void main(String[] args) {
        kidsStillBelieveInSanta.set(true);

        SantaClaus sc = new SantaClaus();
        sc.elvesGroup.setUp("elvesGroup", ELVES_NEEDED_TO_WAKE_SANTA);
        sc.reinsGroup.setUp("reinsGroup", NUMBER_OF_REINDEER);

        // Create entities
        ArrayList<Thread> threads = new ArrayList<>();
        threads.add(new Thread(sc));
        for (int i = 0; i < NUMBER_OF_ELVES; ++i)
            threads.add(new Thread(new Elf(i)));
        for (int i = 0; i < NUMBER_OF_REINDEER; ++i)
            threads.add(new Thread(new Reindeer(i)));

        log.print("Once upon in the year " + year.get() + " :");
        for (Thread t : threads)
            t.start();

        try {
            // wait until !kidsStillBelieveInSanta
            // All threads will naturally end on the condition
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Santa, bored, retires.");
    }


    /**
     * Logic of the Santa Claus entity
     */
    @Override
    public void run() {
        WorkRoom work;
        while (kidsStillBelieveInSanta.get()) {
            try {
                santasAttention.acquire();

                // Check reins first
                work = reinsGroup.awaitGroup();
                if (work != null) {
                    log.print("=== Delivery for Christmas " + year.get() + " ===");
                    // Harness the reindeer
                    work.inGate().operateGate();
                    // Go deliver toys
                    log.print("HO! HO! HO! Let's deliver toys!");
                    // Unharness the reindeer
                    work.outGate().operateGate();
                    log.print("=== Toys are delivered ===");
                    if (year.incrementAndGet() == END_OF_FAITH) {
                        log.print("Faith has vanished from the world");
                        kidsStillBelieveInSanta.set(false);
                        reinsGroup.disband().inGate().disband();
                        elvesGroup.disband().inGate().disband();
                        // With kids belief to false, all threads will naturally end
                    }
                    // Go back to sleep
                    continue;
                }
                // else check the elves
                work = elvesGroup.awaitGroup();
                if (work != null) {
                    // Let the elves enter the shop
                    work.inGate().operateGate();
                    // Solve their problems
                    log.print("HO! HO! HO! Let's solve those problems!");
                    // Sent the elves out
                    work.outGate().operateGate();
                    // Go back to sleep
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.print("Santa goes to an eternal sleep.");
    }
}