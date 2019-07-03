package crucial.examples.santa.threads.objects;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class SantaClaus implements Runnable {
    // helper variables for program termination and output
    private final static int END_OF_FAITH = 2025;
    static AtomicBoolean kidsStillBelieveInSanta = new AtomicBoolean(true);
    static AtomicInteger year = new AtomicInteger(2018);
    static final Semaphore santasAttention = new Semaphore(0, true);

    // problem dimensions
    private final static int NUMBER_OF_REINDEER = 9;
    private final static int NUMBER_OF_ELVES = 10;
    private final static int ELVES_NEEDED_TO_WAKE_SANTA = 3;

    private Group elvesGroup, reinsGroup;

    private SantaClaus(Group elves, Group reins) {
        elvesGroup = elves;
        reinsGroup = reins;
    }

    public static void main(String[] args) {
        Group elfGroup = new Group(ELVES_NEEDED_TO_WAKE_SANTA);
        Group reinGroup = new Group(NUMBER_OF_REINDEER);

        // Create entities
        ArrayList<Thread> threads = new ArrayList<>();
        threads.add(new Thread(new SantaClaus(elfGroup, reinGroup)));
        for (int i = 0; i < NUMBER_OF_ELVES; ++i)
            threads.add(new Thread(new Elf(elfGroup, i)));
        for (int i = 0; i < NUMBER_OF_REINDEER; ++i)
            threads.add(new Thread(new Reindeer(reinGroup, i)));
        System.out.println("Once upon in the year " + year + " :");
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
                    System.out.println("=== Delivery for Christmas " + year.get() + " ===");
                    // Harness the reindeer
                    work.inGate().operateGate();
                    // Go deliver toys
                    System.out.println("HO! HO! HO! Let's deliver toys!");
                    // Unharness the reindeer
                    work.outGate().operateGate();
                    System.out.println("=== Toys are delivered ===");
                    if (year.incrementAndGet() == END_OF_FAITH) {
                        System.out.println("Faith has vanished from the world");
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
                    System.out.println("HO! HO! HO! Let's solve those problems!");
                    // Sent the elves out
                    work.outGate().operateGate();
                    // Go back to sleep
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Santa goes to an eternal sleep.");
    }
}