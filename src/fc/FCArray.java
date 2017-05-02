package fc;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Created by vaksenov on 16.01.2017.
 */
public class FCArray {
    public static abstract class FCRequest {
        int pos = -1;

        public abstract boolean holdsRequest();
    }

    static final AtomicIntegerFieldUpdater<FCArray> lockUpdater =
            AtomicIntegerFieldUpdater.newUpdater(FCArray.class, "lock");
    volatile int lock;

    static final AtomicIntegerFieldUpdater<FCArray> lengthUpdater =
            AtomicIntegerFieldUpdater.newUpdater(FCArray.class, "length");
    volatile int length;

    private final AtomicReferenceArray<FCRequest> requests;

    public FCArray(int threads) {
        requests = new AtomicReferenceArray<>(threads);
        length = 0;
    }

    public boolean tryLock() {
        return lock == 0 && lockUpdater.compareAndSet(this, 0, 1);
    }

    public void unlock() {
        lock = 0;
    }

    public boolean isLocked() {
        return lock != 0;
    }

    public void addRequest(FCRequest request) {
        if (!request.holdsRequest()) { // The request is not old yet
            return;
        }

        if (request.pos == -1) {
            
            request.pos = lengthUpdater.getAndIncrement(this);
            requests.set(request.pos, request);
        }
    }

    public ArrayList<FCRequest> loadRequestsList() {
        int end = length;
        ArrayList<FCRequest> requests = new ArrayList<>();
        for (int i = 0; i < end; i++) {
            FCRequest request = this.requests.get(i);
            if (request != null && request.holdsRequest()) {
                requests.add(request);
            }
        }
        return requests;
    }

    public FCRequest[] loadRequests() {
        ArrayList<FCRequest> requests = loadRequestsList();
        return requests.toArray(new FCRequest[0]);
    }

    public void cleanup() {
    }
}
