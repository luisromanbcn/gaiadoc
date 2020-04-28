package com.luisromanbcn.gaia.models

data class ContainerCreation(
    val image: String,
    val volumes: List<String>? = null,
    val name: String? = null,
    val portBindings: List<PortBindings>? = null,
    val restartAlways: Boolean = false,
    val networkMode: String? = null,
    val privileged: Boolean = false,
    val envVariables: List<String>? = null
)