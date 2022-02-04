package ai.bright.generator.types

import java.util.*

actual fun uuidString(): String = UUID.randomUUID().toString().lowercase()