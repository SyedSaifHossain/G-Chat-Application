package com.syedsaifhossain.g_chatapplication

import android.os.Build // Keep existing imports
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
import com.syedsaifhossain.g_chatapplication.models.Chats // Make sure Chats model is imported

class ChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    // messageList will now hold Chats objects with more fields
    private lateinit var messageList: ArrayList<Chats>

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

        // --- MODIFICATION START ---

        // 1. TODO: Replace this with REAL data loading from Firebase later!
        // Create NEW example data that includes all required fields from the updated Chats class
        messageList = arrayListOf(
            Chats(
                imageRes = R.drawable.profile, // Still using drawable for now
                name = "Aaron Loed (Example)", // This will be otherUserName (original data)
                message = "Hi from example!",
                // Add values for the new fields:
                otherUserId = "user_id_aaron", // Example other user ID
                otherUserAvatarUrl = "https://example.com/aaron_avatar.png" // Example avatar URL (can be null)
            ),
            Chats(
                imageRes = R.drawable.cityimg, // Another example image
                name = "Jane Smith (Example)", // This will be otherUserName (original data)
                message = "See you soon!",
                // Add values for the new fields:
                otherUserId = "user_id_jane",
                otherUserAvatarUrl = null // Example where avatar URL might be null
            )
            // Add more sample chats here if needed for testing
        )

        // TODO: Get the current user's avatar URL (replace with actual logic)
        // For now, using a placeholder. You might get this after login or from user profile data.
        val myAvatarUrlPlaceholder = "https://example.com/my_avatar.png"

        // 2. Set adapter WITH argument passing logic in onItemClick
        chatAdapter = ChatAdapter(messageList) { clickedChatItem ->
            // This lambda is executed when a chat item is clicked
            // clickedChatItem is the Chats object for the clicked row

            // Prepare arguments to pass to ChatScreenFragment using a Bundle
            val args = Bundle().apply {
                putString("otherUserId", clickedChatItem.otherUserId)

                // --- MODIFIED: Clean the name before passing ---
                val originalName = clickedChatItem.name
                val cleanedName = if (originalName.endsWith(" (Example)")) {
                    originalName.removeSuffix(" (Example)")
                } else {
                    originalName
                }
                putString("otherUserName", cleanedName) // Pass the cleaned name
                // --- END MODIFICATION ---

                putString("otherUserAvatarUrl", clickedChatItem.otherUserAvatarUrl) // Pass the URL (can be null)
                putString("myAvatarUrl", myAvatarUrlPlaceholder) // Pass your own avatar URL placeholder
            }

            // Navigate to ChatScreenFragment, passing the arguments Bundle
            // Make sure the action ID is correct (it was defined in nav_graph.xml)
            findNavController().navigate(R.id.action_homeFragment_to_chatScreenFragment, args)
        }
        recyclerView.adapter = chatAdapter

        // --- MODIFICATION END ---

        // Show PopupMenu when addButton is clicked
        binding.addButton.setOnClickListener {
            showPopupMenu(it)
        }
        return binding.root
    }

    // Method to show PopupMenu (remains the same)
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