import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapNamesPolicy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.net.URI

plugins {
    // Shared plugins
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.js.plain.objects) apply false
    // foojay resolver applied in settings.gradle.kts
    alias(libs.plugins.vanniktechMavenPublish)

    // Only need within root
    // see: https://github.com/johnrengelman/shadow
    alias(libs.plugins.shadow) apply false
    id("jacoco")
}

//configurations.implementation {
//    exclude(module = "commons-logging")
//}

// Create the fat jar for the whole Naksha.
//rootProject.dependencies {
//    // This is needed, otherwise the blank root project will include nothing in the fat jar.
//    implementation(project(":here-naksha-app-service"))
//}
data class MvnInfo(val url: String, val user: String, val password: String)

data class SigningKey(val keyId: String, val key: String, val password: String)

fun Project.getMvnInfo(prefix: String): MvnInfo? {
    val url = getPropertyFromRootProject("${prefix}Url")
    val user = getPropertyFromRootProject("${prefix}User")
    val password = getPropertyFromRootProject("${prefix}Password")
    if (url != null && user != null && password != null) {
        return MvnInfo(url, user, password)
    }
    return null
}

fun Project.getMavenCentralInfo(): MvnInfo? {
    val url = getPropertyFromRootProject("mavenCentralUrl", "ORG_GRADLE_PROJECT_") ?: "https://central.sonatype.com/api/v1/publisher/deployments"
    val user = getPropertyFromRootProject("mavenCentralUsername", "ORG_GRADLE_PROJECT_")
    val password = getPropertyFromRootProject("mavenCentralPassword", "ORG_GRADLE_PROJECT_")
    if (user != null && password != null) {
        return MvnInfo(url, user, password)
    }
    return null
}

fun Project.getSigningKey(): SigningKey? {
    val keyId = getPropertyFromRootProject("signingInMemoryKeyId", "ORG_GRADLE_PROJECT_")
    val key = getPropertyFromRootProject("signingInMemoryKey", "ORG_GRADLE_PROJECT_")
    val pwd = getPropertyFromRootProject("signingInMemoryKeyPassword", "ORG_GRADLE_PROJECT_")
    if (keyId != null && key != null && pwd != null) {
        return SigningKey(keyId, key, pwd)
    }
    return null
}

enum class CleanAndTest {
    // Do not run tests
    OFF,
    // Run jvmTest
    KOTLIN,
}

enum class PublishModule {
    // do NOT publish the module to artifactory
    NO,
    // publish the module to artifactory
    YES,
    // configure the module for publication, but do not publish
    CONFIG_ONLY
}

// Modules that compile to JavaScript. The JS target configuration is centralized here.
@Suppress("ArrayInDataClass")
data class JsModuleConfig(
    val _moduleName: String,
    val _enableTests: Boolean = false,
    val _dependencies: Array<String> = emptyArray()
)
val jsModules = mapOf(
    Pair("here-naksha-lib-base", JsModuleConfig("naksha_base")),
    Pair("here-naksha-lib-auth", JsModuleConfig("naksha_auth")),
    Pair("here-naksha-lib-geo", JsModuleConfig("naksha_geo")),
    Pair("here-naksha-lib-jbon", JsModuleConfig("naksha_jbon")),
    Pair("here-naksha-lib-model", JsModuleConfig("naksha_model")),
    Pair("here-naksha-lib-psql", JsModuleConfig("naksha_psql", _dependencies = arrayOf(
        ":here-naksha-lib-base:jsNodeProductionLibraryDistribution",
        ":here-naksha-lib-geo:jsNodeProductionLibraryDistribution",
        ":here-naksha-lib-jbon:jsNodeProductionLibraryDistribution",
        ":here-naksha-lib-model:jsNodeProductionLibraryDistribution"))),
    Pair("here-naksha-lib-diff", JsModuleConfig("naksha_diff", _enableTests = true)),
)

val allModules = mapOf(
    Pair("naksha", Pair(CleanAndTest.OFF, PublishModule.CONFIG_ONLY)),
    Pair("here-naksha-app-service", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-common-http", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-handler-activitylog", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-auth", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-base", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-core", Pair(CleanAndTest.OFF, PublishModule.YES)),
    Pair("here-naksha-lib-diff", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-ext-manager", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-geo", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-handlers", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-hub", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-jbon", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-model", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-psql", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-view", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-storage-http", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-cli", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
    Pair("here-naksha-lib-mm-util", Pair(CleanAndTest.KOTLIN, PublishModule.YES)),
)

fun Project.configureVanniktechMavenPublish() {
    println("[$name] --> Configure publishing")
//    val keyId = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId")
//    val key = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
//    val keyPwd = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
//    val centralUser = System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername")
//    val centralPwd = System.getenv("ORG_GRADLE_PROJECT_mavenCentralPassword")
//    println("publish keyId: $keyId, key: $key, keyPwd: $keyPwd, centralUser: $centralUser, centralPwd: $centralPwd")
    val projectRepoURI = "github.com/heremaps/naksha"
    val here = getMvnInfo("here")
    if (here != null) {
        println("[$name] --> Add 'HereMaven' repository, ${here.user}:***@${here.url}")
        publishing {
            repositories {
                maven {
                    name = "HereMaven"
                    url = URI(here.url)
                    credentials.username = here.user
                    credentials.password = here.password
                }
            }
        }
    }
    val sign = getSigningKey()
    val portal = getMavenCentralInfo()
    mavenPublishing {
        if (sign != null && portal != null) {
            println("[$name] --> Add 'MavenCentral' repository, ${portal.user}:***@${portal.url}")
            println("[$name] --> Configure mavenPublishing for 'local' and 'CENTRAL_PORTAL'")
            publishToMavenCentral()
            signAllPublications()
        } else {
            println("[$name] --> Configure mavenPublishing for 'local'")
        }
        coordinates(group.toString(), name, version.toString())
        pom {
            name = project.name
            description = project.gatherDescription() ?: throw Exception("No Description for ${project.name}")
            url = "https://${projectRepoURI}"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            scm {
                connection = "scm:git:https://${projectRepoURI}.git"
                developerConnection = "scm:git:ssh://git@${projectRepoURI}.git"
                url = "https://${projectRepoURI}"
            }
            developers {
                developer {
                    id = "xeus2001"
                    name = "Alexander Lowey-Weber"
                    email = "naksha@here.com"
                    url = "https://github.com/xeus2001"
                }
                developer {
                    id = "gunplar"
                    name = "Phuc Mai"
                    url = "https://github.com/gunplar"
                }
                developer {
                    id = "hirenkp2000"
                    name = "Hiren Patel"
                    url = "https://github.com/hirenkp2000"
                }
                developer {
                    id = "Amaneusz"
                    name = "Jakub Amanowicz "
                    url = "https://github.com/Amaneusz"
                }
            }
        }
        //println("--------- group: $group, version: $version, mavenUrl: $mavenUrl, mavenUser: $mavenUser, mavenPassword: $mavenPassword")
//        artifacts {
//            file("build/libs/${project.name}-${project.version}.jar")
//            file("build/libs/${project.name}-${project.version}-javadoc.jar")
//            file("build/libs/${project.name}-${project.version}-sources.jar")
//        }
    }
}

// To be sure that we get that early on for publication (somehow this is collected early)
group = "io.github.naksha-oss"
version = getRequiredPropertyFromRootProject("version")

allprojects {
    group = "io.github.naksha-oss"
    version = getRequiredPropertyFromRootProject("version")

    repositories {
        maven("https://repo.osgeo.org/repository/release/")
        mavenCentral()
        mavenLocal()
    }

    println("[$name] --> $group:$name:$version")
    val _name = name
    if (project !== rootProject) {
        println("[$_name] --> Apply KMP plugin")
        apply(plugin = "org.jetbrains.kotlin.multiplatform")

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            val jvmTargetName = getJvmTargetName(this@allprojects)
            println("[$_name] --> Configure Kotlin to $jvmTargetName")
            val target = JvmTarget.entries.find { it.name == jvmTargetName } ?: throw IllegalStateException(
                "Invalid JVM target version '${getJvmTargetVersion(this@allprojects)}' defined in gradle.properties " +
                        "(property 'jvm.target'). The Kotlin compiler does not provide a constant for '$jvmTargetName'. " +
                        "Please choose a supported version (e.g., 21, 17, 11)."
            )
            compilerOptions.jvmTarget.set(target)
        }
        val jvmTargetVersion: String = getJvmTargetVersion(this@allprojects)
        val jvmToolchainVersion: String = getJvmToolchainVersion( this@allprojects )
        plugins.withType<JavaPlugin> {
            extensions.configure<JavaPluginExtension> {
                println("[$_name] --> Configure JavaPlugin to JDK $jvmTargetVersion")
                toolchain { languageVersion.set(JavaLanguageVersion.of(jvmToolchainVersion.toInt())) }
                sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
                targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(jvmTargetVersion.toInt())
        }
        tasks.withType<Jar> {
            from(rootProject.file("HERE_NOTICE"))
            into("")
            from(rootProject.file("LICENSE"))
            into("")
        }
        val jsNoduleConfig = jsModules[_name]
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                    freeCompilerArgs.add("-opt-in=kotlin.ExperimentalUnsignedTypes,kotlin.RequiresOptIn,kotlin.js.ExperimentalJsExport,kotlin.js.ExperimentalJsStatic")
                    freeCompilerArgs.add("-Xes-long-as-bigint")
                    freeCompilerArgs.add("-XXLanguage:+JsAllowLongInExportedDeclarations")
                }
                jvm {
                    compilerOptions {
                        freeCompilerArgs.add("-Xexpect-actual-classes")
                        freeCompilerArgs.add("-opt-in=kotlin.ExperimentalUnsignedTypes,kotlin.RequiresOptIn")
                    }
                }
                jvmToolchain(jvmToolchainVersion.toInt())
                if (jsNoduleConfig != null) {
                    val _moduleName = jsNoduleConfig._moduleName
                    println("[$_name] --> Configure JavaScript, moduleName=${_moduleName}")
                    js {
                        outputModuleName = _moduleName
                        useEsModules()
                        compilerOptions {
                            // See: https://kotlinlang.org/docs/js-to-kotlin-interop.html#use-bigint-type-to-represent-kotlin-s-long-type
                            target = "es2015"
                            freeCompilerArgs.add("-Xexpect-actual-classes")
                            freeCompilerArgs.add("-opt-in=kotlin.ExperimentalUnsignedTypes,kotlin.RequiresOptIn,kotlin.js.ExperimentalJsExport,kotlin.js.ExperimentalJsStatic")
                            freeCompilerArgs.add("-Xes-long-as-bigint")
                            freeCompilerArgs.add("-XXLanguage:+JsAllowLongInExportedDeclarations")
                        }
                        nodejs {
                            compilerOptions {
                                moduleKind = JsModuleKind.MODULE_ES
                                moduleName = _moduleName
                                sourceMap = true
                                useEsClasses = true
                                sourceMapNamesPolicy = JsSourceMapNamesPolicy.SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES
                                sourceMapEmbedSources = JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
                            }
                            generateTypeScriptDefinitions()
                            binaries.library()
                            binaries.executable()
                        }
                    }
                }
                project.tasks {
                    getByName<Test>("jvmTest") {
                        useJUnitPlatform()
                        maxHeapSize = "8g"
                        val dbUrl = System.getenv("NAKSHA_TEST_PSQL_DB_URL")
                        if (dbUrl != null) environment("NAKSHA_TEST_PSQL_DB_URL", dbUrl)
                    }
                }

                // We patch the build so that the JavaScript files are included into the JAR!
                if (jsNoduleConfig != null) {
                    val _dependencies = jsNoduleConfig._dependencies
                    val _enableTests = jsNoduleConfig._enableTests
                    println("[$_name] --> Configure JavaScript dependencies: ${_dependencies.contentToString()} and tests: $_enableTests")
                    project.tasks {
                        getByName<Task>("jsNodeProductionLibraryDistribution") {
                            dependsOn("jsProductionLibraryCompileSync", "jsProductionExecutableCompileSync")
                        }
                        // Release
                        getByName<ProcessResources>("jvmProcessResources") {
                            dependsOn(_dependencies + "jsNodeProductionLibraryDistribution") // "jsBrowserDistribution"
                        }
                        getByName<Jar>("jvmJar") { dependsOn("jvmProcessResources") }
                        // Test
                        getByName<ProcessResources>("jvmTestProcessResources") { dependsOn("jvmProcessResources") }
                    }
                    project.tasks.matching { it.name == "jsNodeTest" }.configureEach {
                        enabled = _enableTests
                    }
                }
            }
        }

        if(allModules[_name]?.first == CleanAndTest.KOTLIN) {
            println("[$_name] --> Configure Kotlin tests, including Jacoco reports")
            apply(plugin = "jacoco")

            jacoco {
                toolVersion = rootProject.libs.versions.jacoco.get()
                reportsDirectory = layout.buildDirectory.dir("reports/jacoco")
            }

            tasks {
                val jacocoTestReport = register<JacocoReport>("jacocoTestReport") {
                    group = "jacoco"

                    dependsOn("jvmTest")
                    configureJacocoForKmp(project)
                    reports {
                        xml.required = true
                    }
                }
                register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
                    group = "jacoco"
                    dependsOn(jacocoTestReport)
                    val reportTask = jacocoTestReport.get()
                    sourceDirectories.setFrom(reportTask.sourceDirectories)
                    classDirectories.setFrom(reportTask.classDirectories)
                    executionData.setFrom(reportTask.executionData)
                    violationRules {
                        rule {
                            limit {
                                minimum = getOverallCoverage().toBigDecimal()
                            }
                        }
                    }
                }
            }
        }
    }

    val info = allModules[_name]
    if (info != null && info.second == PublishModule.YES) {
        println("[$_name] --> Configure publication")
        apply(plugin = "com.vanniktech.maven.publish")
        configureVanniktechMavenPublish()
    }
}

jacoco {
    toolVersion = rootProject.libs.versions.jacoco.get()
    reportsDirectory = layout.buildDirectory.dir("reports/jacoco")
}

// Helper, run as `gradle cleanAndTestAll`
fun Task.configureCleanAndTestTasks() {
    allModules.forEach {
        when (it.value.first) {
            CleanAndTest.KOTLIN -> {
                println("Run tests for ${it.key}")
                dependsOn(":${it.key}:cleanJvmTest")
                dependsOn(":${it.key}:jvmTest")
            }
            else -> {}
        }
    }
}
tasks.register("cleanAndTestAll") { configureCleanAndTestTasks() }

tasks.register<JacocoReport>("jacocoAggregatedTestReport") {
    group = "jacoco"

    val modulesWithTestsEnabled = allModules.filter {(_, moduleInfo) ->  moduleInfo.first == CleanAndTest.KOTLIN }
    modulesWithTestsEnabled.forEach {(moduleName, _) ->
        dependsOn(":${moduleName}:jvmTest")
    }
    modulesWithTestsEnabled
        .map { (moduleName, _) -> project(":${moduleName}")
        .extractJacocoProjectDirs() }
        .merge()
        .let {
            sourceDirectories.setFrom(it.sourceDirectories)
            classDirectories.setFrom(it.classDirectories)
            executionData.setFrom(it.executionData)
        }

    reports {
        xml.required = true
    }
}

tasks.register<JacocoCoverageVerification>("jacocoAggreagetedTestCoverageVerification") {
    group = "jacoco"

    val reportTask = tasks.named<JacocoReport>("jacocoAggregatedTestReport").get()
    dependsOn(reportTask)
    sourceDirectories.setFrom(reportTask.sourceDirectories)
    classDirectories.setFrom(reportTask.classDirectories)
    executionData.setFrom(reportTask.executionData)
    violationRules {
        rule {
            limit {
                minimum = getOverallCoverage().toBigDecimal()
            }
        }
    }
}

// Helper, run as `gradle publishToLocal`
fun Task.publishToLocal() {
    allModules.forEach {
        when (it.value.second) {
            PublishModule.YES -> {
                //val project = project(":${it.key}")
                //project.pluginManager.apply("naksha.publish")
                dependsOn(":${it.key}:publishJvmPublicationToMavenLocal")
                dependsOn(":${it.key}:publishKotlinMultiplatformPublicationToMavenLocal")
            }
            PublishModule.CONFIG_ONLY -> {}
            else -> {}
        }
    }
}
tasks.register("publishToLocal") { publishToLocal() }

// Helper, run as `gradle publishToHere`
fun Task.publishToHere() {
    getMvnInfo("here") ?:
        throw IllegalArgumentException(
            "Missing 'hereUrl', 'hereUser', and 'herePassword', declare in environment-variable or ~/.gradle/gradle.properties"
        )
    allModules.forEach {
        when (it.value.second) {
            PublishModule.YES -> {
                dependsOn(":${it.key}:clean")
                dependsOn(":${it.key}:publishJvmPublicationToHereMavenRepository")
                dependsOn(":${it.key}:publishKotlinMultiplatformPublicationToHereMavenRepository")
            }
            PublishModule.CONFIG_ONLY -> {}
            else -> {}
        }
    }
}
tasks.register("publishToHere") { publishToHere() }

// Helper, run as `gradle publishToCentral`
fun Task.publishToCentral() {
    val sign = getSigningKey()
    if (sign == null) {
        println("To publish to maven central define the following properties in you gradle.properties:")
        println("\tsigningInMemoryKeyId")
        println("\tsigningInMemoryKey")
        println("\tsigningInMemoryKeyPassword")
        println("Or export them as env variables prefixed with 'ORG_GRADLE_PROJECT_':")
        println("\tORG_GRADLE_PROJECT_signingInMemoryKeyId")
        println("\tORG_GRADLE_PROJECT_signingInMemoryKey")
        println("\tORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
        throw IllegalArgumentException("Missing signing key")
    }
    val central = getMavenCentralInfo()
    if (central == null) {
        println("To publish to maven central define the following properties in you gradle.properties:")
        println("\tmavenCentralUsername")
        println("\tmavenCentralPassword")
        println("Or export them as env variables prefixed with 'ORG_GRADLE_PROJECT_':")
        println("\tORG_GRADLE_PROJECT_mavenCentralUsername")
        println("\tORG_GRADLE_PROJECT_mavenCentralPassword")
        throw IllegalArgumentException("Missing central credentials")
    }
    allModules.forEach {
        when (it.value.second) {
            PublishModule.YES -> {
                println("Publish to Maven-Central: ${it.key}")
                dependsOn(":${it.key}:publishToMavenCentral")
                //dependsOn(":${it.key}:publishAndReleaseToMavenCentral")
            }
            PublishModule.CONFIG_ONLY -> {}
            else -> {}
        }
    }
}
tasks.register("publishToCentral") { publishToCentral() }

tasks.register("shadowJar") {
    dependsOn(":here-naksha-app-service:shadowJar")
}

data class JacocoProjectDirs(
    val sourceDirectories: ConfigurableFileCollection,
    val classDirectories: ConfigurableFileCollection,
    val executionData: ConfigurableFileCollection
)

fun Project.extractJacocoProjectDirs(): JacocoProjectDirs {
    val kotlinExtension = requireNotNull(
        extensions.findByType(KotlinMultiplatformExtension::class.java)
    ) { "KotlinMultiplatformExtension not found in project '$name'" }
    val sourceSets = kotlinExtension.sourceSets
    val commonSrcDirs = sourceSets.getByName("commonMain").kotlin.srcDirs
    val jvmSrcDirs = sourceSets.getByName("jvmMain").kotlin.srcDirs
    val buildDirectory = layout.buildDirectory
    val classesDirs = kotlinExtension.jvm().compilations.getByName("main").output.classesDirs
    val buildData = buildDirectory.files("jacoco/jvmTest.exec")
    return JacocoProjectDirs(
        sourceDirectories = files(commonSrcDirs + jvmSrcDirs),
        classDirectories = classesDirs,
        executionData = files(buildData)
    )
}

fun JacocoReportBase.configureJacocoForKmp(project: Project) {
    project.extractJacocoProjectDirs().let {
        sourceDirectories.setFrom(it.sourceDirectories)
        classDirectories.setFrom(it.classDirectories)
        executionData.setFrom(it.executionData)
    }
}

fun List<JacocoProjectDirs>.merge(): JacocoProjectDirs {
    val allSourceDirs = flatMap { it.sourceDirectories }
    val allClassDirs = flatMap { it.classDirectories }
    val allExecData = flatMap { it.executionData }

    return JacocoProjectDirs(
        sourceDirectories = files(allSourceDirs),
        classDirectories = files(allClassDirs),
        executionData = files(allExecData)
    )
}