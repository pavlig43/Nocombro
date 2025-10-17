package ru.pavlig43.convention.extension


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
