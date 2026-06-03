package com.nexora.player.ui.navigation

import com.nexora.player.data.model.AppDestination

fun AppDestination.routeName(): String = name.lowercase()
