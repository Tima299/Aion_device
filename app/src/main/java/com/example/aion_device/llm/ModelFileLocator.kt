package com.example.aion_device.llm

import android.content.Context
import java.io.File

object ModelFileLocator {

    private val preferredFileNames = listOf(
        "model_version.task",
        "gemma-3-1b-it-int4.task",
        "gemma-3n-e2b-it-int4.task",
        "gemma-2b-it-cpu-int4.bin",
        "gemma2-2b-it-cpu-int4.bin",
    )

    fun resolveBestModelFile(context: Context): File? {
        val externalRoot = context.getExternalFilesDir(null)
        val modelDir = externalRoot?.resolve("models")

        val candidates = buildList {
            if (modelDir != null) {
                addAll(preferredFileNames.map { File(modelDir, it) })
                modelDir.listFiles()?.let(::addAll)
            }
            if (externalRoot != null) {
                addAll(preferredFileNames.map { File(externalRoot, it) })
                externalRoot.listFiles()?.let(::addAll)
            }
        }

        return candidates
            .filter { it.exists() && it.isFile && it.length() > 50L * 1024L * 1024L }
            .sortedWith(
                compareByDescending<File> { it.extension.equals("task", ignoreCase = true) }
                    .thenByDescending { it.length() }
            )
            .firstOrNull()
    }
}
