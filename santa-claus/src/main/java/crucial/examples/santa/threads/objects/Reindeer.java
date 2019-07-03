package crucial.examples.santa.threads.objects;

import java.util.Random;

import static crucial.examples.santa.threads.objects.SantaClaus.kidsStillBelieveInSanta;
import static crucial.examples.santa.threads.objects.SantaClaus.year;

public class Reindeer implements Runnable {
    private static Random generator = new Random();
    private int name;
    private Group waitsForSanta;

    Reindeer(Group g, int id) {
        waitsForSanta = g;
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
                    System.out.println("=== Reindeer reunion for Christmas " + year.get() + " ===");

                // Santa harnesses the reindeer
                if (!santasStable.inGate().passGate())
                    continue;  // Reindeer fired for faith disappearance

                Thread.sleep(generator.nextInt(20));
                System.out.println("Reindeer " + name + " delivers toys.");

                // unharness the reindeer
                santasStable.outGate().passGate();

                // All reindeer go back on vacation until next Christmas
            } catch (InterruptedException e) {
                // thread interruption
            }
        }
        System.out.println("Reindeer " + name + " retires");
    }
}
