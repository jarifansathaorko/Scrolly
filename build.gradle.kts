import java.io.File

// Root build file for BrainRot Mobile App
// This orchestrates web build + Capacitor sync + Android APK assembly

val nodeModulesDir = File(rootDir, "node_modules")
val distDir = File(rootDir, "dist")
val androidDir = File(rootDir, "android")

tasks.register<Exec>("installDependencies") {
    description = "Install npm dependencies"
    group = "mobile"
    commandLine("npm", "ci")
    onlyIf { !nodeModulesDir.exists() }
}

tasks.register<Exec>("buildWeb") {
    description = "Build web assets with Vite"
    group = "mobile"
    commandLine("npm", "run", "build")
    dependsOn("installDependencies")
}

tasks.register<Exec>("syncCapacitor") {
    description = "Sync web assets to native projects via Capacitor"
    group = "mobile"
    commandLine("npx", "cap", "sync")
    dependsOn("buildWeb")
    onlyIf { androidDir.exists() }
}

tasks.register<Exec>("lint") {
    description = "Run TypeScript lint/type-check"
    group = "verification"
    commandLine("npm", "run", "lint")
}

tasks.register<Exec>("assembleDebug") {
    description = "Build BrainRot mobile app (web + Android debug APK)"
    group = "mobile"
    
    // First build web and sync
    dependsOn("buildWeb")
    
    // Then if android exists, build APK
    doLast {
        if (androidDir.exists()) {
            println("🤖 Building Android Debug APK...")
            val gradlew = File(androidDir, if (System.getProperty("os.name").lowercase().contains("win")) "gradlew.bat" else "gradlew")
            if (gradlew.exists()) {
                gradlew.setExecutable(true)
                val process = ProcessBuilder(gradlew.absolutePath, "assembleDebug")
                    .directory(androidDir)
                    .inheritIO()
                    .start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Android assembleDebug failed with exit code $exitCode")
                } else {
                    val apkPath = File(androidDir, "app/build/outputs/apk/debug/app-debug.apk")
                    if (apkPath.exists()) {
                        println("✅ APK built: ${apkPath.absolutePath} (${apkPath.length() / 1024 / 1024} MB)")
                        // Copy to root for convenience
                        val dest = File(rootDir, "brainrot-debug.apk")
                        apkPath.copyTo(dest, overwrite = true)
                        println("📦 Copied to: ${dest.absolutePath}")
                    }
                }
            } else {
                println("⚠️ gradlew not found, skipping native build")
            }
        } else {
            println("ℹ️ Android project not found. Run: npx cap add android")
        }
        println("✅ BrainRot web build complete: ${distDir.absolutePath}")
    }
}

tasks.register<Exec>("assembleRelease") {
    description = "Build BrainRot mobile app release APK"
    group = "mobile"
    dependsOn("buildWeb")
    
    doLast {
        if (androidDir.exists()) {
            println("🚀 Building Android Release APK...")
            val gradlew = File(androidDir, if (System.getProperty("os.name").lowercase().contains("win")) "gradlew.bat" else "gradlew")
            if (gradlew.exists()) {
                gradlew.setExecutable(true)
                val process = ProcessBuilder(gradlew.absolutePath, "assembleRelease")
                    .directory(androidDir)
                    .inheritIO()
                    .start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Android assembleRelease failed")
                } else {
                    val apkPath = File(androidDir, "app/build/outputs/apk/release/app-release-unsigned.apk")
                    if (apkPath.exists()) {
                        println("✅ Release APK: ${apkPath.absolutePath}")
                    }
                }
            }
        }
    }
}

tasks.register("cleanAll") {
    description = "Clean all build artifacts"
    group = "build"
    doLast {
        distDir.deleteRecursively()
        File(rootDir, "brainrot-debug.apk").delete()
        File(rootDir, "android/app/build").deleteRecursively()
        File(rootDir, "android/build").deleteRecursively()
        File(rootDir, "android/.gradle").deleteRecursively()
        println("🧹 Cleaned build artifacts")
    }
}

tasks.register("mobileInfo") {
    description = "Show mobile build info"
    group = "mobile"
    doLast {
        println("""
            📱 BrainRot Mobile App Build Info
            ================================
            App ID: com.brainrot.app
            App Name: BrainRot
            Version: 1.0.0
            Web Dir: dist/
            
            Available tasks:
              ./gradlew assembleDebug   - Build web + debug APK
              ./gradlew assembleRelease - Build web + release APK
              ./gradlew buildWeb        - Build web only
              ./gradlew syncCapacitor   - Sync to native
              npm run android:build     - Alternative via npm
              
            Capacitor:
              npx cap sync              - Sync assets
              npx cap open android      - Open in Android Studio
              npx cap run android       - Run on device/emulator
        """.trimIndent())
    }
}
