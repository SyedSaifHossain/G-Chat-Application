package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.syedsaifhossain.g_chatapplication.adapter.ChatMessageAdapter
import com.syedsaifhossain.g_chatapplication.adapter.UserAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatScreenBinding
import com.syedsaifhossain.g_chatapplication.models.MessageChat
import com.syedsaifhossain.g_chatapplication.models.User

class ChatScreenFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private lateinit var messagelist: ArrayList<MessageChat>

    var receiverRoom : String?=null

    var senderRoom : String?=null
    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        val name = arguments?.getString("name")
        val receiverUid = arguments?.getString("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid

        val database = FirebaseDatabase.getInstance().getReference()
        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid
        messagelist = ArrayList()
        chatMessageAdapter = ChatMessageAdapter(messagelist)
        binding.userRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
        binding.userRecyclerView.adapter = chatMessageAdapter
        database.child("chats").child(senderRoom!!).child("message")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    messagelist.clear()
                    for(postSnapshot in snapshot.children){
                        val message = postSnapshot.getValue(MessageChat::class.java)
                        messagelist.add(message!!)
                    }
                    chatMessageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })



binding.chatSendButton.setOnClickListener {

    val message = binding.chatMessageInput.text.toString()

    val messageObj = MessageChat(message,senderUid)
    database.child("chats").child(senderRoom!!).child("message").push().setValue(messageObj)
        .addOnSuccessListener {
            database.child("chats").child(receiverRoom!!).child("message").push().setValue(messageObj)
        }
    binding.chatMessageInput.setText(" ")

}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}