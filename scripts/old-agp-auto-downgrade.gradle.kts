import java.io.File


fun updateLibsVersions(
    filePath: String,
    oldAgpVersion: String,
) {
    val file = File(filePath)
    val lines = file.readLines().toMutableList()

    val versionUpdates =
        mapOf(
            "gradle_plugin_version" to oldAgpVersion,
            "lifecycleProcessVersion" to "2.3.1",
            "compose_version" to "2022.10.00",
            "kotlinx_serialization_json_version" to "1.5.1",
            "activity_compose_version" to "1.5.1",
            "core_version" to "1.6.0",
            "appcompat_version" to "1.6.1",
            "material_version" to "1.8.0",
            "core_ktx_version" to "1.6.0",
            "lifecycle_runtime_ktx_version" to "2.3.1",
            "kotlinx_coroutines_test_version" to "1.7.1",
            "kotlin" to "1.8.21",
            "kotlinx_coroutines_core_version" to "1.7.1",
        )

    val updatedLines =
        lines.map { line ->
            val trimmedLine = line.trim()
            versionUpdates.entries
                .find { (key, _) ->
                    trimmedLine.matches(Regex("^$key\\s*=.*"))
                }?.let { (key, value) ->
                    "$key = \"$value\""
                } ?: line
        }

    file.writeText(updatedLines.joinToString("\n"))
}

fun removeEdgeToEdgeReference() {
    val file =
        File("superwall/src/main/java/com/superwall/sdk/paywall/vc/SuperwallPaywallActivity.kt")
    val lines =
        file
            .readLines()
            .map { line ->
                if (line.contains("enableEdgeToEdge")) {
                    null
                } else {
                    line
                }
            }.filterNotNull()
            .toMutableList()
    file.writeText(lines.joinToString("\n"))
}

fun fixViewModelStoreReference() {
    val toWrite = "override fun getViewModelStore(): ViewModelStore"
    val text =
        File("superwall/src/main/java/com/superwall/sdk/paywall/vc/SuperwallStoreOwner.kt").let {
            val newText =
                it.readText().replace("override val viewModelStore: ViewModelStore", toWrite)
            it.writeText(newText)
        }
}

fun replaceEntriesCall() {
    val file = File("superwall/src/main/java/com/superwall/sdk/models/paywall/Paywall.kt")
    val text = file.readText().replace(".entries.", ".values().")
    file.writeText(text)
}

fun updateGradleWrapper(
    filePath: String,
    oldGradleVersion: String,
) {
    val file = File(filePath)
    val content = file.readText()
    val updatedContent =
        content.replace(
            Regex("distributionUrl=.*"),
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-$oldGradleVersion-bin.zip",
        )
    file.writeText(updatedContent)
}

fun updateBuildGradle(
    filePath: String,
    oldKotlinCompilerVersion: String,
) {
    val file = File(filePath)
    val lines = file.readLines().toMutableList()

    for (i in lines.indices) {
        if (lines[i].contains("kotlinCompilerExtensionVersion = ")) {
            lines[i] = "        kotlinCompilerExtensionVersion = \"$oldKotlinCompilerVersion\""
        }
    }

    // Replace packaging block with tasks.withType
    val packagingBlockStart = lines.indexOfFirst { it.contains("packaging {") }
    if (packagingBlockStart != -1) {
        val packagingBlockEnd =
            lines
                .subList(packagingBlockStart, lines.size)
                .indexOfFirst { it.contains("}") } + packagingBlockStart
        val newBlock =
            """
            tasks.withType<AbstractArchiveTask> {
                exclude("META-INF/LICENSE.md")
                exclude("META-INF/LICENSE-notice.md")
            }
            """.trimIndent()
        lines.subList(packagingBlockStart, packagingBlockEnd + 1).clear()
        lines.add(packagingBlockStart, newBlock)
    }

    file.writeText(lines.joinToString("\n"))
}

fun updateArtifactId(
    filePath: String,
    newArtifactId: String,
) {
    val file = File(filePath)
    val txt = file.readText()
    val updatedTxt =
        txt.replace(
            "artifactId = \"superwall-android\"",
            "artifactId = \"$newArtifactId\"",
        )
    file.writeText(updatedTxt)
}

fun action() {
    val projectDir = projectDir.toString()
    updateLibsVersions("$projectDir/gradle/libs.versions.toml", "7.4.2")
    updateGradleWrapper("$projectDir/gradle/wrapper/gradle-wrapper.properties", "7.5")
    updateBuildGradle("$projectDir/superwall/build.gradle.kts", "1.4.7")
    updateArtifactId("$projectDir/superwall/build.gradle.kts", "superwall-android-agp-7")
    removeEdgeToEdgeReference()
    fixViewModelStoreReference()
    replaceEntriesCall()
    println("AGP version update and artifactId change completed successfully.")
}

tasks.register("downgradeAgpTask") {
    group = "custom"
    description = "Downgrade AGP version and change artifactId"
    action()
}
