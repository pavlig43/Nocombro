package ru.pavlig43.core

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

inline fun <reified T : Any> KClass<T>.entries(): List<T> {
    require(this.isSealed) { "${this.simpleName} must be a sealed class" }
    return this.sealedSubclasses.map { it.createInstance()}
}


