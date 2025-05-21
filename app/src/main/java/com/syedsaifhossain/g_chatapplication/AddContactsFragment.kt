package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.databinding.FragmentAddContactsBinding
import com.syedsaifhossain.g_chatapplication.managers.FriendManager

class AddContactsFragment : Fragment() {
    private var _binding: FragmentAddContactsBinding? = null
    private val binding get() = _binding!!
    private lateinit var friendManager: FriendManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        friendManager = FriendManager()

        binding.btnSaveContact.setOnClickListener {
            val phoneNumber = binding.etPhone.text.toString().trim()
            if (phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "请输入手机号码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 根据手机号码查找用户
            findUserByPhone(phoneNumber)
        }
    }

    private fun findUserByPhone(phoneNumber: String) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        // 查询用户
        usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // 找到用户，获取第一个匹配的用户ID
                    val userId = snapshot.children.firstOrNull()?.key
                    if (userId != null) {
                        // 发送好友请求
                        sendFriendRequest(userId)
                    } else {
                        Toast.makeText(requireContext(), "未找到用户", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "未找到该手机号码的用户", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "查询用户失败", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendFriendRequest(userId: String) {
        friendManager.sendFriendRequest(userId) { success, message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            if (success) {
                // 清空输入框
                binding.etPhone.text?.clear()
                binding.etFullName.text?.clear()
                binding.etEmail.text?.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}