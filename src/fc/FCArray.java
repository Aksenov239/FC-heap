package fc;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceArray;

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
/*        if (!request.holdsRequest()) { // The request is not old yet
            return;
        }*/

        if (request.pos == -1) {
            request.pos = lengthUpdater.getAndIncrement(this);
            requests.set(request.pos, request);
        }
    }

    private final FCRequest[] tlReq =  new FCRequest[fc.FC.MAX_THREADS + 1];

    public FCRequest[] loadRequests() {
        int end = length;
        FCRequest[] requests = tlReq;
        int j = 0;
        for (int i = 0; i < end; i++) {
            FCRequest request = this.requests.get(i);
            if (request != null && request.holdsRequest()) {
                requests[j++] = request;
            }
        }
        requests[j] = null;
        return requests;
    }

    public void cleanup() {
    }
}
