plugins {
    java
    application
}

fun Project.jarConfig(mainClassFQName: String) {
    application {
        mainClass.set(mainClassFQName)
    }

    tasks.jar {
        archiveFileName.set("${project.name}.jar")
        manifest {
            attributes["Main-Class"] = mainClassFQName
        }
        configurations["compileClasspath"].forEach {
            from(zipTree(it.absolutePath))
        }
        destinationDirectory.set(rootProject.file("build"))
    }
}

allprojects {
    apply(plugin = "java")

    group = "fr.upem.chatfusion"
    version = "1.0"

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    }

    tasks.getByName<Test>("test") {
        useJUnitPlatform()
    }
}

project(":client") {
    apply(plugin = "application")

    dependencies {
        implementation(project(":common"))
    }

    jarConfig("fr.upem.chatfusion.client.Application")
}

project(":server") {
    apply(plugin = "application")

    dependencies {
        implementation(project(":common"))
    }

    jarConfig("fr.upem.chatfusion.server.Application")
}