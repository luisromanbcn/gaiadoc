package com.luisromanbcn.gaia.models

data class PortBindings(
    val privatePort: Int,
    val publicPort: Int? = null,
    val publicRandomRangePort: String? = null
)