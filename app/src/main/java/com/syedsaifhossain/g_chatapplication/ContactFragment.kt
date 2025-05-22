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
    }

    private fun setupRecyclerView() {
        binding.contactRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        contactAdapter = ContactAdapter(requireContext(), emptyList()) { contact ->
            // 点击联系人后跳转到ChatScreenFragment
            val bundle = Bundle().apply {
                putString("otherUserId", contact.id)
                putString("otherUserName", contact.name)
            }
            findNavController().navigate(R.id.chatScreenFragment, bundle)
        }
        binding.contactRecyclerView.adapter = contactAdapter
    }

    private fun loadContactsFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val friendsRef = usersRef.child(currentUser.uid).child("friends")

        friendsRef.get().addOnSuccessListener { snapshot ->
            val friendIds = snapshot.children.mapNotNull { it.key }
            if (friendIds.isEmpty()) {
                contactAdapter.updateData(emptyList())
                return@addOnSuccessListener
            }

            usersRef.get().addOnSuccessListener { usersSnapshot ->
                val contacts = friendIds.mapNotNull { fid ->
                    val userSnap = usersSnapshot.child(fid)
                    val name = userSnap.child("name").getValue(String::class.java) ?: ""
                    val phone = userSnap.child("phone").getValue(String::class.java) ?: ""
                    Contact(id = fid, name = name, phone = phone)
                }
                contactAdapter.updateData(contacts)
            }
        }
    }
}


