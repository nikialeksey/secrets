package com.alexeycode.secrets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alexeycode.secrets.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = Secrets.secret2
    }
}