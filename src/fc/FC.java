package fc;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Created by vaksenov on 16.01.2017.
 */
public class FC {
    private final static int DELTA = 1000;
    public final static int MAX_THREADS = 64;

    static final AtomicIntegerFieldUpdater<FC> lockUpdater =
            AtomicIntegerFieldUpdater.newUpdater(FC.class, "lock");
    volatile int lock;

    static final AtomicReferenceFieldUpdater<FC, FCRequest> topUpdater =
            AtomicReferenceFieldUpdater.newUpdater(FC.class, FCRequest.class, "top");
    volatile FCRequest top;
    final FCRequest DUMMY;
    int current_timestamp;

    public FC() {
        DUMMY = new FCRequest() {
            public boolean holdsRequest() {
                return true;
            }
        };
        top = DUMMY;
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
        if (request.next != null) { // The request is not old yet
            return;
        }
        do {
            request.next = top;
        } while (!topUpdater.compareAndSet(this, request.next, request));
    }

    public ArrayList<FCRequest> loadRequestsList() {
        FCRequest tail = this.top;
        ArrayList<FCRequest> requests = new ArrayList<>();
        while (tail != DUMMY) {
            if (tail.holdsRequest()) {
                tail.timestamp = current_timestamp;
                requests.add(tail);
            }
            tail = tail.next;
        }
        return requests;
    }

    private static final FCRequest[] tlReq = new FCRequest[MAX_THREADS + 1];

    public FCRequest[] loadRequests() {
        FCRequest tail = this.top;
        FCRequest[] requests = tlReq;
        int i = 0;
        while (tail != DUMMY) {
            if (tail.holdsRequest()) {
                tail.timestamp = current_timestamp;
                requests[i++] = tail;
            }
            tail = tail.next;
        }
        requests[i] = null;
        return requests;
    }

    public void cleanup() {
        current_timestamp++;
        if (current_timestamp % DELTA != 0) {
            return;
        }
        FCRequest tail = this.top;
        while (tail.next != DUMMY) {
            FCRequest next = tail.next;
            if (next.timestamp + DELTA < current_timestamp && !next.holdsRequest()) { // The node has not updated for a long time, better to remove it
                tail.next = next.next;
                next.next = null;
            } else {
                tail = next;
            }
        }
    }
}
