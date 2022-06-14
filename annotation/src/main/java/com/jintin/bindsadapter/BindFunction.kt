package com.jintin.bindsadapter

/**
 * Indicate current function will be called by corresponding Adapter
 * when its onBindViewHolder is called.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindFunction