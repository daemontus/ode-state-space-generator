package com.github.sybila.ode.generator

import mpi.Comm
import mpi.Datatype
import mpi.MPI


internal val COMMAND_TAG = 0;
internal val DATA_TAG = 1;

internal val TOKEN = 1;
internal val JOB = 2;
internal val TERMINATE = 3;

/**
 * We don't want to initialise MPJ during tests, so we can't touch any code in MPJ package.
 */
enum class Type {
    INT, DOUBLE;

    fun getMPJType(): Datatype {
        return when (this) {
            INT -> MPI.INT
            DOUBLE -> MPI.DOUBLE
        }
    }
}

val ANY_SOURCE = -2

/**
 * We need this primarily for testing.
 */
interface AbstractComm {

    /**
     * Blocking receive message
     */
    fun receive(buffer: Any, offset: Int, size: Int, dataType: Type, source: Int, tag: Int)

    /**
     * Non blocking send
     */
    fun send(buffer: Any, offset: Int, size: Int, dataType: Type, destination: Int, tag: Int)

}

class MPJComm(val comm: Comm) : AbstractComm {

    override fun receive(buffer: Any, offset: Int, size: Int, dataType: Type, source: Int, tag: Int) {
        comm.Recv(
                buffer,
                offset,
                size,
                dataType.getMPJType(),
                if (source == ANY_SOURCE) MPI.ANY_SOURCE else source,
                tag)
    }

    override fun send(buffer: Any, offset: Int, size: Int, dataType: Type, destination: Int, tag: Int) {
        comm.Send(buffer, offset, size, dataType.getMPJType(), destination, tag)
    }

}