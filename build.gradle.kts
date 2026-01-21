import net.ltgt.gradle.errorprone.errorprone
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    kotlin("jvm")
    id("fabric-loom")
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.serialization")
    java
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone") version "4.3.0"
}
group = property("maven_group")!!
version = property("mod_version")!!

repositories {
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
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("maven.modrinth:modmenu:${property("mod_menu_version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${property("kotlinx_serialization_json_version")}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${property("kotlin_version")}")
    implementation("dev.babbaj:nether-pathfinder:${property("nether_pathfinder_version")}")
//    modImplementation("meteordevelopment:baritone:${property("baritone_version")}")
//    implementation("org.lwjgl:lwjgl-stb:${property("lwjgl_version")}")
    implementation("com.squareup.okhttp3:okhttp:${property("ok_http_version")}")
    implementation("org.apache.maven:maven-artifact:${property("maven_artifact_version")}")
    implementation("io.github.classgraph:classgraph:4.8.184")
    // テスト依存関係
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.12.0") // Mockkの最新安定版
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1") // JUnit Jupiter API
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1") // JUnit Jupiter Engine
    errorprone("com.google.errorprone:error_prone_core:2.45.0")
}
// 1. 環境設定の定義
val rustProjectDir = file("rust/infinite-client")
val generatedJavaDir: Directory = layout.projectDirectory.dir("src/main/java-generated") // jextractの出力先を分けるのがおすすめ

// 2. cbindgen タスク (Rustからヘッダー生成)
val generateRustHeaders = tasks.register<Exec>("generateRustHeaders") {
    group = "build"
    workingDir = rustProjectDir
    // cbindgen がインストールされている前提
    commandLine(
        "cbindgen",
        "--config",
        "cbindgen.toml",
        "--crate",
        "infinite-client",
        "--output",
        "../../build/rust/infinite_client.h",
    )
    outputs.file(file("build/rust/infinite_client.h"))
}

val jextractVersion = "25"
val jextractInstallDir: Provider<Directory> = layout.buildDirectory.dir("jextract-$jextractVersion")
val jextractBin: Provider<RegularFile> = jextractInstallDir.map {
    it.file(if (System.getProperty("os.name").lowercase().contains("win")) "bin/jextract.exe" else "bin/jextract")
}

val setupJextract: Provider<Task> = tasks.register("setupJextract") {
    group = "setup"
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val isWindows = os.contains("win")
    val isMac = os.contains("mac")
    val isLinux = os.contains("nix") || os.contains("nux")
    val isArm64 = arch.contains("aarch64") || arch.contains("arm64")
    // 見つけていただいたURLに基づき、プラットフォーム名を決定
    val platformTag = when {
        isWindows -> if (isArm64) "windows-aarch64" else "windows-x64"
        isMac -> if (isArm64) "macos-aarch64" else "macos-x64"
        isLinux -> if (isArm64) "linux-aarch64" else "linux-x64"
        else -> throw GradleException("Unsupported OS: $os")
    }

    // 基本URL (Java 25 EA 2-4)
    val downloadUrl =
        "https://download.java.net/java/early_access/jextract/25/2/openjdk-25-jextract+2-4_${platformTag}_bin." +
            (if (isWindows) "zip" else "tar.gz")

    val archiveFile = layout.buildDirectory.file("jextract-archive." + (if (isWindows) "zip" else "tar.gz"))

    // 入出力の定義を明確にすることで、GradleのUP-TO-DATEチェックを機能させる
    outputs.dir(jextractInstallDir)

    onlyIf {
        // 既に実行ファイルが存在する場合は実行しない
        !jextractBin.get().asFile.exists()
    }
    outputs.file(jextractBin)

    doLast {
        if (!jextractBin.get().asFile.exists()) {
            println("Downloading jextract 25 EA from $downloadUrl...")
            // 既存の古いディレクトリがあれば一度消去（権限エラー対策）
            if (jextractInstallDir.get().asFile.exists()) {
                jextractInstallDir.get().asFile.deleteRecursively()
            }
            URI(downloadUrl).toURL().openStream().use { input ->
                archiveFile.get().asFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            copy {
                val tree = if (isWindows) zipTree(archiveFile) else tarTree(resources.gzip(archiveFile))
                from(tree)
                into(jextractInstallDir)
                // 展開された最上位フォルダをスキップする処理
                eachFile { path = path.substringAfter("/") }
                includeEmptyDirs = false
            }

            if (!isWindows) {
                jextractBin.get().asFile.setExecutable(true)
            }
        }
    }
}
// 3. jextract タスク (ヘッダーからJavaバインディング生成)
val runJextract = tasks.register<Exec>("runJextract") {
    group = "build"
    // Rustヘッダー生成と、jextract本体のセットアップ両方に依存させる
    dependsOn(generateRustHeaders, setupJextract)

    val headerFile = file("build/rust/infinite_client.h")
    // doFirstの中で実行ファイルを確定させる
    doFirst {
        val jextractExecutable = when {
            // setupJextractでダウンロードされたファイルを最優先
            jextractBin.get().asFile.exists() -> jextractBin.get().asFile.absolutePath
            // 環境変数があればそれを使用
            System.getenv("JEXTRACT_HOME") != null -> "${System.getenv("JEXTRACT_HOME")}/bin/jextract"
            // どちらもなければシステムのパスに期待
            else -> "jextract"
        }

        println("Using jextract from: $jextractExecutable")

        // ExecタスクのcommandLineを上書き設定
        commandLine(
            jextractExecutable,
            "--output",
            generatedJavaDir,
            "--target-package",
            "org.infinite.nativebind",
            headerFile.absolutePath,
        )
    }
    // キャッシュを有効にするための入力・出力定義
    inputs.file(headerFile)
    outputs.dir(generatedJavaDir)
}

// 4. zigbuild タスク (クロスコンパイル対応)
// OSごとにターゲットを定義
val rustTargets = mapOf(
    "windows-x64" to "x86_64-pc-windows-gnu",
    "windows-arm64" to "aarch64-pc-windows-gnullvm",
    "linux-x64" to "x86_64-unknown-linux-gnu",
    "linux-arm64" to "aarch64-unknown-linux-gnu",
    "macos-x64" to "x86_64-apple-darwin",
    "macos-arm64" to "aarch64-apple-darwin",
)

// 各ターゲット向けのビルドタスクを個別に登録
rustTargets.forEach { (id, targetTriple) ->
    tasks.register<Exec>("rustBuild_$id") {
        group = "build"
        description = "Build Rust library for $id ($targetTriple) using zigbuild"
        workingDir = rustProjectDir

        // 開発環境に cargo-zigbuild がインストールされている必要があります
        commandLine("cargo", "zigbuild", "--release", "--target", targetTriple)

        // 出力ファイルのヒント（ビルドのスキップ判定用）
        outputs.dir(rustProjectDir.resolve("target/$targetTriple/release"))
    }
}

// 全プラットフォームのRustビルドをまとめるタスク
val buildRustAll: TaskProvider<Task> = tasks.register("buildRustAll") {
    group = "build"
    description = "Triggers Rust builds for all supported platforms"
    dependsOn(rustTargets.keys.map { "rustBuild_$it" })
    dependsOn(runJextract) // Rustの変更がヘッダー経由でJavaに反映されるように
}
sourceSets {
    main {
        // jextractで生成されたJavaコードをソースセットに含める
        java.srcDir(generatedJavaDir)
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        dependsOn(buildRustAll)
        // 各プラットフォームのバイナリを適切なディレクトリに配置
        rustTargets.forEach { (id, target) ->
            from("$rustProjectDir/target/$target/release") {
                include("*.so", "*.dll", "*.dylib")
                into("natives/$id")
            }
        }
    }

    loom {
        runs {
            configureEach {
                vmArg("--enable-native-access=ALL-UNNAMED")
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
        // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
        // if it is present.
        // If you remove this line, sources will not be generated.
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    jar {
        // 重複エラーの解決策
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        val archiveName = project.base.archivesName
        inputs.property("archivesName", archiveName)

        // clientソースセットが存在する場合のみ含める
        if (project.sourceSets.findByName("client") != null) {
            from(sourceSets["client"].output)
        }

        from("LICENSE")
    }
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifact(remapJar) {
                    builtBy(remapJar)
                }
                artifact(kotlinSourcesJar) {
                    builtBy(remapSourcesJar)
                }
            }
        }

        // select the repositories you want to publish to
        repositories {
            mavenLocal()
        }
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
        }
    }
    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
        }
    }
}
tasks.named<Jar>("sourcesJar") {
    dependsOn(runJextract)
}

tasks.withType<com.diffplug.gradle.spotless.SpotlessTask>().configureEach {
    mustRunAfter(buildRustAll)
    mustRunAfter(runJextract)
}

spotless {
    java {
        target("src/**/*.java")
        targetExclude("src/main/java-generated/**/*.java")
        googleJavaFormat()
    }
    kotlin {
        target("src/**/*.kt")
        // ここに設定を追加
        ktlint("1.8.0").editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ij_kotlin_allow_trailing_comma" to "true",
            ),
        )
    }
    groovyGradle {
        target("build.gradle")
        greclipse()
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled",
            ),
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        isEnabled.set(true)

        excludedPaths.set(".*/org/infinite/mixin/.*|.*/src/main/java-generated/.*")
        // 頻出するMixinアノテーションをリストアップ
        val mixinAnnotations = listOf(
            "org.spongepowered.asm.mixin.injection.Inject",
            "org.spongepowered.asm.mixin.injection.ModifyArg",
            "org.spongepowered.asm.mixin.injection.ModifyArgs",
            "org.spongepowered.asm.mixin.injection.ModifyConstant",
            "org.spongepowered.asm.mixin.injection.ModifyVariable",
            "org.spongepowered.asm.mixin.injection.Redirect",
            "org.spongepowered.asm.mixin.injection.At",
            "org.spongepowered.asm.mixin.Shadow",
            "org.spongepowered.asm.mixin.Overwrite",
            "org.spongepowered.asm.mixin.Unique",
        ).joinToString(",")

        option("UnusedMethod:ExemptAnnotations", mixinAnnotations)
        option("UnusedVariable:ExemptAnnotations", mixinAnnotations)
    }
}
tasks.register<JavaExec>("docs") {
    description = "Generate Documents"
    group = "application"
    classpath = sourceSets["client"].runtimeClasspath
    mainClass.set("org.infinite.docs.Infinite")
    args(project.rootDir.absolutePath)
}
