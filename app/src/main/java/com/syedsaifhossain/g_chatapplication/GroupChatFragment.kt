package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.adapter.GroupMessageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentGroupChatBinding
import com.syedsaifhossain.g_chatapplication.models.GroupMessage

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var groupId: String
    private lateinit var groupChatRef: DatabaseReference
    private val messagesList = ArrayList<GroupMessage>()
    private lateinit var adapter: GroupMessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId = arguments?.getString("groupId") ?: return

        groupChatRef = FirebaseDatabase.getInstance()
            .getReference("GroupChats").child(groupId)

        adapter = GroupMessageAdapter(messagesList)
        binding.groupChatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.groupChatRecyclerView.adapter = adapter

        // Send on emoji button click
        binding.emojiButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) sendMessage(messageText)
        }

        // Send on Enter/Done key press
        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)
            ) {
                val messageText = binding.messageInput.text.toString().trim()
                if (messageText.isNotEmpty()) sendMessage(messageText)
                true
            } else {
                false
            }
        }


        binding.groupchatBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        listenForMessages()
    }

    private fun sendMessage(messageText: String) {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val messageId = groupChatRef.push().key ?: return

        val message = GroupMessage(
            senderId = senderId,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        groupChatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                binding.messageInput.setText("")
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
            }
            .addOnFailureListener {
                Log.e("GroupChat", "Failed to send: ${it.message}")
            }
    }

    private fun listenForMessages() {
        groupChatRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(GroupMessage::class.java)
                message?.let {
                    messagesList.add(it)
                    adapter.notifyItemInserted(messagesList.size - 1)
                    binding.groupChatRecyclerView.scrollToPosition(messagesList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}