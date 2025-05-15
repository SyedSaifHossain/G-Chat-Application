package com.syedsaifhossain.g_chatapplication

import android.os.Build // Keep existing imports
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast // Import Toast
import android.util.Log // Import Log
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.syedsaifhossain.g_chatapplication.adapter.ChatAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
import com.syedsaifhossain.g_chatapplication.models.Chats // Make sure Chats model is imported

class ChatFragment : Fragment() {

    // Use ViewBinding for layout inflation
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!! // Non-null assertion

    // RecyclerView and Adapter members
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    // messageList will hold Chats objects
    private lateinit var messageList: ArrayList<Chats>

    @RequiresApi(Build.VERSION_CODES.Q) // Keep Q requirement if needed for popup menu icons
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Return non-nullable View
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val view = binding.root // Get root view from binding

        // Initialize RecyclerView using binding
        recyclerView = binding.chatRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context) // Set LayoutManager

        // --- Data Loading and Adapter Setup ---

        // 1. TODO: Replace example data with REAL data loading from Firebase later!
        // This should query the `/user-chats/{currentUserId}` node in Firebase
        // to get the list of recent chats.
        messageList = arrayListOf(
            Chats(
                imageRes = R.drawable.profile, // Still using drawable for example
                name = "Aaron Loed (Example)",
                message = "Hi from example!",
                otherUserId = "user_id_aaron", // Example ID
                otherUserAvatarUrl = "https://example.com/aaron_avatar.png" // Example URL
            ),
            Chats(
                imageRes = R.drawable.cityimg,
                name = "Jane Smith (Example)",
                message = "See you soon!",
                otherUserId = "user_id_jane",
                otherUserAvatarUrl = null // Example null URL
            )
        )

        // 2. TODO: IMPORTANT - Get the current user's avatar URL reliably!
        // Replace this placeholder with actual logic. Fetch from SharedPreferences,
        // ViewModel, or a one-time Firebase read in this Fragment.
        val myAvatarUrlPlaceholder = "https://example.com/my_avatar.png" // *** CRITICAL PLACEHOLDER ***

        // 3. Initialize and set the ChatAdapter with click listener containing navigation logic and checks
        chatAdapter = ChatAdapter(messageList) { clickedChatItem ->
            // --- ** PRE-NAVIGATION CHECKS START ** ---

            // a) Check if current user is logged in and UID is available
            val currentUserIdAuth = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserIdAuth.isNullOrBlank()) {
                Log.e("ChatFragment", "User not identified. Cannot navigate to chat screen.")
                Toast.makeText(requireContext(), "Please log in to chat", Toast.LENGTH_SHORT).show()
                return@ChatAdapter // Stop execution here if user is not valid
            }

            // b) Get the current user's avatar URL (replace placeholder!)
            // For now, we use the placeholder. Ensure this gets replaced!
            val actualMyAvatarUrl = myAvatarUrlPlaceholder // <-- ** REPLACE THIS **
            if (actualMyAvatarUrl.isBlank() || actualMyAvatarUrl == "https://example.com/my_avatar.png") { // Check if still placeholder or blank
                Log.w("ChatFragment", "Current user avatar URL is missing or using placeholder: $actualMyAvatarUrl")
                // Decide how to handle: pass null, pass placeholder, or show error?
                // Passing the placeholder might be okay if ChatScreenFragment handles it gracefully.
            }

            // --- ** PRE-NAVIGATION CHECKS END ** ---


            // 4. Prepare arguments for ChatScreenFragment
            val args = Bundle().apply {
                putString("otherUserId", clickedChatItem.otherUserId)

                // Clean the name before passing (remove "(Example)")
                val originalName = clickedChatItem.name
                val cleanedName = if (originalName.endsWith(" (Example)")) {
                    originalName.removeSuffix(" (Example)")
                } else {
                    originalName
                }
                putString("otherUserName", cleanedName)

                putString("otherUserAvatarUrl", clickedChatItem.otherUserAvatarUrl) // Pass URL (can be null)
                putString("myAvatarUrl", actualMyAvatarUrl) // Pass the (potentially placeholder) avatar URL
            }

            Log.d("ChatFragment", "Navigating to ChatScreen. OtherUser ID: ${clickedChatItem.otherUserId}, MyAvatar: $actualMyAvatarUrl")

            // 5. Perform Navigation with error handling
            try {
                // Use the correct action ID from your nav_graph.xml
                findNavController().navigate(R.id.action_homeFragment_to_chatScreenFragment, args)
            } catch (e: IllegalArgumentException) {
                Log.e("ChatFragment", "Navigation failed: Invalid argument or action ID?", e)
                Toast.makeText(context, "Error opening chat (Nav)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ChatFragment", "Navigation failed with unexpected error", e)
                Toast.makeText(context, "Error opening chat (Other)", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = chatAdapter // Set the adapter to the RecyclerView

        // --- End Data Loading and Adapter Setup ---


        // Setup PopupMenu for the addButton (remains the same)
        binding.addButton.setOnClickListener {
            showPopupMenu(it)
        }

        return view // Return the root view
    }

    // Method to show PopupMenu (remains the same)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.popup_menu)
        try { // Add try-catch for reflection method
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

    // Add onDestroyView to clean up binding (remains the same)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}



//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import android.view.*
//import android.widget.PopupMenu
//import androidx.annotation.RequiresApi
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//import com.syedsaifhossain.g_chatapplication.adapter.UserAdapter
//import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
//import com.syedsaifhossain.g_chatapplication.models.User
//
//class ChatFragment : Fragment() {
//
//    private lateinit var mAuth: FirebaseAuth
//    private lateinit var userList: ArrayList<User>
//    private lateinit var adapter: UserAdapter
//    private lateinit var mDbRef: DatabaseReference
//    private var _binding: FragmentChatBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentChatBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        mAuth = FirebaseAuth.getInstance()
//        mDbRef = FirebaseDatabase.getInstance().reference
//        userList = ArrayList()
//
//        adapter = UserAdapter(userList) { user ->
//            val bundle = Bundle().apply {
//                putString("userUid", user.uid)
//                putString("userPhone", user.phone?.toString())
//            }
//            findNavController().navigate(R.id.action_homeFragment_to_chatScreenFragment, bundle)
//        }
//
//        binding.userRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
//        binding.userRecyclerView.adapter = adapter
//        mDbRef.child("Users").addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                userList.clear()
//                for (postSnapshot in snapshot.children) {
//                    val currentUser = postSnapshot.getValue(User::class.java)
//
//                    if (mAuth.currentUser?.uid != currentUser?.uid) {
//                        userList.add(currentUser!!)
//                    }
//                }
//                adapter.notifyDataSetChanged()
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("ChatFragment", "Database error: ${error.message}")
//            }
//        })
//
//        binding.addButton.setOnClickListener {
//            showPopupMenu(it)
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun showPopupMenu(view: View) {
//        val popupMenu = PopupMenu(requireContext(), view)
//        popupMenu.inflate(R.menu.popup_menu)
//
//        try {
//            val method = PopupMenu::class.java.getDeclaredMethod(
//                "setForceShowIcon", Boolean::class.java
//            )
//            method.isAccessible = true
//            method.invoke(popupMenu, true)
//        } catch (e: Exception) {
//            Log.w("ChatFragment", "Failed to force show popup menu icons", e)
//        }
//
//        popupMenu.setOnMenuItemClickListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.newChats -> {
//                    findNavController().navigate(R.id.action_homeFragment_to_newChatsFragment)
//                    true
//                }
//                R.id.addContacts -> {
//                    findNavController().navigate(R.id.action_homeFragment_to_addContactsFragment)
//                    true
//                }
//                R.id.scan -> {
//                    findNavController().navigate(R.id.action_homeFragment_to_scanFragment)
//                    true
//                }
//                else -> false
//            }
//        }
//        popupMenu.show()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}