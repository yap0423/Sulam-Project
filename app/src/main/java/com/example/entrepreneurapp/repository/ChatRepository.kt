package com.example.entrepreneurapp.repository

import com.example.entrepreneurapp.models.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatCollection = db.collection("conflict_chats")

    suspend fun sendMessage(message: ChatMessage): Result<String> {
        return try {
            val docRef = chatCollection.document()
            val messageWithId = message.copy(id = docRef.id)
            docRef.set(messageWithId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(conflictDate: String): Result<List<ChatMessage>> {
        return try {
            val snapshot = chatCollection
                .whereEqualTo("conflictDate", conflictDate)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)
            }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToMessages(conflictDate: String, onMessagesChanged: (List<ChatMessage>) -> Unit) {
        chatCollection
            .whereEqualTo("conflictDate", conflictDate)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()

                onMessagesChanged(messages)
            }
    }
}