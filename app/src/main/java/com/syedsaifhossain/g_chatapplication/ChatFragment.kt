package com.syedsaifhossain.g_chatapplication

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.syedsaifhossain.g_chatapplication.adapter.ChatAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
import com.syedsaifhossain.g_chatapplication.models.Chats
import com.syedsaifhossain.g_chatapplication.api.FirebaseManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: ArrayList<Chats>

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val view = binding.root

        recyclerView = binding.chatRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize empty message list
        messageList = arrayListOf()

        // Initialize Adapter and set click event
        chatAdapter = ChatAdapter(messageList) { clickedChatItem ->
            val currentUserIdAuth = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserIdAuth.isNullOrBlank()) {
                Log.e("ChatFragment", "User not identified. Cannot navigate to chat screen.")
                Toast.makeText(requireContext(), "Please log in to chat", Toast.LENGTH_SHORT).show()
                return@ChatAdapter
            }

            // Asynchronously get current user's avatar URL
            lifecycleScope.launch {
                val myUser = FirebaseManager.UserManager.getUser(currentUserIdAuth)
                val myAvatarUrl = myUser?.avatarUrl ?: ""

                val args = Bundle().apply {
                    putString("otherUserId", clickedChatItem.otherUserId)
                    putString("otherUserName", clickedChatItem.name)
                    putString("otherUserAvatarUrl", clickedChatItem.otherUserAvatarUrl)
                    putString("myAvatarUrl", myAvatarUrl)
                }

                Log.d("ChatFragment", "Navigating to ChatScreen. OtherUser ID: ${clickedChatItem.otherUserId}, MyAvatar: $myAvatarUrl")

                try {
                    findNavController().navigate(R.id.action_homeFragment_to_chatScreenFragment, args)
                } catch (e: Exception) {
                    Log.e("ChatFragment", "Navigation failed", e)
                    Toast.makeText(context, "Error opening chat", Toast.LENGTH_SHORT).show()
                }
            }
        }
        recyclerView.adapter = chatAdapter

        // Load real chat data from FirebaseManager
        FirebaseManager.ChatManager.getUserChats { chats ->
            messageList.clear()
            messageList.addAll(chats)
            chatAdapter.notifyDataSetChanged()
        }

        binding.addButton.setOnClickListener {
            showPopupMenu(it)
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.popup_menu)
        try {
            popupMenu.setForceShowIcon(true)
        } catch (e: Exception) {
            Log.w("ChatFragment", "Failed to force show popup menu icons", e)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.newChats -> { findNavController().navigate(R.id.action_homeFragment_to_newChatsFragment); true }
                R.id.addContacts -> { findNavController().navigate(R.id.action_homeFragment_to_addContactsFragment); true }
                R.id.scan -> { findNavController().navigate(R.id.action_homeFragment_to_scanFragment); true }
                else -> false
            }
        }
        popupMenu.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}