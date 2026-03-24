package com.example.aion_device.model

data class ModelInfo(
    val isInstalled: Boolean = false,
    val fileName: String? = null,
    val absolutePath: String? = null,
    val sizeBytes: Long = 0L,
    val guidance: String = "Import a local .task or .bin model file to unlock real on-device inference."
) {
    val sizeInMb: String
        get() = if (sizeBytes <= 0L) "0 MB" else String.format("%.1f MB", sizeBytes / 1024f / 1024f)
}
