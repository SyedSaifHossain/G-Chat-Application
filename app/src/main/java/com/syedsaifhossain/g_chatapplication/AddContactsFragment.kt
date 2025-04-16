package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.databinding.FragmentAddContactsBinding
import com.syedsaifhossain.g_chatapplication.models.Contact
import java.util.UUID

class AddContactsFragment : Fragment() {

    private var _binding: FragmentAddContactsBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase reference
        databaseRef = FirebaseDatabase.getInstance().getReference("Contacts")

        binding.btnSaveContact.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Name & Phone are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val contactId = databaseRef.push().key ?: UUID.randomUUID().toString()

            val contact = Contact(
                id = contactId,
                name = name,
                phone = phone,
                email = email
            )

            databaseRef.child(contactId).setValue(contact)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Contact Saved!", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearFields() {
        binding.etFullName.text?.clear()
        binding.etPhone.text?.clear()
        binding.etEmail.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}