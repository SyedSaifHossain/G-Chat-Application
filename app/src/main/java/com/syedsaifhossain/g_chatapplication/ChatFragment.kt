package com.syedsaifhossain.g_chatapplication

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.adapter.UserAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
import com.syedsaifhossain.g_chatapplication.models.User

class ChatFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var userList: ArrayList<User>
    private lateinit var adapter: UserAdapter
    private lateinit var mDbRef: DatabaseReference
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().reference
        userList = ArrayList()

        adapter = UserAdapter(userList) { user ->
            val bundle = Bundle().apply {
                putString("userUid", user.uid)
                putString("userPhone", user.phone?.toString())
            }
            findNavController().navigate(R.id.action_homeFragment_to_chatScreenFragment, bundle)
        }

        binding.userRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
        binding.userRecyclerView.adapter = adapter

        mDbRef.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (postSnapshot in snapshot.children) {
                    val currentUser = postSnapshot.getValue(User::class.java)

                    if (mAuth.currentUser?.uid != currentUser?.uid) {
                        userList.add(currentUser!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatFragment", "Database error: ${error.message}")
            }
        })

        binding.addButton.setOnClickListener {
            showPopupMenu(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.popup_menu)

        try {
            val method = PopupMenu::class.java.getDeclaredMethod(
                "setForceShowIcon", Boolean::class.java
            )
            method.isAccessible = true
            method.invoke(popupMenu, true)
        } catch (e: Exception) {
            Log.w("ChatFragment", "Failed to force show popup menu icons", e)
        }

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
        popupMenu.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}