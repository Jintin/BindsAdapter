package com.jintin.bindsadapter

import kotlin.reflect.KClass

/**
 * Used to label current Adapter will binds with [holders] automatically
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class BindAdapter(val holders: Array<KClass<*>>)