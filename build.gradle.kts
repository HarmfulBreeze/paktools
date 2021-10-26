import org.graalvm.buildtools.utils.SharedConstants

plugins {
    id("application")
    id("org.graalvm.buildtools.native") version "0.9.6"
}

group = "com.github.piorrro33"
version = "0.1"

application {
    mainClass.set("com.github.piorrro33.paktools.Main")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

repositories {
    mavenCentral()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set(project.name + SharedConstants.EXECUTABLE_EXTENSION)
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
            verbose.set(true)
        }
    }
}
