package com.jintin.kfactory.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

/**
 * Write current [fileSpec] into File.
 */
fun CodeGenerator.writeFile(fileSpec: FileSpec) {
    createNewFile(
        dependencies = Dependencies(true),
        packageName = fileSpec.packageName,
        fileName = fileSpec.name
    ).use {
        it.writer().use(fileSpec::writeTo)
    }
}

/**
 * Transform [KSValueParameter] List to Map structure
 * where key is its annotation and value is the parameter name.
 */
fun List<KSValueParameter>.groupByAnnotation(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    this.forEach { p ->
        p.annotations.forEach { a ->
            result[a.toString()] = p.name?.asString().orEmpty()
        }
    }
    return result
}

/**
 * Transform the [KSDeclaration] into [ClassName]
 */
fun KSDeclaration.toClassName(): ClassName {
    return ClassName(packageName.asString(), asNameList())
}

/**
 * Transform the [KSDeclaration] into [List<String>] represent its hierarchy.
 */
fun KSDeclaration.asNameList(): List<String> {
    val list = mutableListOf<String>()
    var definition: KSDeclaration? = this
    while (definition != null) {
        list.add(0, definition.simpleName.asString())
        if (definition is KSTypeParameter) {
            break
        }
        definition = definition.parentDeclaration
    }
    return list
}

/**
 * Wrap [ClassName] with generic type if the [typeVariables] has value.
 */
fun ClassName.asParameterized(typeVariables: List<TypeName>?): TypeName {
    return if (typeVariables == null || typeVariables.isEmpty()) {
        this
    } else {
        parameterizedBy(typeVariables)
    }
}

/**
 * Transform [KSTypeArgument] into [ClassName]
 */
fun KSTypeArgument.toClassName(): TypeName {
    val declaration = type?.resolve()?.declaration
    val generics = type?.element?.typeArguments?.map(KSTypeArgument::toClassName)
    return ClassName(
        declaration?.packageName?.asString().orEmpty(),
        declaration?.simpleName?.asString().orEmpty()
    ).asParameterized(generics) //TODO more depth generic type
}

/**
 * Transform [List<KSValueParameter>] into [List<ParameterSpec>]
 */
fun List<KSValueParameter>.toParameterSpec(): List<ParameterSpec> {
    return this.map {

        val declaration = it.type.resolve().declaration
        val generics = it.type.element?.typeArguments?.map(KSTypeArgument::toClassName)

        ParameterSpec.builder(
            it.name?.getShortName().orEmpty(),
            declaration.toClassName().asParameterized(generics)
                .copy(it.type.resolve().isMarkedNullable)
        ).build()
    }
}