package com.jintin.bindsadapter.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Entry point of the whole BindsAdapter KSP processing
 */
class BuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return BuilderProcessor(
            environment.options["package"].orEmpty(),
            environment.codeGenerator,
            environment.logger
        )
    }
}