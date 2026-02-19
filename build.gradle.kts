import net.ltgt.gradle.errorprone.errorprone
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    kotlin("jvm")
    id("fabric-loom")
    id("maven-publish")
    id("eclipse")
    id("org.jetbrains.kotlin.plugin.serialization")
    java
    id("com.diffplug.spotless") version "8.0.0"
    id("net.ltgt.errorprone") version "4.3.0"
    id("org.xross")
}

group = property("maven_group")!!
version = property("mod_version")!!

repositories {
    mavenCentral()
    maven {
        name = "LocalCache"
        url = uri("$rootDir/local-m2")
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    maven {
        name = "babbaj-repo"
        url = uri("https://babbaj.github.io/maven/")
    }
}

eclipse {
    classpath {
        plusConfigurations.add(configurations.getByName("kotlinCompilerClasspath"))
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("compileKotlin")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(
        loom.layered {
            officialMojangMappings()
        },
    )
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("maven.modrinth:modmenu:${property("mod_menu_version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${property("kotlinx_serialization_json_version")}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${property("kotlin_version")}")
    implementation("dev.babbaj:nether-pathfinder:${property("nether_pathfinder_version")}")
    implementation("com.squareup.okhttp3:okhttp:${property("ok_http_version")}")
    implementation("org.apache.maven:maven-artifact:${property("maven_artifact_version")}")
    implementation("io.github.classgraph:classgraph:4.8.184")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    errorprone("com.google.errorprone:error_prone_core:2.45.0")
}

xross {
    rustProjectDir = project.file("rust/infinite-client").absolutePath
    packageName = "org.infinite.nativebind"
}

// --- Native Refresh Workflow ---

val cleanNative = tasks.register("cleanNative") {
    group = "native"
    description = "Cleans Rust target directory and generated Xross bindings."
    doLast {
        delete(project.file("rust/infinite-client/target"))
        delete(layout.buildDirectory.dir("generated/xross/kotlin"))
        // 検証エラーを避けるためにディレクトリだけ再作成しておく
        project.file("rust/infinite-client/target/xross").mkdirs()
        println("Native build artifacts and generated code cleaned.")
    }
}

val rustTargets = mapOf(
    "windows-x64" to "x86_64-pc-windows-gnu",
    "windows-arm64" to "aarch64-pc-windows-gnullvm",
    "linux-x64" to "x86_64-unknown-linux-gnu",
    "linux-arm64" to "aarch64-unknown-linux-gnu",
    "macos-x64" to "x86_64-apple-darwin",
    "macos-arm64" to "aarch64-apple-darwin",
)

val hostOs = System.getProperty("os.name").lowercase()
val hostArch = System.getProperty("os.arch").lowercase()
val hostIsArm64 = hostArch.contains("aarch64") || hostArch.contains("arm64")
val hostRustTargetId = when {
    hostOs.contains("win") -> if (hostIsArm64) "windows-arm64" else "windows-x64"
    hostOs.contains("mac") -> if (hostIsArm64) "macos-arm64" else "macos-x64"
    hostOs.contains("nix") || hostOs.contains("nux") -> if (hostIsArm64) "linux-arm64" else "linux-x64"
    else -> throw GradleException("Unsupported host OS for Rust build: $hostOs")
}

// デフォルトで全ターゲットビルドを有効にする (ユーザーの要望)
val buildAllRustTargets = providers.gradleProperty("buildRustAllTargets").getOrElse("true") == "true"

rustTargets.forEach { (id, targetTriple) ->
    tasks.register<Exec>("rustBuild_$id") {
        group = "build"
        description = "Build Rust library for $id ($targetTriple) using zigbuild"
        workingDir = project.file("rust/infinite-client")
        val useZigbuild = buildAllRustTargets || id != hostRustTargetId || providers.gradleProperty("useZigbuild").orNull == "true"

        // メタデータの競合を避けるため、buildディレクトリ内にターゲットごとのディレクトリを作成
        val targetMetadataDir = project.layout.buildDirectory.dir("xross-metadata/$id").get().asFile
        doFirst {
            targetMetadataDir.deleteRecursively()
            targetMetadataDir.mkdirs()
        }
        environment("XROSS_METADATA_DIR", targetMetadataDir.absolutePath)

        if (useZigbuild) {
            commandLine("cargo", "zigbuild", "--release", "--target", targetTriple)
        } else {
            commandLine("cargo", "build", "--release")
        }
        val outputDir = if (useZigbuild) {
            project.file("rust/infinite-client/target/$targetTriple/release")
        } else {
            project.file("rust/infinite-client/target/release")
        }
        outputs.dir(outputDir)
    }
}

val buildRustAll = tasks.register("buildRustAll") {
    group = "build"
    description = "Triggers Rust builds for all supported platforms"
    inputs.dir(project.file("rust/infinite-client"))
    val rustBuildTasks = if (buildAllRustTargets) {
        rustTargets.keys.map { "rustBuild_$it" }
    } else {
        listOf("rustBuild_$hostRustTargetId")
    }
    dependsOn(rustBuildTasks)
}

val mergeXrossMetadata = tasks.register("mergeXrossMetadata") {
    group = "native"
    description = "Merges Xross metadata from all build targets."
    dependsOn(buildRustAll)
    doLast {
        val mergedDir = project.file("rust/infinite-client/target/xross")
        mergedDir.deleteRecursively()
        mergedDir.mkdirs()

        val targetIds = if (buildAllRustTargets) rustTargets.keys else listOf(hostRustTargetId)
        targetIds.forEach { id ->
            val targetMetadataDir = project.layout.buildDirectory.dir("xross-metadata/$id").get().asFile
            if (targetMetadataDir.exists()) {
                targetMetadataDir.listFiles()?.forEach { file ->
                    if (file.extension == "json") {
                        file.copyTo(File(mergedDir, file.name), overwrite = true)
                    }
                }
            }
        }
    }
}

// Xrossのバインディング生成はRustのビルド（メタデータ生成）に依存する
tasks.named("generateXrossBindings") {
    dependsOn(mergeXrossMetadata)
}

// --- Rust Formatting ---

val rustFmt = tasks.register<Exec>("rustFmt") {
    group = "formatting"
    description = "Formats Rust code using cargo fmt"
    commandLine("cargo", "fmt", "--all")
}

val rustFmtCheck = tasks.register<Exec>("rustFmtCheck") {
    group = "verification"
    description = "Checks Rust code formatting using cargo fmt --check"
    commandLine("cargo", "fmt", "--all", "--", "--check")
}

tasks.named("spotlessApply") {
    dependsOn(rustFmt)
}

tasks.named("spotlessCheck") {
    dependsOn(rustFmtCheck)
}

val refreshNative = tasks.register("refreshNative") {
    group = "native"
    description = "Performs a clean rebuild of Rust binaries and regenerates Xross bindings."
    dependsOn(cleanNative)
    // 依存関係の連鎖により、generateXrossBindings を呼べば自動的に buildRustAll も走る
    finalizedBy("generateXrossBindings")
}

sourceSets {
    main {
        kotlin.srcDir(layout.buildDirectory.dir("generated/xross/kotlin"))
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
    processResources {
        dependsOn(buildRustAll)
        rustTargets.forEach { (id, target) ->
            val hostUsesZigbuild = buildAllRustTargets || providers.gradleProperty("useZigbuild").orNull == "true"
            val sourceDir = if (id == hostRustTargetId && !hostUsesZigbuild) {
                "${project.file("rust/infinite-client")}/target/release"
            } else {
                "${project.file("rust/infinite-client")}/target/$target/release"
            }
            from(sourceDir) {
                include("*.so", "*.dll", "*.dylib")
                into("natives/$id")
            }
        }
        val expandProps = mapOf("version" to project.version)
        inputs.properties(expandProps)
        filesMatching("fabric.mod.json") {
            expand(expandProps)
        }
    }

    loom {
        runs {
            configureEach {
                vmArg("--enable-native-access=ALL-UNNAMED")
                vmArg("-Dforeign.restricted=permit")
                vmArg("-XX:+UnlockDiagnosticVMOptions")
                vmArg("-XX:+AlwaysCompileLoopMethods")
            }
        }
        splitEnvironmentSourceSets()
        accessWidenerPath = file("src/main/resources/infinite.accesswidener")
    }

    fabricApi {
        configureDataGeneration {
            client = true
        }
    }
    compileJava {
        dependsOn(buildRustAll)
    }
    compileKotlin {
        dependsOn(buildRustAll)
    }
    java {
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        if (project.sourceSets.findByName("client") != null) {
            from(sourceSets["client"].output)
        }
        from("LICENSE")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
        }
    }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn("generateXrossBindings")
}

tasks.withType<com.diffplug.gradle.spotless.SpotlessTask>().configureEach {
    mustRunAfter(buildRustAll)
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
    }
    kotlin {
        target("src/**/*.kt")
        ktlint("1.8.0").editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ij_kotlin_allow_trailing_comma" to "true",
            ),
        )
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint().editorConfigOverride(
            mapOf("ktlint_standard_no-wildcard-imports" to "disabled"),
        )
    }
    format("rust") {
        target("**/*.rs")
        targetExclude("**/target/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("toml") {
        target("**/*.toml")
        targetExclude("**/target/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        isEnabled.set(true)
        excludedPaths.set(".*/org/infinite/mixin/.*")
        val mixinAnnotations = listOf(
            "org.spongepowered.asm.mixin.injection.Inject",
            "org.spongepowered.asm.mixin.injection.ModifyArg",
            "org.spongepowered.asm.mixin.injection.ModifyArgs",
            "org.spongepowered.asm.mixin.injection.ModifyConstant",
            "org.spongepowered.asm.mixin.injection.ModifyVariable",
            "org.spongepowered.asm.mixin.injection.Redirect",
            "org.spongepowered.asm.mixin.At",
            "org.spongepowered.asm.mixin.Shadow",
            "org.spongepowered.asm.mixin.Overwrite",
            "org.spongepowered.asm.mixin.Unique",
        ).joinToString(",")
        option("UnusedMethod:ExemptAnnotations", mixinAnnotations)
        option("UnusedVariable:ExemptAnnotations", mixinAnnotations)
        options.compilerArgs.addAll(
            listOf(
                "--add-modules",
                "jdk.incubator.vector",
                "-Xlint:-options",
                "-parameters",
            ),
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf("-Xlint:unchecked", "-Xlint:deprecation", "--release", "25"),
    )
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs.addAll(
            "-jvm-default=enable",
            "-Xbackend-threads=0",
            "-opt-in=kotlin.RequiresOptIn",
        )
    }
}
