# BindsAdapter

[![CircleCI](https://circleci.com/gh/Jintin/BindsAdapter.svg?style=shield)](https://app.circleci.com/pipelines/github/Jintin/BindsAdapter)
[![Jitpack](https://jitpack.io/v/Jintin/BindsAdapter.svg)](https://jitpack.io/#Jintin/BindsAdapter)

BindsAdapter is an Android library to help you create and maintain `Adapter` class easier via ksp(
Kotlin Symbol Processing).

## Installation

First, add jitpack as one of the repositories in your project.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

And then apply the ksp plugin in your module where you need the factory be generated.

```groovy
plugins {
    id 'com.google.devtools.ksp' version '1.7.0-1.0.6'
}
```

And then declare the dependency. Do noted that for the processor dependency, it requires `ksp`
not `kapt`

```groovy
implementation 'com.github.Jintin.BindsAdapter:annotation:{latest-version}'
ksp 'com.github.Jintin.BindsAdapter:processor:{latest-version}'
```

Lastly, the generated file will be located inside `build/generated/ksp/`, but your IDE might not
able to identify it. In such case you can add it manually like below:

```groovy
sourceSets {
    main {
        java {
            srcDir "${buildDir.absolutePath}/generated/ksp/"
        }
    }
}
```

## Usage

### Adapter

First, create a abstract `ListAdapter` with annotation `@BindAdapter` and link all the
associate `ViewHolder` into it.

```kotlin
@BindAdapter([MyViewHolder1::class, MyViewHolder2::class])
abstract class MyAdapter(
    diffCallback: DiffUtil.ItemCallback<String>
) : ListAdapter<String, RecyclerView.ViewHolder>(diffCallback)
```

> Note: `ListAdapter` is a must now, but it doesn't mean we can't support general `Adapter` in the future.

### ViewHolder

And then, create all your `ViewHolder` and label `@BindFunction` to the function you want to be
called when `onBindViewHolder` is called from `Adapter`.

```kotlin
class MyViewHolder1(
    val binding: AdapterHolder1Binding,
) : RecyclerView.ViewHolder(binding.root) {

    @BindFunction
    fun bindHolder1(data: String) {
        binding.text.text = data
    }
}
```

> Note: Using `ViewBinding` is a must too and there's no other options for now but we do recommend to use it.

### ViewTypes

After successfully compile, the `MyAdapterImpl.kt` will auto-generated with the all the viewType you
need like below:

```kotlin
public class MyAdapterImpl(
    diffCallback: DiffUtil.ItemCallback<String>,
) : MyAdapter(diffCallback) {

    public companion object {
        public const val TYPE_MY_VIEW_HOLDER1: Int = 0

        public const val TYPE_MY_VIEW_HOLDER2: Int = 1
    }
}
```

> Note: The value and order will be the same as you declared in `@BindAdapter`

And you can go back to the abstract `Adapter` to write `getItemViewType` if you have multi-viewType
like below:

```kotlin
@BindAdapter([MyViewHolder1::class, MyViewHolder2::class])
abstract class MyAdapter(
    diffCallback: DiffUtil.ItemCallback<String>,
) : ListAdapter<String, RecyclerView.ViewHolder>(diffCallback) {

    override fun getItemViewType(position: Int): Int {
        return if (position % 2 == 0) {
            MyAdapterImpl.TYPE_MY_VIEW_HOLDER1
        } else {
            MyAdapterImpl.TYPE_MY_VIEW_HOLDER2
        }
    }
}
```

### Parameter

In real world you might want to pass value from `Adapter` to `ViewHolder`. If you want to achieve
this just label the parameter with the same `Annotation` from both side than we will help you to
link them together. For `Adapter`, please add parameter in constructor. For `ViewHolder`, you can
choose either constructor or the bind function which has `@BindFunction`.

```kotlin
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class BindListener

@BindAdapter([MyViewHolder1::class, MyViewHolder2::class])
abstract class MyAdapter(
    diffCallback: DiffUtil.ItemCallback<String>,
    @BindListener val listener: (String) -> Unit,
) : ListAdapter<String, RecyclerView.ViewHolder>(diffCallback)

class MyViewHolder1(
    private val binding: AdapterHolder1Binding,
) : RecyclerView.ViewHolder(binding.root) {

    @BindFunction
    fun bindHolder1(data: String, @BindListener listener: (String) -> Unit) {
        binding.text.text = data
        binding.root.setOnClickListener { listener.invoke(data) }
    }
}

class MyViewHolder2(
    @BindListener val listener: (String) -> Unit,
    val binding: AdapterHolder2Binding,
) : RecyclerView.ViewHolder(binding.root) {

    @BindFunction
    fun bindHolder2(String, data: String) {
        binding.text.text = data
        binding.root.setOnClickListener { listener.invoke(data) }
    }
}
```

Note: There's not type restriction for the parameter and we also don't force them to be equal, but
it will compile fail if it can't match.

## Contributing

It's an interesting project to me and it's also in very early stage for now. Any thing can be
changed and any kind of contribution or participate is welcome. Feel free to ask questions or report
bugs. And we're also welcome you to create new pr if you have any idea!!

## License

The module is available as open source under the terms of
the [MIT License](http://opensource.org/licenses/MIT).