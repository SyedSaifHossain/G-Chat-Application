package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
        loadContactsFromFirebase()
        // 返回按钮点击事件
        binding.contactBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }
        // 搜索框监听
        binding.contactSearchbar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                contactAdapter.filterContacts(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        binding.contactRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        contactAdapter = ContactAdapter(requireContext(), emptyList()) { contact ->
            when (contact.type) {
                Contact.TYPE_NEW_FRIENDS -> {
                    findNavController().navigate(R.id.friendRequestsFragment)
                }
                Contact.TYPE_GROUP_CHATS -> {
                    findNavController().navigate(R.id.groupChatsFragment)
                }
                Contact.TYPE_NORMAL -> {
                    // 只处理普通联系人且id不为空
                    if (contact.id.isNotBlank()) {
                        val bundle = Bundle().apply {
                            putString("otherUserId", contact.id)
                            putString("otherUserName", contact.name)
                        }
                        findNavController().navigate(R.id.chatScreenFragment, bundle)
                    }
                }
                // 其它类型不做跳转
            }
        }
        binding.contactRecyclerView.adapter = contactAdapter
    }

    private fun loadContactsFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val friendsRef = usersRef.child(currentUser.uid).child("friends")

        friendsRef.get().addOnSuccessListener { snapshot ->
            val friendIds = snapshot.children.mapNotNull { it.key }
            val displayList = mutableListOf<Contact>()
            // 插入分组入口
            displayList.add(Contact(id = "new_friends", name = "New Friends", type = Contact.TYPE_NEW_FRIENDS))
            displayList.add(Contact(id = "group_chats", name = "Group Chats", type = Contact.TYPE_GROUP_CHATS))
            if (friendIds.isEmpty()) {
                contactAdapter.updateData(displayList)
                return@addOnSuccessListener
            }

            usersRef.get().addOnSuccessListener { usersSnapshot ->
                val contacts = friendIds.mapNotNull { fid ->
                    val userSnap = usersSnapshot.child(fid)
                    val name = userSnap.child("name").getValue(String::class.java) ?: ""
                    val phone = userSnap.child("phone").getValue(String::class.java) ?: ""
                    Contact(id = fid, name = name, phone = phone)
                }
                displayList.addAll(contacts)
                contactAdapter.updateData(displayList)
            }
        }
    }
}


