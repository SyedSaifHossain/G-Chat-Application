package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.databinding.FragmentAddContactsBinding

class AddContactsFragment : Fragment() {
    private var _binding: FragmentAddContactsBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

        binding.btnSaveContact.setOnClickListener {
            val phoneNumber = binding.etPhone.text.toString().trim()
            if (phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "请输入手机号码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 规范化手机号码格式
            val formattedPhone = formatPhoneNumber(phoneNumber)
            Log.d("AddContacts", "原始手机号: $phoneNumber")
            Log.d("AddContacts", "格式化后手机号: $formattedPhone")

            // 根据手机号码查找用户
            findUserByPhone(formattedPhone)
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        // 移除所有非数字字符
        val digitsOnly = phone.replace(Regex("[^0-9+]"), "")
        
        // 如果号码以+开头，保留+号
        return if (digitsOnly.startsWith("+")) {
            digitsOnly
        } else {
            // 如果没有+号，添加+号
            "+$digitsOnly"
        }
    }

    private fun findUserByPhone(phoneNumber: String) {
        val usersRef = database.getReference("users")
        val currentUser = auth.currentUser ?: return

        Log.d("AddContacts", "开始查询用户，手机号: $phoneNumber")

        // 查询用户
        usersRef.orderByChild("phone").equalTo(phoneNumber)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("AddContacts", "查询结果: ${snapshot.exists()}")
                Log.d("AddContacts", "查询到的数据: ${snapshot.value}")
                
                if (snapshot.exists()) {
                    // 找到用户，获取第一个匹配的用户ID
                    val userId = snapshot.children.firstOrNull()?.key
                    if (userId != null) {
                        Log.d("AddContacts", "找到用户ID: $userId")
                        // 直接添加为好友
                        addFriend(userId)
                    } else {
                        Log.d("AddContacts", "未找到用户ID")
                        Toast.makeText(requireContext(), "未找到用户", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("AddContacts", "未找到匹配的用户")
                    Toast.makeText(requireContext(), "未找到该手机号码的用户", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Log.e("AddContacts", "查询失败", error)
                Toast.makeText(requireContext(), "查询用户失败: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addFriend(userId: String) {
        val currentUser = auth.currentUser ?: return
        val usersRef = database.getReference("users")

        Log.d("AddContacts", "开始添加好友，目标用户ID: $userId")

        // 在双方的好友列表中添加对方
        usersRef.child(currentUser.uid).child("friends").child(userId).setValue(true)
            .addOnSuccessListener {
                usersRef.child(userId).child("friends").child(currentUser.uid).setValue(true)
                    .addOnSuccessListener {
                        Log.d("AddContacts", "添加好友成功")
                        Toast.makeText(requireContext(), "添加好友成功", Toast.LENGTH_SHORT).show()
                        // 清空输入框
                        binding.etPhone.text?.clear()
                        binding.etFullName.text?.clear()
                        binding.etEmail.text?.clear()
                    }
                    .addOnFailureListener { error ->
                        Log.e("AddContacts", "添加好友失败", error)
                        Toast.makeText(requireContext(), "添加好友失败: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { error ->
                Log.e("AddContacts", "添加好友失败", error)
                Toast.makeText(requireContext(), "添加好友失败: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}