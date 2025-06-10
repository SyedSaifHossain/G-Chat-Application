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

            // Determine input type
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                // Search by email
                findUserByEmail(input)
            } else if (android.util.Patterns.PHONE.matcher(input).matches()) {
                // Normalize phone number format
                val formattedPhone = formatPhoneNumber(input)
                Log.d("AddContacts", "Original phone: $input")
                Log.d("AddContacts", "Formatted phone: $formattedPhone")
                findUserByPhone(formattedPhone)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid phone number or email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        // Remove all non-digit characters
        val digitsOnly = phone.replace(Regex("[^0-9+]"), "")
        
        // If the number starts with +, keep it
        return if (digitsOnly.startsWith("+")) {
            digitsOnly
        } else {
            // If no +, add it
            "+$digitsOnly"
        }
    }

    private fun findUserByPhone(phoneNumber: String) {
        val usersRef = database.getReference("users")
        val currentUser = auth.currentUser ?: return

        Log.d("AddContacts", "Start querying user, phone: $phoneNumber")

        // Query user
        usersRef.orderByChild("phone").equalTo(phoneNumber)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("AddContacts", "Query result: ${snapshot.exists()}")
                Log.d("AddContacts", "Data retrieved: ${snapshot.value}")
                
                if (snapshot.exists()) {
                    // User found, get the first matching user ID
                    val userId = snapshot.children.firstOrNull()?.key
                    if (userId != null) {
                        Log.d("AddContacts", "Found user ID: $userId")
                        // Add as friend directly
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

        Log.d("AddContacts", "Start querying user, email: $email")

        usersRef.orderByChild("email").equalTo(email)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("AddContacts", "Query result: "+snapshot.exists())
                Log.d("AddContacts", "Data retrieved: "+snapshot.value)
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

        Log.d("AddContacts", "Start adding friend, target user ID: $userId")

        // Add each other to each other's friend list
        usersRef.child(currentUser.uid).child("friends").child(userId).setValue(true)
            .addOnSuccessListener {
                usersRef.child(userId).child("friends").child(currentUser.uid).setValue(true)
                    .addOnSuccessListener {
                        Log.d("AddContacts", "Friend added successfully")
                        Toast.makeText(requireContext(), "Friend added successfully", Toast.LENGTH_SHORT).show()
                        // Clear input field
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