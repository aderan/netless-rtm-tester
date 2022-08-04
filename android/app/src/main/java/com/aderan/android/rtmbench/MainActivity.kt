package com.aderan.android.rtmbench

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aderan.android.rtmbench.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var clients = HashMap<String, Client>()

    private val handler = CoroutineExceptionHandler { context, exception ->
        println("Caught $exception")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.join.setOnClickListener {
            clients.values.forEachIndexed { index, client ->
                lifecycleScope.launch(handler) {
                    delay(index * 100L)
                    launch {
                        client.initChannel(Constants.CHANNLE_ID)
                        client.initChannelStatus()
                    }
                }
            }
        }

        binding.logout.setOnClickListener {
            clients.forEach {
                lifecycleScope.launch(handler) {
                    launch {
                        it.value.logout()
                    }
                }
            }
        }

        Constants.LINES.forEach {
            val item = it.split(":")
            val client = Client(userId = item[0], rtmToken = item[1])
            client.init(applicationContext)
            clients[item[0]] = client
        }
    }
}