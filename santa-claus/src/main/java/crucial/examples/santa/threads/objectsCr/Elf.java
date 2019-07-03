package crucial.examples.santa.threads.objectsCr;


import org.infinispan.crucial.CAtomicBoolean;
import org.infinispan.crucial.Shared;

import java.util.Random;

import static crucial.examples.santa.threads.objectsCr.SantaClaus.cc;
import static crucial.examples.santa.threads.objectsCr.SantaClaus.log;

public class Elf implements Runnable {
    private static Random generator = new Random();
    @Shared(key = "elvesGroup")
    public Group waitsForSanta = new Group();
    private CAtomicBoolean kidsStillBelieveInSanta = cc.getBoolean("kidsBelief");
    private int name;

    public Elf(int elfName) {
        name = elfName;
    }

    /**
     * Logic of the Elf entities
     */
    @Override
    public void run() {
        WorkRoom santasShop;
        while (kidsStillBelieveInSanta.get()) {
            try {
                // Work until a problem arises
                Thread.sleep(generator.nextInt(2000));

                // Join the group to ask Santa
                log.print("Elf " + name + " has a problem.");
                santasShop = waitsForSanta.joinGroup();
                if (santasShop == null)
                    continue; // Elf fired for faith disappearance

                log.print("Elf " + name + " has a group.");

                // Enter the shop when the group is full with Santa's permission
                if (!santasShop.inGate().passGate())
                    continue;  // Elf fired for faith disappearance

                Thread.sleep(generator.nextInt(500));
                log.print("Elf " + name + " has solved the problem.");

                // Exit the shop when the problem is solved
                santasShop.outGate().passGate();

                // All elves exit the shop at the same time and go back to work

            } catch (InterruptedException e) {
                // thread interrupted
            }
        }
        log.print("Elf " + name + " retires.");
    }
}
