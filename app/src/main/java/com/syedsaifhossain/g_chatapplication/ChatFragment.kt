package com.syedsaifhossain.g_chatapplication

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.adapter.ChatAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
import com.syedsaifhossain.g_chatapplication.models.Chats

class ChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: ArrayList<Chats> // Change to ArrayList

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using ViewBinding
        val binding = FragmentChatBinding.inflate(inflater, container, false)

        // Initialize RecyclerView
        recyclerView = binding.chatRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context) // Vertical List

        // Example data (You can replace it with dynamic data)
        messageList = arrayListOf(
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
            Chats(R.drawable.profile,"Aaron Loed", "I am fine")
        )

        // Set adapter
        chatAdapter = ChatAdapter(messageList){

            findNavController().navigate(R.id.action_homeFragment_to_chatScreenFragment)
        }
        recyclerView.adapter = chatAdapter

        // Show PopupMenu when addButton is clicked
        binding.addButton.setOnClickListener {
            showPopupMenu(it)
        }
        return binding.root
    }

    // Method to show PopupMenu
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.inflate(R.menu.popup_menu)

        // Force icons to show (for API 26 and above)
        popupMenu.setForceShowIcon(true)

        // Set a listener for the menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.newChats -> {
                    findNavController().navigate(R.id.action_homeFragment_to_newChatsFragment)
                    true
                }
                R.id.addContacts -> {
                    findNavController().navigate(R.id.action_homeFragment_to_addContactsFragment)
                    true
                }
                R.id.scan -> {
                    findNavController().navigate(R.id.action_homeFragment_to_scanFragment)
                    true
                }
                else -> false
            }
        }

        // Show the PopupMenu
        popupMenu.show()
    }
}