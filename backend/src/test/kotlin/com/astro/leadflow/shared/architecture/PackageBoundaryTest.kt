package com.astro.leadflow.shared.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class PackageBoundaryTest {

    @Test
    fun `codebase does not reference legacy modules namespace`() {
        val forbiddenNamespace = listOf("com", "astro", "leadflow", "modules").joinToString(".", postfix = ".")
        val sourceRoots = listOf(
            Path.of("src/main/kotlin"),
            Path.of("src/test/kotlin"),
        )

        val offenders = mutableListOf<String>()

        sourceRoots.filter(Files::exists).forEach { root ->
            Files.walk(root).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .forEach { file ->
                        val content = Files.readString(file)
                        if (content.contains(forbiddenNamespace)) {
                            offenders += file.toString()
                        }
                    }
            }
        }

        assertTrue(
            offenders.isEmpty(),
            "Found forbidden namespace references ($forbiddenNamespace) in: ${offenders.joinToString()}"
        )
    }

    @Test
    fun `shared modules namespace remains present in production code`() {
        val canonicalNamespace = "com.astro.leadflow.shared.modules."
        val mainRoot = Path.of("src/main/kotlin")

        val hasCanonicalUsage = Files.walk(mainRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .anyMatch { file -> Files.readString(file).contains(canonicalNamespace) }
        }

        assertTrue(
            hasCanonicalUsage,
            "Expected canonical namespace usage ($canonicalNamespace) in production source files."
        )
    }
}
