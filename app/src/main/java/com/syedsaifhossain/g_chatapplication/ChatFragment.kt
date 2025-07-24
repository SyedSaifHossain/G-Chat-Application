package com.syedsaifhossain.g_chatapplication

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.syedsaifhossain.g_chatapplication.adapter.ChatAdapter
import com.syedsaifhossain.g_chatapplication.api.FirestoreManager
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
import com.syedsaifhossain.g_chatapplication.models.Chats
import com.syedsaifhossain.g_chatapplication.models.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: ArrayList<Chats>

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setup chat and user lists
        setupChatList()
      //  setupUserList()

        // Setup add button popup menu
        binding.addButton.setOnClickListener {
            showPopupMenu(it)
        }

        return view
    }

    private fun setupChatList() {
        recyclerView = binding.chatRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        messageList = arrayListOf()

        chatAdapter = ChatAdapter(messageList) { clickedChatItem ->
            if (clickedChatItem.isGroup) {
                val bundle = Bundle().apply { putString("groupId", clickedChatItem.otherUserId) }
                findNavController().navigate(R.id.groupChatFragment, bundle)
            } else {
                navigateToChatScreen(clickedChatItem)
            }
        }
        recyclerView.adapter = chatAdapter

        // 🔁 Attach swipe-to-delete
        val swipeToDeleteCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val chat = messageList[position]

                // 🧹 Remove from Firestore & adapter
                deleteChat(chat)
                messageList.removeAt(position)
                chatAdapter.notifyItemRemoved(position)
            }
        }

        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(recyclerView)

        val currentUserId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val userDoc = firestore.collection("users").document(currentUserId).get().await()
                val friendsData = userDoc.get("friends") as? Map<String, Boolean> ?: emptyMap()
                val friendUidSet = friendsData.keys.toSet()

                val users = FirestoreManager.UserManager.getUsersByFriendIds(friendUidSet.toList())
                messageList.clear()
                for (user in users) {
                    val chat = Chats(
                        imageRes = if (user.avatarUrl.isNullOrBlank()) R.drawable.profilenew else 0,
                        name = user.name,
                        message = user.status,
                        otherUserId = user.uid,
                        otherUserAvatarUrl = user.avatarUrl,
                        isGroup = false,
                        senderId = currentUserId,
                        receiverId = user.uid,
                        lastMessageTime = user.lastSeen,
                        lastMessageSenderId = ""
                    )
                    messageList.add(chat)
                }

                loadGroupChats(currentUserId)

            } catch (e: Exception) {
                Log.e("ChatFragment", "Failed to load friends", e)
                loadGroupChats(currentUserId)
            }
        }
    }

    private fun deleteChat(chat: Chats) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Skip group chats for now
        if (chat.isGroup) {
            Toast.makeText(requireContext(), "Group delete coming soon", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users")
            .document(currentUserId)
            .update("friends.${chat.otherUserId}", null)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete chat", Toast.LENGTH_SHORT).show()
            }
    }

    // 新增：加载群聊
    private fun loadGroupChats(currentUserId: String) {
        lifecycleScope.launch {
            try {
                Log.d("ChatFragment", "开始加载群聊，用户ID: $currentUserId")
                val groups = FirestoreManager.GroupManager.getUserGroups(currentUserId)
                Log.d("ChatFragment", "获取到 ${groups.size} 个群组")
                
                for (groupData in groups) {
                    val groupId = groupData["groupId"] as? String ?: continue
                    val groupName = groupData["name"] as? String ?: "Group"
                    Log.d("ChatFragment", "处理群组: $groupId ($groupName)")
                    
                    // 用 Chats 数据类展示群聊，otherUserId 用 groupId 并加前缀区分
                    val chat = Chats(
                        imageRes = R.drawable.groupiconnew,
                        name = groupName,
                        message = "Group Chat",
                        otherUserId = groupId,
                        otherUserAvatarUrl = "",
                        isGroup = true,
                        senderId = currentUserId,
                        receiverId = groupId,
                        lastMessageTime = 0L,
                        lastMessageSenderId = ""
                    )
                    messageList.add(chat)
                    Log.d("ChatFragment", "添加群聊到列表: $groupName")
                }
                
                Log.d("ChatFragment", "群聊加载完成，总消息数: ${messageList.size}")
                chatAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                if (e.message?.contains("Job was cancelled") == true || e is kotlinx.coroutines.CancellationException) {
                    Log.d("ChatFragment", "群聊加载被取消，这是正常的")
                } else {
                    Log.e("ChatFragment", "Failed to load groups", e)
                }
            }
        }
    }

    private fun navigateToChatScreen(chatItem: Chats) {
        val currentUserIdAuth = auth.currentUser?.uid
        if (currentUserIdAuth.isNullOrBlank()) {
            Log.e("ChatFragment", "用户未登录，无法进入聊天界面")
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val myUser = FirestoreManager.UserManager.getUser(currentUserIdAuth)
            val myAvatarUrl = myUser?.avatarUrl ?: ""

            val args = Bundle().apply {
                putString("otherUserId", chatItem.otherUserId)
                putString("otherUserName", chatItem.name)
                putString("otherUserAvatarUrl", chatItem.otherUserAvatarUrl)
                putString("myAvatarUrl", myAvatarUrl)
            }

            try {
                findNavController().navigate(R.id.chatScreenFragment, args)
            } catch (e: Exception) {
                Log.e("ChatFragment", "导航失败", e)
                Toast.makeText(context, "打开聊天失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToChatScreenWithUser(user: User) {
        val currentUserIdAuth = auth.currentUser?.uid
        if (currentUserIdAuth.isNullOrBlank()) {
            Log.e("ChatFragment", "用户未登录，无法进入聊天界面")
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val myUser = FirestoreManager.UserManager.getUser(currentUserIdAuth)
            val myAvatarUrl = myUser?.avatarUrl ?: ""

            val args = Bundle().apply {
                putString("otherUserId", user.uid)
                putString("otherUserName", user.name)
                putString("otherUserAvatarUrl", user.avatarUrl)
                putString("myAvatarUrl", myAvatarUrl)
            }

            try {
                findNavController().navigate(R.id.chatScreenFragment, args)
            } catch (e: Exception) {
                Log.e("ChatFragment", "导航失败", e)
                Toast.makeText(context, "打开聊天失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPopupMenu(view: View) {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.layout_custom_popup_menu, null)

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 10f

        // 设置点击事件
        popupView.findViewById<LinearLayout>(R.id.item_new_chats).setOnClickListener {
            popupWindow.dismiss()
            findNavController().navigate(R.id.newChatsFragment)
        }
        popupView.findViewById<LinearLayout>(R.id.item_add_contacts).setOnClickListener {
            popupWindow.dismiss()
            findNavController().navigate(R.id.action_homeFragment_to_addContactsFragment)
        }
        popupView.findViewById<LinearLayout>(R.id.item_scan).setOnClickListener {
            popupWindow.dismiss()
            findNavController().navigate(R.id.action_homeFragment_to_scanFragment)
        }
        // 显示在按钮下方
        popupWindow.showAsDropDown(view, 0, 0)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}