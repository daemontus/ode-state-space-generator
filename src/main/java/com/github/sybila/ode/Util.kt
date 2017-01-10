package com.github.sybila.ode

import java.util.*


fun Double.safeString(): String {
    return String.format(Locale.ROOT, "%f", this)
}