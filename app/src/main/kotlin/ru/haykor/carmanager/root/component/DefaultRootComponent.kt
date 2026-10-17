package ru.haykor.carmanager.root.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.haykor.carmanager.feature.car.component.CarComponent
import ru.haykor.carmanager.feature.car.domain.CarId
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.main.component.MainComponent

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val mainFactory: MainComponent.Factory,
    private val garageFactory: GarageComponent.Factory,
    private val carFactory: CarComponent.Factory
) : RootComponent,
    ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val childStack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Main,
            handleBackButton = true,
            childFactory = ::createChild
        )

    override fun onBack() {
        navigation.pop()
    }

    private fun createChild(config: Config, ctx: ComponentContext) = when (config) {
        Config.Main -> RootComponent.Child.Main(
            mainFactory.create(
                ctx,
                garage = { c ->
                    garageFactory.create(
                        ctx = c,
                        onOpenCar = { id ->
                            navigation.pushNew(Config.Car(id))
                        }
                    )
                }
            )
        )

        is Config.Car -> RootComponent.Child.Car(
            carFactory.create(
                ctx = ctx,
                carId = config.carId,
                onBack = { navigation.pop() }
            )
        )
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Main : Config

        @Serializable
        data class Car(val carId: CarId) : Config
    }
}
