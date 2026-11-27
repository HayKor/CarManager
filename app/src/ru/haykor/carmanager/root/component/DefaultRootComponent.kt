package ru.haykor.carmanager.root.component

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable
import ru.haykor.carmanager.core.domain.CarId
import ru.haykor.carmanager.core.domain.model.EditorMode
import ru.haykor.carmanager.feature.car.component.CarComponent
import ru.haykor.carmanager.feature.carEditor.component.CarEditorComponent
import ru.haykor.carmanager.feature.garage.component.GarageComponent
import ru.haykor.carmanager.feature.main.component.MainComponent
import ru.haykor.carmanager.feature.main.component.MainTab
import ru.haykor.carmanager.feature.search.component.SearchComponent
import ru.haykor.carmanager.feature.serviceEditor.component.ServiceEditorComponent
import ru.haykor.carmanager.photoviewer.component.PhotoViewerComponent

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val mainFactory: MainComponent.Factory,
    private val garageFactory: GarageComponent.Factory,
    private val searchFactory: SearchComponent.Factory,
    private val carFactory: CarComponent.Factory,
    private val carEditorFactory: CarEditorComponent.Factory,
    private val serviceEditorFactory: ServiceEditorComponent.Factory,
    private val photoViewerFactory: PhotoViewerComponent.Factory
) : RootComponent,
    ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val tabBackCallback = BackCallback(isEnabled = false) {
        activeMain()?.onTabSelected(MainTab.Garage)
    }

    private var pagesSubscription: Cancellation? = null
    private var stackSubscription: Cancellation? = null

    override val childStack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Main,
            handleBackButton = true,
            childFactory = ::createChild
        )

    init {
        backHandler.register(tabBackCallback)
        pagesSubscription = activeMain()?.pages?.subscribe { updateTabBackCallback() }
        stackSubscription = childStack.subscribe { updateTabBackCallback() }
        updateTabBackCallback()
        lifecycle.doOnDestroy {
            pagesSubscription?.cancel()
            stackSubscription?.cancel()
            backHandler.unregister(tabBackCallback)
        }
    }

    override fun onBack() {
        navigation.pop()
    }

    private fun activeMain(): MainComponent? =
        (childStack.value.active.instance as? RootComponent.Child.Main)?.component

    private fun updateTabBackCallback() {
        tabBackCallback.isEnabled =
            activeMain()?.pages?.value?.selectedIndex?.let { it != MainTab.Garage.ordinal } == true
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
                },
                search = { c ->
                    searchFactory.create(
                        ctx = c,
                        onOpenCar = { id -> navigation.pushNew(Config.Car(id)) },
                        onOpenRecord = { id -> navigation.pushNew(Config.ServiceEditor(EditorMode.Edit(id))) }
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
                onOpenCreateRecord = { navigation.pushNew(Config.ServiceEditor(EditorMode.Create(config.carId))) }
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
                onOpenImage = { photoPath -> navigation.pushNew(Config.PhotoViewer(photoPath)) },
                onBack = { navigation.pop() }
            )
        )

        is Config.PhotoViewer -> RootComponent.Child.PhotoViewer(
            photoViewerFactory.create(
                ctx = ctx,
                photoPath = config.photoPath,
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

        @Serializable
        data class PhotoViewer(val photoPath: String) : Config
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultRootComponentFactory(
    private val mainFactory: MainComponent.Factory,
    private val garageFactory: GarageComponent.Factory,
    private val searchFactory: SearchComponent.Factory,
    private val carFactory: CarComponent.Factory,
    private val carEditorFactory: CarEditorComponent.Factory,
    private val serviceEditorFactory: ServiceEditorComponent.Factory,
    private val photoViewerFactory: PhotoViewerComponent.Factory
) : RootComponent.Factory {
    override fun create(ctx: ComponentContext): RootComponent = DefaultRootComponent(
        componentContext = ctx,
        mainFactory = mainFactory,
        garageFactory = garageFactory,
        searchFactory = searchFactory,
        carFactory = carFactory,
        carEditorFactory = carEditorFactory,
        serviceEditorFactory = serviceEditorFactory,
        photoViewerFactory = photoViewerFactory
    )
}
