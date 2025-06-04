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
            val input = binding.etContact.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a phone number or email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 判断输入内容类型
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                // 按邮箱查找
                findUserByEmail(input)
            } else if (android.util.Patterns.PHONE.matcher(input).matches()) {
                // 规范化手机号码格式
                val formattedPhone = formatPhoneNumber(input)
                Log.d("AddContacts", "原始手机号: $input")
                Log.d("AddContacts", "格式化后手机号: $formattedPhone")
                findUserByPhone(formattedPhone)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid phone number or email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
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
                        Log.d("AddContacts", "Found user ID: $userId")
                        // 直接添加为好友
                        addFriend(userId)
                    } else {
                        Log.d("AddContacts", "User ID not found")
                        Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("AddContacts", "No matching user found")
                    Toast.makeText(requireContext(), "No user found with this phone number", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Log.e("AddContacts", "Query failed", error)
                Toast.makeText(requireContext(), "Failed to query user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun findUserByEmail(email: String) {
        val usersRef = database.getReference("users")
        val currentUser = auth.currentUser ?: return

        Log.d("AddContacts", "开始查询用户，邮箱: $email")

        usersRef.orderByChild("email").equalTo(email)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("AddContacts", "查询结果: "+snapshot.exists())
                Log.d("AddContacts", "查询到的数据: "+snapshot.value)
                if (snapshot.exists()) {
                    val userId = snapshot.children.firstOrNull()?.key
                    if (userId != null) {
                        Log.d("AddContacts", "Found user ID: $userId")
                        addFriend(userId)
                    } else {
                        Log.d("AddContacts", "User ID not found")
                        Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("AddContacts", "No matching user found")
                    Toast.makeText(requireContext(), "No user found with this email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Log.e("AddContacts", "Query failed", error)
                Toast.makeText(requireContext(), "Failed to query user: ${error.message}", Toast.LENGTH_SHORT).show()
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
                        Log.d("AddContacts", "Friend added successfully")
                        Toast.makeText(requireContext(), "Friend added successfully", Toast.LENGTH_SHORT).show()
                        // 清空输入框
                        binding.etContact.text?.clear()
                    }
                    .addOnFailureListener { error ->
                        Log.e("AddContacts", "Failed to add friend", error)
                        Toast.makeText(requireContext(), "Failed to add friend: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { error ->
                Log.e("AddContacts", "Failed to add friend", error)
                Toast.makeText(requireContext(), "Failed to add friend: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}