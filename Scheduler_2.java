/*
 * @file    Scheduler_2.java
 *           Modified from Scheduler.java from ThreadOS
 * @brief   This class is a thread scheduler for ThreadOS that uses multilevel
 *           feedback queues. New threads are added to the highest-priority
 *           queue and given the smallest time slice for execution. Any thread
 *           that does not finish within the allotted time is sent to the next
 *           queue and given a larger time slice, but that queue will not have
 *           any of its threads executed until the first queue is empty. Any
 *           thread in the second queue that does not finish within its time
 *           slice is demoted to the lowest queue and given a larger time slice
 *           (double the default). Threads in the lowest queue that do not
 *           finish within the given time slice are moved to the back of the
 *           queue in round-robin fashion. New threads in higher queues can
 *           preempt running threads in lower queues in itervals of the
 *           smallest used time slice (half the default). Threads that are
 *           preempted stay at the front of their respective queue and maintain
 *           a record of how much time they have left.
 * @author  Brendan Sweeney, SID 1161836
 * @date    October 26, 2012
 *
 */


import java.util.*;

public class Scheduler_2 extends Thread
{
    private Vector[] Q;     //queue;  lower index == higher priority
    private int[] slices;   // remaining time slices for first thread in queues
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;
    private static final int DEFAULT_QUEUE_COUNT = 3;   // queue array size

    // New data added to p161 
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to p161 
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;
    private void initTid( int maxThreads ) {
	tids = new boolean[maxThreads];
	for ( int i = 0; i < maxThreads; i++ )
	    tids[i] = false;
    }

    // A new feature added to p161 
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid( ) {
	for ( int i = 0; i < tids.length; i++ ) {
	    int tentative = ( nextId + i ) % tids.length;
	    if ( tids[tentative] == false ) {
		tids[tentative] = true;
		nextId = ( tentative + 1 ) % tids.length;
		return tentative;
	    }
	}
	return -1;
    }

    // A new feature added to p161 
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid( int tid ) {
	if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
	    tids[tid] = false;
	    return true;
	}
	return false;
    }

    // A new feature added to p161 
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb( ) {
	Thread myThread = Thread.currentThread( ); // Get my thread object
	synchronized( Q ) {
            for (int i = 0; i < DEFAULT_QUEUE_COUNT; ++i) {
                for ( int j = 0; j < Q[i].size( ); j++ ) {
                    TCB tcb = ( TCB )Q[i].elementAt( j );
                    Thread thread = tcb.getThread( );
                    if ( thread == myThread ) // if this is my TCB, return it
                        return tcb;
                }
            } // end for (; i < DEFAULT_QUEUE_COUNT; )
	}
	return null;
    }

    // A new feature added to p161 
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads( ) {
	return tids.length;
    }

    public Scheduler_2( ) {
        // treating time slices as discrete multiples of the smallest allowable
        // unit simplifies checking timeouts
	timeSlice = DEFAULT_TIME_SLICE / 2;
        // array of queues allows for simple iteration
        Q = new Vector[DEFAULT_QUEUE_COUNT];
        slices = new int[DEFAULT_QUEUE_COUNT];
        for (int i = 0; i < DEFAULT_QUEUE_COUNT; ++i) {
            Q[i] = new Vector( );
            slices[i] = (int)java.lang.Math.pow(2, i) - 1;
        } // end for (; i < DEFAULT_QUEUE_COUNT; )
	initTid( DEFAULT_MAX_THREADS );
    }

    public Scheduler_2( int quantum ) {
	timeSlice = quantum;
        Q = new Vector[DEFAULT_QUEUE_COUNT];
        slices = new int[DEFAULT_QUEUE_COUNT];
        for (int i = 0; i < DEFAULT_QUEUE_COUNT; ++i) {
            Q[i] = new Vector( );
            slices[i] = (int)java.lang.Math.pow(2, i) - 1;
        } // end for (; i < DEFAULT_QUEUE_COUNT; )
	initTid( DEFAULT_MAX_THREADS );
    }

    // A new feature added to p161 
    // A constructor to receive the max number of threads to be spawned
    public Scheduler_2( int quantum, int maxThreads ) {
	timeSlice = quantum;
        Q = new Vector[DEFAULT_QUEUE_COUNT];
        slices = new int[DEFAULT_QUEUE_COUNT];
        for (int i = 0; i < DEFAULT_QUEUE_COUNT; ++i) {
            Q[i] = new Vector( );
            slices[i] = (int)java.lang.Math.pow(2, i) - 1;
        } // end for (; i < DEFAULT_QUEUE_COUNT; )
	initTid( maxThreads );
    }

    private void schedulerSleep( ) {
	try {
	    Thread.sleep( timeSlice );
	} catch ( InterruptedException e ) {
	}
    }

    // A modified addThread of p161 example
    public TCB addThread( Thread t ) {
//	t.setPriority( 2);
	TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
	int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
	int tid = getNewTid( ); // get a new TID
	if ( tid == -1)
	    return null;
	TCB tcb = new TCB( t, tid, pid ); // create a new TCB
	Q[0].add( tcb );
	return tcb;
    }

    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread( ) {
	TCB tcb = getMyTcb( ); 
	if ( tcb!= null )
	    return tcb.setTerminated( );
	else
	    return false;
    }

    public void sleepThread( int milliseconds ) {
	try {
	    sleep( milliseconds );
	} catch ( InterruptedException e ) { }
    }
    
    // A modified run of p161
    public void run( ) {
	Thread current = null;

//	this.setPriority( 6 );
	
	while ( true ) {
	    try {
                int queueNum = -1;  // keep track of active queue
		// get the next TCB and its thread
                for (int i = 0; i < DEFAULT_QUEUE_COUNT; ++i) {
                    if (Q[i].size() > 0) {
                        queueNum = i;
                        i = DEFAULT_QUEUE_COUNT;
                    } // end if (Q[i].size() > 0)
                } // end for (; i < DEFAULT_QUEUE_COUNT; )
		if (queueNum == -1)
                    continue;
                TCB currentTCB = (TCB)Q[queueNum].firstElement( );
                if ( currentTCB.getTerminated( ) == true ) {
                    Q[queueNum].remove( currentTCB );
                    returnTid( currentTCB.getTid( ) );
                    continue;
                }
                current = currentTCB.getThread( );
		if ( current != null ) {
		    if ( current.isAlive( ) )
			current.resume();//setPriority( 4 );
		    else {
			// Spawn must be controlled by Scheduler
			// Scheduler must start a new thread
			current.start( ); 
//			current.setPriority( 4 );
		    }
		}
		
		schedulerSleep( );
		// System.out.println("* * * Context Switch * * * ");

		synchronized ( Q ) {
		    if ( current != null && current.isAlive( ) )
			current.suspend();//setPriority( 2 );
                    // If this thread has no time left, move it to the back of
                    // the appropriate queue
                    if (slices[queueNum] < 1) {
                        // special case: last queue enqueues to itself
                        if (queueNum == DEFAULT_QUEUE_COUNT - 1) {
                            Q[queueNum].remove( currentTCB );
                            Q[queueNum].add( currentTCB );
                        } // end if (queueNum == DEFAULT_QUEUE_COUNT - 1)
                        else {
                            Q[queueNum].remove( currentTCB );
                            Q[queueNum + 1].add( currentTCB );
                        } // end else queueNum == DEFAULT_QUEUE_COUNT - 1
                        slices[queueNum] =
                                (int)java.lang.Math.pow(2, queueNum) - 1;
                    } // end if (slices[queueNum] == 0)
                    else {
                        slices[queueNum] -= 1;
                    } // end else slices[queueNum] == 0
		}
	    } catch ( NullPointerException e3 ) { };
	}
    }
}
