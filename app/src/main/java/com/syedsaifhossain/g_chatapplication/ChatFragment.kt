package com.syedsaifhossain.g_chatapplication

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.adapter.ChatAdapter
import com.syedsaifhossain.g_chatapplication.api.FirebaseManager
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatBinding
import com.syedsaifhossain.g_chatapplication.models.Chats
import com.syedsaifhossain.g_chatapplication.models.User
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: ArrayList<Chats>

    private val database = FirebaseDatabase.getInstance()
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
        chatAdapter = ChatAdapter(
            messageList,
            onItemClick = { clickedChatItem ->
                if (clickedChatItem.isGroup) {
                    val bundle = Bundle().apply { putString("groupId", clickedChatItem.otherUserId) }
                    findNavController().navigate(R.id.groupChatFragment, bundle)
                } else {
                    navigateToChatScreen(clickedChatItem)
                }
            },
            onItemLongClick = { chatItem, view ->
                showChatDeletePopup(chatItem, view)
            }
        )


        recyclerView.adapter = chatAdapter

        val currentUserId = auth.currentUser?.uid ?: return

        // 1. 获取当前用户的好友UID列表
        database.getReference("users").child(currentUserId).child("friends")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(friendsSnapshot: DataSnapshot) {
                    val friendUidSet = mutableSetOf<String>()
                    for (friendSnap in friendsSnapshot.children) {
                        val friendUid = friendSnap.key
                        if (!friendUid.isNullOrBlank()) {
                            friendUidSet.add(friendUid)
                        }
                    }
                    // 2. 获取所有好友的详细信息
                    database.getReference("users").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            messageList.clear()
                            for (userSnap in userSnapshot.children) {
                                val user = userSnap.getValue(User::class.java)
                                if (user != null && friendUidSet.contains(user.uid)) {
                                    // 构造 Chats 对象，显示好友昵称和头像
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
                            }
                            // 加载群聊
                            loadGroupChats(currentUserId)
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 新增：加载群聊
    private fun loadGroupChats(currentUserId: String) {
        database.getReference("groups").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (groupSnap in snapshot.children) {
                    val members = groupSnap.child("members").value as? Map<String, Boolean>
                    if (members?.containsKey(currentUserId) == true) {
                        val groupId = groupSnap.key ?: continue
                        val groupName = groupSnap.child("name").getValue(String::class.java) ?: "Group"
                        // 用 Chats 数据类展示群聊，otherUserId 用 groupId 并加前缀区分
                        val chat = Chats(
                            imageRes = R.drawable.addcontacticon,
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
                    }
                }
                chatAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun navigateToChatScreen(chatItem: Chats) {
        val currentUserIdAuth = auth.currentUser?.uid
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
            val myUser = FirebaseManager.UserManager.getUser(currentUserIdAuth)
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

    private fun showChatDeletePopup(chatItem: Chats, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Delete").setOnMenuItemClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnMenuItemClickListener true
            val otherUserId = chatItem.otherUserId

            val chatId1 = "${currentUserId}_$otherUserId"
            val chatId2 = "${otherUserId}_$currentUserId"

            val chatRef1 = database.getReference("chats").child(chatId1)
            val chatRef2 = database.getReference("chats").child(chatId2)

            // Try to delete both possible chat IDs (safe way)
            chatRef1.removeValue()
            chatRef2.removeValue()

            // ✅ Remove from UI
            val index = messageList.indexOf(chatItem)
            if (index != -1) {
                messageList.removeAt(index)
                chatAdapter.notifyItemRemoved(index)
            }

            Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show()
            true
        }
        popup.show()

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}