package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: List<Chats> // or ArrayList

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using ViewBinding
        val binding = FragmentChatBinding.inflate(inflater, container, false)

        // Initialize RecyclerView
        recyclerView = binding.chatRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context) // Vertical List

        // Example data (you can replace it with dynamic data)
        messageList = listOf(
            Chats(R.drawable.profile,"Aaron Loed", "Hi"),
            Chats(R.drawable.profile,"Aaron Loed", "How are you"),
            Chats(R.drawable.profile,"Aaron Loed", "I am fine"),
            Chats(R.drawable.profile,"Aaron Loed", "Hi"),
            Chats(R.drawable.profile,"Aaron Loed", "How are you"),
            Chats(R.drawable.profile,"Aaron Loed", "I am fine"),
            Chats(R.drawable.profile,"Aaron Loed", "Hi"),
            Chats(R.drawable.profile,"Aaron Loed", "How are you"),
            Chats(R.drawable.profile,"Aaron Loed", "I am fine"),
            Chats(R.drawable.profile,"Aaron Loed", "Hi"),
            Chats(R.drawable.profile,"Aaron Loed", "How are you"),
            Chats(R.drawable.profile,"Aaron Loed", "I am fine"),
            Chats(R.drawable.profile,"Aaron Loed", "Hi"),
            Chats(R.drawable.profile,"Aaron Loed", "How are you"),
            Chats(R.drawable.profile,"Aaron Loed", "I am fine"),
            Chats(R.drawable.profile,"Aaron Loed", "Hi"),
            Chats(R.drawable.profile,"Aaron Loed", "How are you"),
            Chats(R.drawable.profile,"Aaron Loed", "I am fine"),
            Chats(R.drawable.profile,"Aaron Loed", "Hi"),
            Chats(R.drawable.profile,"Aaron Loed", "How are you"),
            Chats(R.drawable.profile,"Aaron Loed", "I am fine"),
            Chats(R.drawable.profile,"Aaron Loed", "Hi"),
            Chats(R.drawable.profile,"Aaron Loed", "How are you"),
            Chats(R.drawable.profile,"Aaron Loed", "I am fine"),

        )

        // Set adapter
        chatAdapter = ChatAdapter(messageList)
        recyclerView.adapter = chatAdapter

        return binding.root
    }
}
