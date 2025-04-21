package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.ContactAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentContactBinding

class ContactFragment : Fragment() {

    private lateinit var binding: FragmentContactBinding

    private lateinit var contactAdapter: ContactAdapter
    private var contactsList: List<String> = listOf(
        "John Doe", "Jane Smith", "Alice Johnson", "Bob Brown", "Charlie Davis"
    ) // Example list, replace with actual data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContactBinding.inflate(inflater, container, false)
        val view = binding.root

        // Set up RecyclerView
        setupRecyclerView()

        // Handle search input using EditText
        binding.contactSearchbar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
                // Filter contacts based on search query
                val filteredContacts = contactAdapter.filterContacts(charSequence.toString())
                contactAdapter.updateData(filteredContacts)
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        return view
    }

    private fun setupRecyclerView() {
        binding.contactRecyclerView.layoutManager = LinearLayoutManager(context)
        contactAdapter = ContactAdapter(requireContext(), contactsList)
        binding.contactRecyclerView.adapter = contactAdapter
    }

}