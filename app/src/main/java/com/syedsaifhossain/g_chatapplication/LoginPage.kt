package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLoginPageBinding

class LoginPage : Fragment() {

    private var _binding: FragmentLoginPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().getReference("Users")

        binding.loginButton.setOnClickListener {
            val phone = binding.phoneLoginEdt.text.toString().trim()
            val password = binding.passwordLoginEdt.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter phone and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(phone, password)
        }

        binding.backToSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginPage_to_signupPageFragment)
        }
    }

    private fun loginUser(phone: String, password: String) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false
                for (userSnapshot in snapshot.children) {
                    val dbPhone = userSnapshot.child("phone").getValue(String::class.java)
                    val dbPassword = userSnapshot.child("password").getValue(String::class.java)

                    if (dbPhone == phone && dbPassword == password) {
                        found = true
                        Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginPage_to_homeFragment)
                        break
                    }
                }

                if (!found) {
                    Toast.makeText(requireContext(), "Invalid phone or password", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}