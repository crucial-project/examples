package crucial.examples.santa.threads.objects;

import java.util.Random;

import static crucial.examples.santa.threads.objects.SantaClaus.kidsStillBelieveInSanta;

public class Elf implements Runnable {
    private static Random generator = new Random();
    private int name;
    private Group waitsForSanta;

    Elf(Group g, int elfName) {
        waitsForSanta = g;
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
                System.out.println("Elf " + name + " has a problem.");
                santasShop = waitsForSanta.joinGroup();
                if (santasShop == null)
                    continue; // Elf fired for faith disappearance

                System.out.println("Elf " + name + " has a group.");

                // Enter the shop when the group is full with Santa's permission
                if (!santasShop.inGate().passGate())
                    continue;  // Elf fired for faith disappearance

                Thread.sleep(generator.nextInt(500));
                System.out.println("Elf " + name + " has solved the problem.");

                // Exit the shop when the problem is solved
                santasShop.outGate().passGate();

                // All elves exit the shop at the same time and go back to work

            } catch (InterruptedException e) {
                // thread interrupted
            }
        }
        System.out.println("Elf " + name + " retires.");
    }
}
