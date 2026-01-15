
import net.ltgt.gradle.errorprone.errorprone
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Locale

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

    // テスト依存関係
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.12.0") // Mockkの最新安定版
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1") // JUnit Jupiter API
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1") // JUnit Jupiter Engine
    errorprone("com.google.errorprone:error_prone_core:2.45.0")
}

// Rustビルドタスクの定義
val buildRust = tasks.register<Exec>("buildRust") {
    group = "build"
    workingDir = file("src/main/rust")

    val osName = System.getProperty("os.name").lowercase(Locale.ENGLISH)

    // Linux環境であれば --target x86_64-unknown-linux-gnu を明示的に付けても良いですが、
    // 単体ビルド（ホスト向け）を優先するため標準のビルドを行います。
    commandLine("cargo", "build", "--release")

    // 自分のOS以外で実行しようとした場合にエラーで止めないための保険
    isIgnoreExitValue = true

    // 現在のOSがLinuxでない場合に実行をスキップしたい場合は以下を有効化
    // onlyIf { osName.contains("nux") }
}

tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        dependsOn(buildRust)
        // 成果物の場所を cargo のデフォルト（target/release）に合わせる
        from("src/main/rust/target/release") {
            include("*.so", "*.dll", "*.dylib")
            into("natives/linux-x64") // フォルダを分けておくとロード時に便利です
        }

        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            // 重複を避けるため expand は1回にまとめるのが安全
        }
    }
    loom {
        splitEnvironmentSourceSets()
        accessWidenerPath = file("src/main/resources/infinite.accesswidener")
    }

    fabricApi {
        configureDataGeneration {
            client = true
        }
    }
    java {
        // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
        // if it is present.
        // If you remove this line, sources will not be generated.
        withSourcesJar()

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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

        from("LICENSE") {
            rename { fileName -> "${fileName}_$archiveName" }
        }
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
            jvmTarget = JvmTarget.JVM_21
        }
    }
    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
    }
    kotlin {
        target("src/**/*.kt")
        // ここに設定を追加
        ktlint("0.50.0").editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ij_kotlin_allow_trailing_comma" to "true",
            ),
        )
    }
    groovyGradle {
        target("**/build.gradle")
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

        excludedPaths.set(".*/org/infinite/mixin/.*")
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
    mainClass.set("org.infinite.docs.Main")
    args(project.rootDir.absolutePath)
}
