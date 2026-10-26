package ru.haykor.carmanager.root.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.feature.car.component.CarComponent
import ru.haykor.carmanager.feature.carEditor.component.CarEditorComponent
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.main.component.MainComponent
import ru.haykor.carmanager.feature.serviceEditor.component.EditorMode
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorComponent

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val mainFactory: MainComponent.Factory,
    private val garageFactory: GarageComponent.Factory,
    private val carFactory: CarComponent.Factory,
    private val carEditorFactory: CarEditorComponent.Factory,
    private val serviceEditorFactory: ServiceEditorComponent.Factory
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
                        },
                        onAddCar = {
                            navigation.pushNew(Config.CarEditor(null))
                        }
                    )
                }
            )
        )

        is Config.Car -> RootComponent.Child.Car(
            carFactory.create(
                ctx = ctx,
                carId = config.carId,
                onBack = { navigation.pop() },
                onOpenEdit = { navigation.pushNew(Config.CarEditor(config.carId)) },
                onOpenRecord = { recordId -> navigation.pushNew(Config.ServiceEditor(EditorMode.Edit(recordId))) },
            )
        )

        is Config.CarEditor -> RootComponent.Child.CarEditor(
            carEditorFactory.create(
                ctx = ctx,
                id = config.carId,
                onBack = { navigation.pop() }
            )
        )

        is Config.ServiceEditor -> RootComponent.Child.ServiceEditor(
            serviceEditorFactory.create(
                ctx = ctx,
                mode = config.mode,
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

        @Serializable
        data class CarEditor(val carId: CarId? = null) : Config

        @Serializable
        data class ServiceEditor(val mode: EditorMode) : Config
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultRootComponentFactory(
    private val mainFactory: MainComponent.Factory,
    private val garageFactory: GarageComponent.Factory,
    private val carFactory: CarComponent.Factory,
    private val carEditorFactory: CarEditorComponent.Factory,
    private val serviceEditorFactory: ServiceEditorComponent.Factory
) : RootComponent.Factory {
    override fun create(ctx: ComponentContext): RootComponent = DefaultRootComponent(
        componentContext = ctx,
        mainFactory = mainFactory,
        garageFactory = garageFactory,
        carFactory = carFactory,
        carEditorFactory = carEditorFactory,
        serviceEditorFactory = serviceEditorFactory
    )
}
