package com.sunionrd.blelocker.Entity

data class ClassicShadow (
    val state: ClassicState
)

data class ClassicState (
    val desired: StateInfo,
    val reported: StateInfo,
    val delta: StateInfo,
)

data class StateInfo (
    val welcome: String,
    val color: String,
    val power: String,
)

