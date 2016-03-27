package com.github.sybila.ode

import com.github.daemontus.jafra.Terminator
import com.github.sybila.checker.*
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import java.util.logging.Logger

class BlockingHashMap<K: Node, V: Colors<V>>() {

    private val mapLock = ReentrantLock()
    private val condition = mapLock.newCondition()
    private val map = HashMap<K, Job<K, V>>()

    private var terminate = false

    fun add(value: Job<K, V>) {
        mapLock.lock()
        val v = map[value.target]
        if (v == null) {
            map[value.target] = value
        } else {
            //TODO: target and source don't matter because we are going to use this for existential queries
            map[value.target] = v.copy(colors = v.colors + value.colors)
        }
        condition.signal()
        mapLock.unlock()
    }

    fun poison() {
        terminate = true
        mapLock.lock()
        condition.signal()
        mapLock.unlock()
    }

    fun threadUntilPoisoned(action: (Job<K, V>) -> Unit): GuardedThread {
        fun getJob(): Job<K, V>? {
            mapLock.lock()
            while (map.isEmpty()) {
                condition.await()
                if (terminate) return null
            }
            var job = map.entries.first()
            map.remove(job.key)
            mapLock.unlock()
            return job.value
        }
        val result = GuardedThread {
            var job = getJob()
            while (job != null) {
                action(job)
                job = getJob()
            }   //got Nothing
        }
        result.thread.start()
        return result
    }

    fun size(): Int {
        mapLock.lock()
        val size = map.size
        mapLock.unlock()
        return size
    }

    fun isEmpty(): Boolean {
        mapLock.lock()
        val empty = map.isEmpty()
        mapLock.unlock()
        return empty
    }

}

class MergeQueue<N: Node, C: Colors<C>>(
        initial: List<Job<N, C>>,
        private val comm: Communicator,
        private val terminators: Terminator.Factory,
        partitioning: PartitionFunction<N>,
        private val onTask: JobQueue<N, C>.(Job<N, C>) -> Unit,
        private val logger: Logger = Logger.getLogger(SingleThreadJobQueue::class.java.canonicalName).apply {
            this.level = Level.OFF
        }
) :
        JobQueue<N, C>,
        PartitionFunction<N> by partitioning
{
    //Time spent processing jobs (including state space generation)
    private var timeInJobs = 0L
    //Number of processed jobs
    private var jobsProcessed = 0L
    //Number of jobs posted to this queue
    private var jobsPosted = 0L
    //Number of jobs sent by this queue
    private var jobsSent = 0L
    //Number of jobs received from communicator
    private var jobsReceived = 0L

    private var active = false

    private var lastProgressUpdate = 0L

    private val queueLock = ReentrantLock()
    private val localQueue = BlockingHashMap<N, C>()

    private val workRound = terminators.createNew() //run before constructor - it can be called from comm listener and that would deadlock

    init {
        //init communication session
        val initRound = terminators.createNew()
        comm.addListener(genericClass<Job<N, C>>()) {
            synchronized(localQueue) {
                //must be set first, otherwise we might process
                //message before terminator is marked as working
                jobsReceived += 1
                workRound.messageReceived()
                localQueue.add(it)
            }
        }
        initRound.setDone()
        initRound.waitForTermination()
        active = true
    }

    init {
        //add initial jobs, if any
        logger.lFine { "Init queue with ${initial.size} jobs."}
        initial.map { post(it) }
        doneIfEmpty()   //at this point, worker is not running, so if something went into our queue, it's still there!
    }

    //Last thing - start the worker
    //this can't be done sooner because we might be interleaving with job insertion
    private val worker = localQueue.threadUntilPoisoned { job ->
        jobsProcessed += 1
        val start = System.nanoTime()
        onTask(job)
        timeInJobs += System.nanoTime() - start
        if (start - lastProgressUpdate > 2 * 1000 * 1000 * 1000L) {
            //print progress every two seconds
            logger.lInfo { "Remaining: ${localQueue.size()}, $lastProgressUpdate, $start" }
            lastProgressUpdate = start
        }
        doneIfEmpty()
    }

    override fun post(job: Job<N, C>) {
        jobsPosted += 1
        when {
            !active -> throw IllegalStateException("Posting on inactive JobQueue! $myId")
            job.target.ownerId() == myId -> {
                synchronized(localQueue) {
                    localQueue.add(job)
                }
            }
            else -> {
                jobsSent += 1
                workRound.messageSent()
                logger.lFinest { "Send job to ${job.target.ownerId()}" }
                comm.send(job.target.ownerId(), job)
            }
        }
    }

    override fun waitForTermination() {
        logger.lFine { "Waiting for termination" }
        workRound.waitForTermination()
        active = false
        val finalRound = terminators.createNew()
        comm.removeListener(genericClass<Job<N, C>>())
        localQueue.poison()
        worker.join()
        finalRound.setDone()
        finalRound.waitForTermination()
    }

    private fun doneIfEmpty() {
        synchronized(localQueue) {
            if (localQueue.isEmpty()) workRound.setDone()
        }
    }

    override fun getStats(): Map<String, Any> {
        return mapOf(
                "Jobs posted" to jobsPosted,
                "Jobs received" to jobsReceived,
                "Jobs sent" to jobsSent,
                "Jobs processed" to jobsProcessed,
                "Time in jobs" to timeInJobs
        )
    }

    override fun resetStats() {
        jobsPosted = 0L
        jobsReceived = 0L
        jobsSent = 0L
        jobsProcessed = 0L
        timeInJobs = 0L
    }

}

fun <N: Node, C: Colors<C>> createMergeQueues(
        processCount: Int,
        partitioning: List<PartitionFunction<N>> = (1..processCount).map { UniformPartitionFunction<N>(it - 1) },
        communicators: List<Communicator>,
        terminators: List<Terminator.Factory>,
        logger: Logger
): List<JobQueue.Factory<N, C>> {
    return (0..(processCount-1)).map { i ->
        object : JobQueue.Factory<N, C> {
            override fun createNew(initial: List<Job<N, C>>, onTask: JobQueue<N, C>.(Job<N, C>) -> Unit): JobQueue<N, C> {
                return MergeQueue(initial, communicators[i], terminators[i], partitioning[i], onTask, logger)
            }
        }
    }
}