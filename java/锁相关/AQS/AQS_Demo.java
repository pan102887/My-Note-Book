import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractQueuedSynchronizer (AQS) Demo
 * 
 * AQS is the foundation for most synchronization utilities in Java's concurrent package.
 * It provides a framework for implementing blocking locks and related synchronizers
 * that rely on first-in-first-out (FIFO) wait queues.
 */
public class AQS_Demo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== AbstractQueuedSynchronizer (AQS) Demo ===\n");
        
        // Demo 1: Understanding AQS with ReentrantLock
        demoReentrantLock();
        
        // Demo 2: Custom AQS Implementation
        demoCustomAQS();
        
        // Demo 3: AQS State Management
        demoAQSStateManagement();
        
        // Demo 4: AQS Queue Operations
        demoAQSQueueOperations();
        
        // Demo 5: Simple Read-Write Lock
        demoSimpleReadWriteLock();
    }
    
    /**
     * Demo 1: Understanding AQS through ReentrantLock
     * ReentrantLock is built on top of AQS
     */
    private static void demoReentrantLock() throws InterruptedException {
        System.out.println("1. ReentrantLock (Built on AQS) Demo:");
        System.out.println("=====================================");
        
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch latch = new CountDownLatch(3);
        
        // Create multiple threads competing for the lock
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    System.out.println("Thread " + threadId + " attempting to acquire lock...");
                    lock.lock();
                    try {
                        System.out.println("Thread " + threadId + " acquired lock!");
                        
                        // Simulate work
                        TimeUnit.MILLISECONDS.sleep(1000);
                        
                        System.out.println("Thread " + threadId + " releasing lock");
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        System.out.println("All threads completed.\n");
    }
    
    /**
     * Demo 2: Custom AQS Implementation
     * Creating a simple binary semaphore using AQS
     */
    private static void demoCustomAQS() throws InterruptedException {
        System.out.println("2. Custom AQS Implementation Demo:");
        System.out.println("=================================");
        
        BinarySemaphore semaphore = new BinarySemaphore();
        CountDownLatch latch = new CountDownLatch(5);
        
        // Create threads that will compete for the semaphore
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    System.out.println("Thread " + threadId + " trying to acquire semaphore...");
                    semaphore.acquire();
                    System.out.println("Thread " + threadId + " acquired semaphore!");
                    
                    // Simulate work
                    TimeUnit.MILLISECONDS.sleep(500);
                    
                    System.out.println("Thread " + threadId + " releasing semaphore");
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        System.out.println("All semaphore operations completed.\n");
    }
    
    /**
     * Demo 3: AQS State Management
     * Demonstrating how AQS manages state
     */
    private static void demoAQSStateManagement() {
        System.out.println("3. AQS State Management Demo:");
        System.out.println("=============================");
        
        StatefulLock statefulLock = new StatefulLock();
        
        System.out.println("Initial state: " + statefulLock.getLockState());
        
        // Acquire lock (increases state)
        statefulLock.lock();
        System.out.println("After lock(): state = " + statefulLock.getLockState());
        
        // Reentrant acquisition (increases state further)
        statefulLock.lock();
        System.out.println("After second lock(): state = " + statefulLock.getLockState());
        
        // Release lock (decreases state)
        statefulLock.unlock();
        System.out.println("After unlock(): state = " + statefulLock.getLockState());
        
        // Release again
        statefulLock.unlock();
        System.out.println("After second unlock(): state = " + statefulLock.getLockState());
        
        System.out.println();
    }
    
    /**
     * Demo 4: AQS Queue Operations
     * Showing how threads are queued when lock is held
     */
    private static void demoAQSQueueOperations() throws InterruptedException {
        System.out.println("4. AQS Queue Operations Demo:");
        System.out.println("=============================");
        
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(3);
        
        // Hold the lock in main thread
        lock.lock();
        try {
            System.out.println("Main thread acquired lock - other threads will queue");
            
            // Start threads that will be queued
            for (int i = 0; i < 3; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try {
                        startLatch.await(); // Wait for signal to start
                        System.out.println("Thread " + threadId + " attempting to acquire lock...");
                        lock.lock();
                        try {
                            System.out.println("Thread " + threadId + " acquired lock!");
                        } finally {
                            lock.unlock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                }).start();
            }
            
            // Signal threads to start competing
            startLatch.countDown();
            
            // Hold lock for a while to show queuing
            TimeUnit.MILLISECONDS.sleep(2000);
            System.out.println("Main thread releasing lock - queued threads will wake up");
        } finally {
            lock.unlock();
        }
        
        endLatch.await();
        System.out.println("All queued threads completed.\n");
    }
    
    /**
     * Demo 5: Simple Read-Write Lock
     * Demonstrating the usage of the SimpleReadWriteLock class
     */
    private static void demoSimpleReadWriteLock() {
        System.out.println("5. Simple Read-Write Lock Demo:");
        System.out.println("=============================");
        
        SimpleReadWriteLock rwLock = new SimpleReadWriteLock();
        
        // Create multiple readers
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                rwLock.readLock().lock();
                try {
                    System.out.println(Thread.currentThread().getName() + " is reading");
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    rwLock.readLock().unlock();
                }
            }).start();
        }
        
        // Create a writer
        new Thread(() -> {
            rwLock.writeLock().lock();
            try {
                System.out.println(Thread.currentThread().getName() + " is writing");
                TimeUnit.MILLISECONDS.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rwLock.writeLock().unlock();
            }
        }).start();
        
        System.out.println("All read-write operations completed.\n");
    }
}

/**
 * Custom Binary Semaphore implementation using AQS
 * This demonstrates the basic AQS pattern
 */
class BinarySemaphore extends AbstractQueuedSynchronizer {
    
    public BinarySemaphore() {
        setState(1); // Available
    }
    
    @Override
    protected boolean tryAcquire(int arg) {
        // Try to change state from 1 to 0
        return compareAndSetState(1, 0);
    }
    
    @Override
    protected boolean tryRelease(int arg) {
        // Try to change state from 0 to 1
        return compareAndSetState(0, 1);
    }
    
    @Override
    protected boolean isHeldExclusively() {
        return getState() == 0; // Held when state is 0
    }
    
    public void acquire() throws InterruptedException {
        acquire(1);
    }
    
    public void release() {
        release(1);
    }
}

/**
 * Stateful Lock implementation showing AQS state management
 */
class StatefulLock extends AbstractQueuedSynchronizer {
    
    @Override
    protected boolean tryAcquire(int arg) {
        int state = getState();
        if (state == 0) {
            // No one holds the lock, try to acquire it
            if (compareAndSetState(0, arg)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
        } else if (getExclusiveOwnerThread() == Thread.currentThread()) {
            // Reentrant acquisition
            int nextState = state + arg;
            if (nextState < 0) // Overflow check
                throw new Error("Maximum lock count exceeded");
            setState(nextState);
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean tryRelease(int arg) {
        int state = getState() - arg;
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        
        boolean free = false;
        if (state == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(state);
        return free;
    }
    
    @Override
    protected boolean isHeldExclusively() {
        return getState() != 0 && getExclusiveOwnerThread() == Thread.currentThread();
    }
    
    public void lock() {
        acquire(1);
    }
    
    public void unlock() {
        release(1);
    }
    
    public int getLockState() {
        return getState();
    }
}

/**
 * Advanced AQS Example: Read-Write Lock Implementation
 * This shows a more complex AQS usage pattern
 */
class SimpleReadWriteLock {
    private final ReadLock readLock;
    private final WriteLock writeLock;
    
    public SimpleReadWriteLock() {
        this.readLock = new ReadLock();
        this.writeLock = new WriteLock();
    }
    
    public ReadLock readLock() { return readLock; }
    public WriteLock writeLock() { return writeLock; }
    
    class ReadLock extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int arg) {
            // Allow multiple readers if no writer
            int state = getState();
            return compareAndSetState(state, state + 1);
        }
        
        @Override
        protected boolean tryRelease(int arg) {
            int state = getState() - 1;
            if (state < 0) throw new IllegalMonitorStateException();
            setState(state);
            return state == 0;
        }
        
        public void lock() { acquire(1); }
        public void unlock() { release(1); }
    }
    
    class WriteLock extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int arg) {
            // Only allow if no readers or writers
            if (getState() == 0 && compareAndSetState(0, -1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
        
        @Override
        protected boolean tryRelease(int arg) {
            if (getExclusiveOwnerThread() != Thread.currentThread())
                throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
        
        public void lock() { acquire(1); }
        public void unlock() { release(1); }
    }
} 