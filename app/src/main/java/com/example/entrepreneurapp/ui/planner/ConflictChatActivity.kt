package com.example.entrepreneurapp.ui.planner

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.entrepreneurapp.databinding.ActivityConflictChatBinding
import com.example.entrepreneurapp.models.ChatMessage
import com.example.entrepreneurapp.repository.AuthRepository
import com.example.entrepreneurapp.repository.ChatRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class ConflictChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConflictChatBinding
    private val chatRepository = ChatRepository()
    private val authRepository = AuthRepository()
    private lateinit var chatAdapter: ChatAdapter

    private var conflictDate = ""
    private var totalYield = 0.0
    private var riskLevel = ""
    private var farmerCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConflictChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conflictDate = intent.getStringExtra("conflictDate") ?: ""
        totalYield = intent.getDoubleExtra("totalYield", 0.0)
        riskLevel = intent.getStringExtra("riskLevel") ?: "normal"
        farmerCount = intent.getIntExtra("farmerCount", 0)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
        listenToMessages()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Group Chat - Conflict Resolution"
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Display conflict info
        binding.tvConflictInfo.text = "${getRiskEmoji()} - Total: ${totalYield.toInt()} tonnes"
        binding.tvParticipants.text = "Participants: $farmerCount farmers"
    }

    private fun getRiskEmoji(): String {
        return when (riskLevel.lowercase()) {
            "high" -> "🔴 HIGH RISK"
            "medium" -> "🟡 MEDIUM RISK"
            else -> "🟢 NORMAL"
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@ConflictChatActivity)
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadMessages() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = chatRepository.getMessages(conflictDate)
            binding.progressBar.visibility = View.GONE

            result.onSuccess { messages ->
                chatAdapter.submitList(messages)
                scrollToBottom()
            }.onFailure { exception ->
                Toast.makeText(this@ConflictChatActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listenToMessages() {
        chatRepository.listenToMessages(conflictDate) { messages ->
            chatAdapter.submitList(messages)
            scrollToBottom()
        }
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        binding.btnSend.isEnabled = false
        lifecycleScope.launch {
            val result = authRepository.getUserProfile()  // 👈 Changed this line
            result.onSuccess { user ->
                val message = ChatMessage(
                    conflictDate = conflictDate,
                    userId = user.uid,
                    userName = user.name,
                    userAvatar = user.avatar,
                    message = messageText,
                    timestamp = Timestamp.now(),
                    isResolution = false
                )

                val sendResult = chatRepository.sendMessage(message)
                binding.btnSend.isEnabled = true

                sendResult.onSuccess {
                    binding.etMessage.text.clear()
                }.onFailure { exception ->
                    Toast.makeText(this@ConflictChatActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { exception ->
                binding.btnSend.isEnabled = true
                Toast.makeText(this@ConflictChatActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scrollToBottom() {
        if (chatAdapter.itemCount > 0) {
            binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }
}