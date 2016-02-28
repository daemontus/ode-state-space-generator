package cz.muni.fi.ode.generator

import com.github.daemontus.jafra.Token
import cz.muni.fi.checker.Communicator
import cz.muni.fi.checker.GuardedThread
import cz.muni.fi.checker.IDNode
import cz.muni.fi.checker.Job
import mpi.Comm
import mpi.MPI
import java.util.*

//TODO: How do we test this class? Maybe a mock MPJ Comm?
class MPJCommunicator(
        override val id: Int,
        override val size: Int,
        parameterCount: Int,
        private val comm: Comm
) : Communicator {

    private val COMMAND_TAG = 0;
    private val DATA_TAG = 1;

    private val TOKEN = 1;
    private val JOB = 2;
    private val TERMINATE = 3;

    //Command message structure:
    //Token: TOKEN | SENDER | FLAG | COUNT | UNDEFINED
    //Job: JOB | SENDER | SOURCE_ID | TARGET_ID | RECTANGLE_COUNT
    //Terminate: TERMINATE | SENDER | UNDEFINED | UNDEFINED | UNDEFINED
    private val inCommandBuffer = IntArray(5)
    private var inDataBuffer = DoubleArray(100)
    private val outCommandBuffer = IntArray(5)
    private var outDataBuffer = DoubleArray(100)

    private val rectangleSize = 2 * parameterCount

    private val listeners = HashMap<Class<*>, (Any) -> Unit>()

    private val mpiListener = GuardedThread() {
        fun getListener(c: Class<*>): (Any) -> Unit {
            return synchronized(listeners) {
                listeners[c]
            } ?: throw IllegalStateException("Message with no listener received! $id ${Arrays.toString(inCommandBuffer)} - listeners: $listeners")
        }
        comm.Recv(inCommandBuffer, 0, inCommandBuffer.size, MPI.INT, MPI.ANY_SOURCE, COMMAND_TAG)
        while (inCommandBuffer[0] != TERMINATE) {
            if (inCommandBuffer[0] == TOKEN) {
                val listener = getListener(Token::class.java)
                listener(inCommandBuffer.toToken())
            } else if (inCommandBuffer[0] == JOB) {
                val listener = getListener(Job::class.java)
                listener(inCommandBuffer.toJob())
            }
            comm.Recv(inCommandBuffer, 0, inCommandBuffer.size, MPI.INT, MPI.ANY_SOURCE, 0)
        }
    }

    private fun receiveColors(sender: Int, rectangleCount: Int): RectangleColors {
        while (rectangleCount * rectangleSize > inDataBuffer.size) { //increase buffer size if needed
            inDataBuffer = DoubleArray(inDataBuffer.size * 2)
        }
        //we need sender to ensure we don't mix data and commands from different senders
        comm.Recv(inDataBuffer, 0, rectangleCount * rectangleSize, MPI.DOUBLE, sender, DATA_TAG)
        val rectangles = HashSet<Rectangle>()
        for (rectangle in 0 until rectangleCount) {
            val startIndex = rectangleSize * rectangle
            val endIndex = rectangleSize * (rectangle+1)
            rectangles.add(Rectangle(inDataBuffer.copyOfRange(startIndex, endIndex)))
        }
        return RectangleColors(rectangles)
    }

    override fun <M : Any> addListener(messageClass: Class<M>, onTask: (M) -> Unit) {
        if (messageClass != Token::class.java && messageClass != Job::class.java) {
            throw IllegalArgumentException("This communicator can't send classes of type: $messageClass")
        }
        synchronized(listeners) {
            @Suppress("UNCHECKED_CAST") //Cast is ok, we have to get rid of the type in the map.
            val previous = listeners.put(messageClass, onTask as (Any) -> Unit)
            if (previous != null) throw IllegalStateException("Replacing already present listener: $id, $messageClass")
        }
    }

    override fun removeListener(messageClass: Class<*>) {
        synchronized(listeners) {
            listeners.remove(messageClass) ?: throw IllegalStateException("Removing non existent listener: $id, $messageClass")
        }
    }

    override fun send(dest: Int, message: Any) {
        if (dest == id) throw IllegalArgumentException("Can't send message to yourself")
        if (message.javaClass == Token::class.java) {
            val token = message as Token
            token.serialize(outCommandBuffer)
            comm.Send(outCommandBuffer, 0, outCommandBuffer.size, MPI.INT, dest, COMMAND_TAG)
        } else if (message.javaClass == Job::class.java) {
            //send command info
            @Suppress("UNCHECKED_CAST") //It's ok, this won't be used outside this module!
            val job = message as Job<IDNode, RectangleColors>
            job.serialize(outCommandBuffer)
            comm.Send(outCommandBuffer, 0, outCommandBuffer.size, MPI.INT, dest, COMMAND_TAG)

            //ensure data buffer capacity
            val rectangleCount = job.colors.rectangleCount()
            while (rectangleCount * rectangleSize > outDataBuffer.size) {
                outDataBuffer = DoubleArray(outDataBuffer.size * 2)
            }

            //send color data
            job.colors.serialize(outDataBuffer)
            comm.Send(outDataBuffer, 0, rectangleCount * rectangleSize, MPI.DOUBLE, dest, DATA_TAG)
        } else {
            throw IllegalArgumentException("Cannot send message: $message to $dest")
        }
    }

    override fun close() {
        synchronized(listeners) {
            if (listeners.isNotEmpty())
                throw IllegalStateException("Someone is still listening! $listeners")
        }
        outCommandBuffer[0] = TERMINATE
        outCommandBuffer[1] = comm.Rank()
        //there is a bug in MPJ where you can't send messages to yourself in MPI configuration (you have to use hybrid)
        //so in order to circumvent this, each node terminates it's successor. - this is valid because
        //termination detection has to be ensured before calling close anyway.
        comm.Send(outCommandBuffer, 0, 2, MPI.INT, (comm.Rank() + 1) % comm.Size(), COMMAND_TAG)
        mpiListener.join()
    }


    /* Data conversion functions */

    private fun IntArray.toToken(): Token = Token(this[2], this[3])

    private fun IntArray.toJob(): Job<IDNode, RectangleColors> = Job(
            source = IDNode(this[2]), target = IDNode(this[3]), colors = receiveColors(this[1], this[4])
    )

    private fun Token.serialize(to: IntArray) {
        to[0] = TOKEN; to[1] = comm.Rank(); to[2] = this.flag; to[3] = this.count
    }

    private fun Job<IDNode, RectangleColors>.serialize(to: IntArray) {
        to[0] = JOB; to[1] = comm.Rank(); to[2] = this.source.id; to[3] = this.target.id; to[4] = this.colors.rectangleCount()
    }

}