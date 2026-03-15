package com.alexeycode.secrets

import android.app.Activity
import android.os.Bundle
import com.alexeycode.secrets.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sampleText.text = Secrets.getSecret3()
    }
}