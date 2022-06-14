package com.jintin.bindsadapter.processor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.jintin.bindsadapter.BindAdapter
import com.jintin.bindsadapter.BindFunction
import com.jintin.kfactory.processor.groupByAnnotation
import com.jintin.kfactory.processor.toClassName
import com.jintin.kfactory.processor.toParameterSpec
import com.jintin.kfactory.processor.writeFile
import com.squareup.kotlinpoet.*

/**
 * The processor which responsible for create the auto-generated Adapter.
 */
class BuilderProcessor(
    private val packageName: String,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    companion object {
        private val VIEW_GROUP = ClassName("android.view", "ViewGroup")
        private val LAYOUT_INFLATER = ClassName("android.view", "LayoutInflater")
        private val VIEW_HOLDER =
            ClassName("androidx.recyclerview.widget.RecyclerView", "ViewHolder")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        getAdapters(resolver)
            .forEach {
                codeGenerator.writeFile(genFile(it.key, it.value))
            }
        return emptyList()
    }

    /**
     * Generate the ViewType definition TypeSpec for Adapter.
     */
    private fun genConstSpec(typeList: List<String>) =
        TypeSpec.companionObjectBuilder()
            .addProperties(
                typeList.mapIndexed { index, s ->
                    PropertySpec.builder(s, Int::class)
                        .addModifiers(KModifier.CONST)
                        .initializer(index.toString())
                        .build()
                }
            ).build()

    /**
     * Generate the onCreateViewHolder definition FuncSpec for Adapter.
     */
    private fun genCreateViewHolderSpec(
        typeList: List<String>,
        variableMap: Map<String, String>,
        holders: List<KSClassDeclaration>
    ) = FunSpec.builder("onCreateViewHolder")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(ParameterSpec.builder("parent", VIEW_GROUP).build())
        .addParameter(ParameterSpec.builder("viewType", Int::class).build())
        .addStatement("val inflater = %T.from(parent.context)", LAYOUT_INFLATER)
        .beginControlFlow("return when (viewType)")
        .apply {
            typeList.forEachIndexed { index, type ->
                val holder = holders[index]
                beginControlFlow("$type ->")
                val parameters = holder.primaryConstructor?.parameters.orEmpty()
                parameters.forEach {
                    if (it.type.resolve().isError) {
                        addStatement(
                            "val binding = %T.inflate(inflater, parent, false)",
                            ClassName("$packageName.databinding", it.type.toString())
                        )
                    }
                }
                addStatement(
                    "%T(${
                        getHolderParameter(
                            holder.simpleName.asString(),
                            parameters,
                            variableMap
                        )
                    })",
                    holder.toClassName()
                )
                endControlFlow()
            }
            addStatement("""else -> throw RuntimeException("Not support type!")""")
        }
        .endControlFlow()
        .returns(VIEW_HOLDER)
        .build()

    /**
     * Generate the onBindViewHolder definition FuncSpec for Adapter.
     */
    private fun genOnBindViewHolderSpec(
        typeList: List<String>,
        variableMap: Map<String, String>,
        holders: List<KSClassDeclaration>
    ) = FunSpec.builder("onBindViewHolder")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(ParameterSpec.builder("holder", VIEW_HOLDER).build())
        .addParameter(ParameterSpec.builder("position", Int::class).build())
        .beginControlFlow("when (getItemViewType(position))")
        .apply {
            typeList.forEachIndexed { index, type ->
                beginControlFlow("$type ->")
                val holder = holders[index]
                val holderName = holder.simpleName.asString()
                val function = holder.getDeclaredFunctions().firstOrNull { f ->
                    f.annotations.any { it.shortName.asString() == BindFunction::class.simpleName }
                }
                if (function == null) {
                    logger.error("$holder don't have an OnBind annotation for Adapter to bind with")
                } else {
                    val functionName = function.simpleName.asString()
                    addStatement(
                        "(holder as $holderName).$functionName(${
                            getBindParameter(
                                "$holderName.$functionName",
                                function.parameters, variableMap
                            )
                        })"
                    )
                }
                endControlFlow()
            }
        }
        .endControlFlow()
        .build()

    /**
     * Generate the whole FileSpec represent the Adapter.
     */
    private fun genFile(target: KSClassDeclaration, holders: List<KSClassDeclaration>): FileSpec {
        val name = target.simpleName.asString()
        val implName = name + "Impl"
        val parameters = target.primaryConstructor?.parameters.orEmpty()
        val variableMap = parameters.groupByAnnotation()
        val typeList = getTypeList(holders)

        return FileSpec.builder(target.packageName.asString(), implName)
            .addType(
                TypeSpec.classBuilder(implName)
                    .superclass(target.toClassName())
                    .addSuperclassConstructorParameter(parameters.joinToString {
                        it.name?.asString().orEmpty()
                    })
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameters(parameters.toParameterSpec())
                            .build()
                    )
                    .addType(genConstSpec(typeList))
                    .addFunction(genCreateViewHolderSpec(typeList, variableMap, holders))
                    .addFunction(genOnBindViewHolderSpec(typeList, variableMap, holders))
                    .build()
            ).build()
    }

    /**
     * Compose the whole parameter String from given [KSValueParameter] list will used in onBindViewHolder.
     */
    private fun getBindParameter(
        name: String,
        list: List<KSValueParameter>,
        map: Map<String, String>
    ): String {
        var noAnnotationCount = 0
        return list.joinToString {
            val result = it.annotations
                .map(KSAnnotation::toString)
                .firstOrNull(map::containsKey)
            if (result != null) {
                map[result.toString()].orEmpty()
            } else {
                if (noAnnotationCount > 0) {
                    logger.error("$name don't have enough annotation to bind Adapter")
                }
                noAnnotationCount++
                "getItem(position)"
            }
        }
    }

    /**
     * Compose the whole parameter String from given [KSValueParameter] list will used in onCreateViewHolder.
     */
    private fun getHolderParameter(
        name: String,
        list: List<KSValueParameter>,
        map: Map<String, String>
    ): String {
        var noAnnotationCount = 0
        return list.joinToString {
            val result = it.annotations
                .map(KSAnnotation::toString)
                .firstOrNull(map::containsKey)
            if (result != null) {
                map[result.toString()].orEmpty()
            } else {
                if (noAnnotationCount > 0) {
                    logger.error("$name didn't add enough annotations for onCreateViewHolder to bind with")
                }
                noAnnotationCount++
                "binding"
            }
        }
    }

    /**
     * Generate the ViewType list.
     */
    private fun getTypeList(holders: List<KSDeclaration>): List<String> {
        return holders.map {
            it.simpleName.asString().map { c ->
                if (c in 'A'..'Z') {
                    "_$c"
                } else {
                    c.uppercase()
                }
            }.joinToString(separator = "", prefix = "TYPE")
        }
    }

    /**
     * Extract information from [resolver] to know which Adapter has the [BindAdapter] annotation
     * and get all its ViewHolders.
     */
    private fun getAdapters(resolver: Resolver): Map<KSClassDeclaration, List<KSClassDeclaration>> {
        val result = mutableMapOf<KSClassDeclaration, List<KSClassDeclaration>>()
        resolver.getSymbolsWithAnnotation(BindAdapter::class.qualifiedName.orEmpty())
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .forEach { d ->
                val list = d.annotations.first { ann ->
                    ann.shortName.getShortName() == BindAdapter::class.simpleName.orEmpty()
                }.arguments[0].value as List<*>

                result[d] =
                    list.filterIsInstance<KSType>().map { it.declaration as KSClassDeclaration }
            }
        return result
    }
}