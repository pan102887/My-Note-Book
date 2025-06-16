# Java AbstractQueuedSynchronizer (AQS) Demo

## Overview

**AbstractQueuedSynchronizer (AQS)** is a core framework in Java's `java.util.concurrent.locks` package. It provides the foundation for building synchronizers such as locks, semaphores, and other thread coordination utilities. AQS manages a state variable and a FIFO wait queue, supporting both exclusive and shared modes for thread access.

AQS was designed by Doug Lea and serves as the backbone for most synchronization utilities in Java's concurrent package, including ReentrantLock, ReentrantReadWriteLock, CountDownLatch, and Semaphore.

## Key Concepts

- **State Management**: Uses an integer state to represent lock status, permits, or other synchronizer state.
- **FIFO Queue**: Threads that cannot acquire the synchronizer are queued in a FIFO structure.
- **CAS Operations**: Atomic state transitions are performed using compare-and-set (CAS) operations.
- **Template Methods**: Subclasses implement methods like `tryAcquire`, `tryRelease`, `tryAcquireShared`, and `tryReleaseShared` to define custom synchronization behavior.

## AQS Architecture

### Core Components

AQS consists of several key components:

1. **State Variable**: An `int` field that represents the synchronizer's state
2. **CLH Queue**: A variant of the Craig, Landin, and Hagersten lock queue for managing waiting threads
3. **Node Objects**: Each waiting thread is represented by a Node in the queue
4. **Exclusive Owner**: For exclusive locks, tracks which thread currently holds the lock

### State Representation

The state variable can represent different concepts depending on the synchronizer:

- **Locks**: 0 = unlocked, >0 = locked (count represents reentrant count)
- **Semaphores**: Number of available permits
- **CountDownLatch**: Number of remaining countdowns
- **ReadWriteLock**: Encoded state with bits for readers and writers

### Queue Structure

```text
Head -> [Node1] -> [Node2] -> [Node3] -> Tail
        Thread1    Thread2    Thread3
```

Each Node contains:

- Thread reference
- Node state (CANCELLED, SIGNAL, CONDITION, PROPAGATE)
- Links to predecessor and successor nodes

## Built-in AQS-Based Classes

### ReentrantLock

- **Exclusive Lock**: Only one thread can hold the lock at a time
- **Reentrant**: Same thread can acquire multiple times (state tracks count)
- **Fair/Unfair**: Supports both fair (FIFO) and unfair (opportunistic) ordering
- **Condition Support**: Provides condition variables for advanced synchronization

### ReentrantReadWriteLock

- **Read Lock**: Multiple readers can hold simultaneously (shared mode)
- **Write Lock**: Exclusive access for writers (exclusive mode)
- **State Encoding**: Uses state bits to track reader count and writer presence
- **Write Preference**: Writers have preference over readers to prevent starvation

### CountDownLatch

- **Shared Synchronizer**: Multiple threads can wait for the same event
- **One-time Use**: Cannot be reset after reaching zero
- **State**: Represents the number of remaining countdowns
- **Common Use Cases**: Thread coordination, startup synchronization

### Semaphore

- **Permit-based**: Controls access to a finite resource pool
- **Shared Access**: Multiple threads can acquire permits simultaneously
- **Fair/Unfair**: Supports both ordering modes
- **State**: Number of available permits

## AQS Internal Workings

### Acquire Process (Exclusive Mode)

1. **tryAcquire()**: Attempt immediate acquisition
2. **If Failed**: Create Node and enqueue in CLH queue
3. **Spin/Block**: Wait until predecessor becomes head
4. **Retry**: Call tryAcquire() again
5. **Success**: Set as head and clear predecessor links

### Release Process (Exclusive Mode)

1. **tryRelease()**: Attempt to release the lock
2. **If Successful**: Check if state becomes available
3. **Wake Successor**: Unpark the next waiting thread
4. **Update Head**: Successor becomes new head

### Node States

- **CANCELLED (1)**: Node cancelled due to timeout or interrupt
- **SIGNAL (-1)**: Successor needs to be unparked
- **CONDITION (-2)**: Node is on condition queue
- **PROPAGATE (-3)**: Release should be propagated to other nodes
- **0**: Initial state

## Template Method Pattern

AQS uses the Template Method pattern where you override specific methods:

```java
// For exclusive locks (like ReentrantLock)
protected boolean tryAcquire(int arg)
protected boolean tryRelease(int arg)

// For shared locks (like Semaphore, CountDownLatch)
protected int tryAcquireShared(int arg)
protected boolean tryReleaseShared(int arg)

// For condition variables
protected boolean isHeldExclusively()
```

## Performance Characteristics

### Advantages

- **Efficient**: Minimal overhead for uncontended locks
- **Scalable**: Good performance under high contention
- **Flexible**: Supports both exclusive and shared modes
- **Fairness**: Optional fair ordering support

### Disadvantages

- **Complexity**: Internal implementation is complex
- **Memory**: Each waiting thread consumes memory for Node objects
- **Cache Effects**: Queue operations can cause cache misses

## Demo Project Structure

This project demonstrates AQS concepts with practical Java code and scripts for easy compilation and execution.

### Directory Layout

```text
AQS/
├── build/                # Compiled .class files (auto-generated)
├── AQS_Demo.java         # Main Java demo source file
├── AQS_Explanation.md    # In-depth theoretical guide to AQS
├── compile.bat           # Batch script to compile Java files
├── compile.ps1           # PowerShell script to compile Java files
├── run.bat               # Batch script to run the demo (auto-compiles if needed)
├── run.ps1               # PowerShell script to run the demo (auto-compiles if needed)
└── README.md             # This documentation file
```

## Demo Features

- **ReentrantLock Demo**: Shows how threads compete for a lock (AQS-based).
- **Custom BinarySemaphore**: Demonstrates a simple synchronizer built with AQS.
- **StatefulLock**: Illustrates state management and reentrancy.
- **Queue Operations**: Visualizes thread queuing and wake-up behavior.

## How to Use

### 1. Compile the Project

You can use either the batch or PowerShell script:

**Command Prompt:**

```cmd
cd AQS
compile.bat
```

**PowerShell:**

```powershell
cd AQS
./compile.ps1
```

### 2. Run the Demo

**Command Prompt:**

```cmd
run.bat
```

**PowerShell:**

```powershell
./run.ps1
```

- The run scripts will auto-compile if needed.
- To clean up all compiled files, use:
  - `run.bat clean` or `./run.ps1 clean`

### 3. Output

The demo will print:

- Lock acquisition and release by multiple threads
- Custom synchronizer behavior
- State transitions
- Thread queuing and wake-up order

## Best Practices

### When to Use AQS

- Building custom synchronization utilities
- Implementing domain-specific locks
- Creating resource pools with access control
- Coordinating multiple threads in complex scenarios

### Common Patterns

- **Binary Semaphore**: Simple on/off synchronization
- **Reentrant Lock**: Exclusive access with reentrancy
- **Read-Write Lock**: Optimized for read-heavy workloads
- **Barrier**: Thread coordination for phased execution

### Error Handling

- Always validate arguments in tryAcquire/tryRelease methods
- Handle interruption correctly
- Consider timeout scenarios
- Validate state transitions

## Further Reading

- See `AQS_Explanation.md` for a deep dive into AQS internals, best practices, and advanced usage.
- Explore the source code in `AQS_Demo.java` for practical implementation patterns.
- Official Java documentation: [AbstractQueuedSynchronizer (Java SE API)](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/locks/AbstractQueuedSynchronizer.html)
- Doug Lea's paper: "The java.util.concurrent Synchronizer Framework"

## License

This demo is provided for educational purposes.
