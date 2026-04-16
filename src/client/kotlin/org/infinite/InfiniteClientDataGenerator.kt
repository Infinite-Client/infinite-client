package org.infinite

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import org.infinite.utils.Document
import java.nio.file.Path

object InfiniteClientDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        var path = java.nio.file.Paths.get("").toAbsolutePath()
        // Try to find the root directory that contains infinite-client
        while (path != null && !java.nio.file.Files.exists(path.resolve("infinite-client"))) {
            path = path.parent
        }
        val projectRoot = path ?: java.nio.file.Paths.get("").toAbsolutePath()

        println("Generating documentation from DataGenerator at: $projectRoot")
        val document = Document(projectRoot)
        document.generateData()
        document.getTranslations()
        document.generateDocs()
    }
}
