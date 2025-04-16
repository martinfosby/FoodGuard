package com.example.foodguard.llminference

object MLCBridge {
    init {
        System.loadLibrary("mlc_llm")
    }

    external fun initModel(modelPath: String): Boolean
    external fun runInference(prompt: String): String
}