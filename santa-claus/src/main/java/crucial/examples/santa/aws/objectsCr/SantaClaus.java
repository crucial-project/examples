package crucial.examples.santa.aws.objectsCr;

import crucial.execution.aws.AWSLambdaThread;
import org.infinispan.crucial.CAtomicBoolean;
import org.infinispan.crucial.CAtomicInt;
import org.infinispan.crucial.CLogger;
import org.infinispan.crucial.CSemaphore;
import org.infinispan.crucial.CrucialClient;
import org.infinispan.crucial.Shared;

import java.util.ArrayList;


public class SantaClaus implements Runnable{
    static CrucialClient cc = CrucialClient.getClient("yourIPhere:11222");
    private final static int END_OF_FAITH = 2033;
    // helper variables for program termination and output
//    static AtomicBoolean kidsStillBelieveInSanta = new AtomicBoolean(true);
    private static CAtomicBoolean kidsStillBelieveInSanta = cc.getBoolean("kidsBelief");
    //    static AtomicInteger year = new AtomicInteger(2018);
    private static CAtomicInt year = cc.getAtomicInt("year", 2018);
//    private static final Semaphore disbelief = new Semaphore(0);
//    private static CSemaphore disbelief = cc.getSemaphore("belief", 0);
    //    static final Semaphore santasAttention = new Semaphore(0, true);
    private CSemaphore santasAttention = cc.getSemaphore("santa", 0);

    static CLogger log = cc.getLog("santa");

    // problem dimensions
    private final static int NUMBER_OF_REINDEER = 9;
    private final static int NUMBER_OF_ELVES = 10;
    private final static int ELVES_NEEDED_TO_WAKE_SANTA = 3;

    @Shared(key = "elvesGroup")
    Group elvesGroup = new Group();
    @Shared(key = "reinsGroup")
    Group reinsGroup = new Group();

    public SantaClaus(){}

    public static void main(String[] args){
        kidsStillBelieveInSanta.set(true);

        SantaClaus sc = new SantaClaus();
        sc.elvesGroup.setUp("elvesGroup", ELVES_NEEDED_TO_WAKE_SANTA);
        sc.reinsGroup.setUp("reinsGroup", NUMBER_OF_REINDEER);

        ArrayList<Thread> threads = new ArrayList<>();
        threads.add(new AWSLambdaThread(sc));
        for (int i = 0; i < NUMBER_OF_ELVES; ++ i)
            threads.add(new AWSLambdaThread(new Elf(i)));
        for (int i = 0; i < NUMBER_OF_REINDEER; ++ i)
            threads.add(new AWSLambdaThread(new Reindeer(i)));

//        for (Thread t : threads)
//            ((CloudThread)t).setLocal(true);
        log.print("Once upon in the year " + year.get() + " :");
        for (Thread t : threads)
            t.start();

        try {
            // wait until !kidsStillBelieveInSanta
            for (Thread t : threads) // All threads end naturally if kids don't believe in santa
                t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AWSLambdaThread.closeInvoker();
        log.print("Santa dies of boredom.");
    }


    @Override
    public void run(){
        WorkRoom work;
        while (kidsStillBelieveInSanta.get()){
            try {
                santasAttention.acquire();

                // Check reins first
                work = reinsGroup.awaitGroup();
                if (work != null){
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
//                        disbelief.release();
                        // With kids belief to false, all threads will naturally end
                    }
                    // Go back to sleep
                    continue;
                }
                // else check the elves
                work = elvesGroup.awaitGroup();
                if (work != null){
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