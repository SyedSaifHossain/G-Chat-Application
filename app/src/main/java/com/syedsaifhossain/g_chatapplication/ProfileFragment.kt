package com.syedsaifhossain.g_chatapplication

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userId get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserInfo()
        binding.editPhoneBtn.setOnClickListener { showEditPhoneDialog() }
        binding.ivProfileAvatar.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_profileSettingFragment)
        }
        binding.tvProfileName.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_profileSettingFragment)
        }
    }

    private fun loadUserInfo() {
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId ?: return)
        dbRef.get().addOnSuccessListener { snapshot ->
            binding.tvProfileName.text = snapshot.child("name").getValue(String::class.java) ?: ""
            binding.tvProfilePhone.text = snapshot.child("phone").getValue(String::class.java) ?: ""
            val avatarUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                ?: snapshot.child("avatarUrl").getValue(String::class.java)
                ?: ""
            if (avatarUrl.isNotEmpty()) {
                Glide.with(this).load(avatarUrl).placeholder(R.drawable.default_avatar).into(binding.ivProfileAvatar)
            } else {
                binding.ivProfileAvatar.setImageResource(R.drawable.default_avatar)
            }
        }
    }

    private fun showEditPhoneDialog() {
        val editText = EditText(requireContext())
        editText.inputType = InputType.TYPE_CLASS_PHONE
        editText.setText(binding.tvProfilePhone.text)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Phone Number")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newPhone = editText.text.toString().trim()
                if (newPhone.isNotEmpty()) {
                    updatePhoneNumber(newPhone)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePhoneNumber(newPhone: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId ?: return)
        val updates = mapOf("phone" to newPhone)
        dbRef.updateChildren(updates)
            .addOnSuccessListener {
                binding.tvProfilePhone.text = newPhone
                Toast.makeText(requireContext(), "Phone updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update phone", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}