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
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.adapter.ChatAdapter
import com.syedsaifhossain.g_chatapplication.adapter.UserAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
import com.syedsaifhossain.g_chatapplication.models.Chats
import com.syedsaifhossain.g_chatapplication.models.User
import com.syedsaifhossain.g_chatapplication.api.FirebaseManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var userAdapter: UserAdapter
    private lateinit var messageList: ArrayList<Chats>
    private lateinit var userList: ArrayList<User>
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val view = binding.root

        // 初始化聊天列表
        setupChatList()
        
        // 初始化好友列表
        setupUserList()

        return view
    }

    private fun setupChatList() {
        recyclerView = binding.chatRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 初始化空消息列表
        messageList = arrayListOf()

        // 初始化聊天适配器
        chatAdapter = ChatAdapter(messageList) { clickedChatItem ->
            navigateToChatScreen(clickedChatItem)
        }
        recyclerView.adapter = chatAdapter

        // 从 Firebase 加载聊天数据
        FirebaseManager.ChatManager.getUserChats { chats ->
            messageList.clear()
            messageList.addAll(chats)
            chatAdapter.notifyDataSetChanged()
        }
    }

    private fun setupUserList() {
        // 初始化用户列表
        userList = arrayListOf()
        
        // 初始化用户适配器
        userAdapter = UserAdapter(userList) { clickedUser ->
            navigateToChatScreenWithUser(clickedUser)
        }

        // 设置用户列表的 RecyclerView
        binding.userRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = userAdapter
        }

        // 从 Firebase 加载用户列表
        loadUsers()
    }

    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        database.getReference("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null && user.uid != currentUserId) {
                            userList.add(user)
                        }
                    }
                    userAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatFragment", "加载用户列表失败", error.toException())
                    Toast.makeText(requireContext(), "加载用户列表失败", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun navigateToChatScreen(chatItem: Chats) {
        val currentUserIdAuth = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserIdAuth.isNullOrBlank()) {
            Log.e("ChatFragment", "用户未登录，无法进入聊天界面")
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val myUser = FirebaseManager.UserManager.getUser(currentUserIdAuth)
            val myAvatarUrl = myUser?.avatarUrl ?: ""

            val args = Bundle().apply {
                putString("otherUserId", chatItem.otherUserId)
                putString("otherUserName", chatItem.name)
                putString("otherUserAvatarUrl", chatItem.otherUserAvatarUrl)
                putString("myAvatarUrl", myAvatarUrl)
            }

            try {
                findNavController().navigate(R.id.action_homeFragment_to_chatScreenFragment, args)
            } catch (e: Exception) {
                Log.e("ChatFragment", "导航失败", e)
                Toast.makeText(context, "打开聊天失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToChatScreenWithUser(user: User) {
        val currentUserIdAuth = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserIdAuth.isNullOrBlank()) {
            Log.e("ChatFragment", "用户未登录，无法进入聊天界面")
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val myUser = FirebaseManager.UserManager.getUser(currentUserIdAuth)
            val myAvatarUrl = myUser?.avatarUrl ?: ""

            val args = Bundle().apply {
                putString("otherUserId", user.uid)
                putString("otherUserName", user.name)
                putString("otherUserAvatarUrl", user.avatarUrl)
                putString("myAvatarUrl", myAvatarUrl)
            }

            try {
                findNavController().navigate(R.id.action_homeFragment_to_chatScreenFragment, args)
            } catch (e: Exception) {
                Log.e("ChatFragment", "导航失败", e)
                Toast.makeText(context, "打开聊天失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}