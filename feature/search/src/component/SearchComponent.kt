package ru.haykor.carmanager.feature.search.component

import com.arkivanov.decompose.ComponentContext

interface SearchComponent {

    fun interface Factory {
        fun create(ctx: ComponentContext): SearchComponent
    }
}
