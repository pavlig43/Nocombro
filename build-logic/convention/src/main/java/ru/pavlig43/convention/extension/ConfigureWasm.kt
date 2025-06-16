package ru.pavlig43.convention.extension

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig


//@OptIn(ExperimentalWasmDsl::class)
//fun Project.wasmTarget() {
//    kotlinMultiplatformConfig {
//        wasmJs {
////            this.moduleName = this@wasmTarget.name
////            this.moduleName = "12345"
//            println(this.moduleName)
//            browser {
//                commonWebpackConfig {
//                    this.outputFileName = "${moduleName}.js"
//                    devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
//                        static = (static ?: mutableListOf()).apply {
//                            add(this@wasmTarget.rootDir.path)
//                            add(this@wasmTarget.projectDir.path)
//                        }
//                    }
//                }
//            }
//            binaries.executable()
//        }
//    }
//}
