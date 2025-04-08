package com.syedsaifhossain.g_chatapplication

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu // Import PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // Import if using Navigation Component
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
// Import your specific Adapter and Model if you have them
// import com.syedsaifhossain.g_chatapplication.adapter.ChatAdapter
// import com.syedsaifhossain.g_chatapplication.models.Chat

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    // Declare your ChatAdapter instance here if you have one
    // private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        loadChatData() // Call function to load chat list data
    }

    // Sets up the toolbar, including the add button listener
    private fun setupToolbar() {
        // Set click listener for the add button using its ID from XML
        binding.addButton.setOnClickListener { anchorView ->
            showPopupMenu(anchorView) // Call function to show the menu
        }
        // You can add other toolbar setup here if needed (e.g., setting title dynamically)
    }

    // Sets up the RecyclerView for displaying chats
    private fun setupRecyclerView() {
        // Initialize your ChatAdapter here
        // chatAdapter = ChatAdapter { chatItem -> /* Handle chat item click */ }
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            // Set your adapter here
            // adapter = chatAdapter
        }
        // Placeholder message until adapter is implemented
        Toast.makeText(context, "Chat RecyclerView needs an Adapter to show chats", Toast.LENGTH_LONG).show()
    }

    // Placeholder function to load chat data
    private fun loadChatData() {
        // Replace this with your actual data loading logic
        // Example: Fetch chats from a database, network, or create dummy data
        // val dummyChatList = listOf(Chat(...), Chat(...))
        // chatAdapter.submitList(dummyChatList) // Update adapter with data
        Log.d("ChatFragment", "Placeholder: Load chat data implementation needed")
    }

    // Shows the popup menu anchored to the provided view (the add button)
    // setForceShowIcon requires API 29+ but is wrapped in a try-catch
    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        // Inflate the menu resource you provided (popup_menu.xml)
        popupMenu.inflate(R.menu.popup_menu)

        // Try to force icons to show (works on API 29 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                popupMenu.setForceShowIcon(true)
            } catch (e: Exception) {
                Log.w("ChatFragment", "Failed to force show icons in popup menu", e)
            }
        }

        // Set listener for menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // Use the IDs from your popup_menu.xml
                R.id.newChats -> {
                    // Handle "New Chats" action
                    Toast.makeText(context, "New Chats selected", Toast.LENGTH_SHORT).show()
                    // Add navigation logic here, e.g.:
                    // findNavController().navigate(R.id.action_chatFragment_to_newChatFragment)
                    true // Indicate the click was handled
                }
                R.id.addContacts -> {
                    // Handle "Add Contacts" action
                    Toast.makeText(context, "Add Contacts selected", Toast.LENGTH_SHORT).show()
                    // Add navigation logic here, e.g.:
                    // findNavController().navigate(R.id.action_chatFragment_to_addContactFragment)
                    true
                }
                R.id.scan -> {
                    // Handle "Scan" action
                    Toast.makeText(context, "Scan selected", Toast.LENGTH_SHORT).show()
                    // Add navigation logic here, e.g.:
                    // findNavController().navigate(R.id.action_chatFragment_to_scanFragment)
                    true
                }
                else -> false // Let the system handle other clicks
            }
        }
        popupMenu.show() // Display the popup menu
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding to avoid memory leaks
    }
}