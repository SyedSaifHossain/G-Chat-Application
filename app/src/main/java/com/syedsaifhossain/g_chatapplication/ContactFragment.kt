package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.adapter.ContactAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentContactBinding
import com.syedsaifhossain.g_chatapplication.models.Contact

class ContactFragment : Fragment() {

    private lateinit var binding: FragmentContactBinding
    private lateinit var contactAdapter: ContactAdapter

    private var contactsList: List<Contact> = listOf(
        Contact(id = "user_1", name = "Alice"),
        Contact(id = "user_2", name = "Bob"),
        Contact(id = "user_3", name = "Charlie")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.contactRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        contactAdapter = ContactAdapter(requireContext(), contactsList) { contact ->
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@ContactAdapter
            val chatId = generateChatId(currentUserId, contact.id)
            val db = FirebaseDatabase.getInstance().reference

            // 创建 chat 节点（可重复写入，不会报错）
            db.child("chats").child(chatId).child("participants").setValue(
                mapOf("user1" to currentUserId, "user2" to contact.id)
            ).addOnCompleteListener {
                // 使用 NavController 跳转到聊天界面
                val bundle = Bundle().apply {
                    putString("otherUserId", contact.id)
                    putString("otherUserName", contact.name)
                    // 可选传递头像参数：
                    // putString("otherUserAvatarUrl", contact.avatarUrl ?: "")
                    // putString("myAvatarUrl", currentUserAvatarUrl)
                }
                findNavController().navigate(R.id.chatScreenFragment, bundle)
            }
        }

        binding.contactRecyclerView.adapter = contactAdapter
    }

    private fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }
}


