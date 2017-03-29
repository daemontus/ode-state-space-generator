package com.github.sybila.ode.generator.rect

import java.util.concurrent.atomic.AtomicLong

object PerfCounters {
    val elapsed = AtomicLong(0)
    val events = AtomicLong(0)

    fun log(time: Long) {
        elapsed.addAndGet(time)
        events.incrementAndGet()
    }
}