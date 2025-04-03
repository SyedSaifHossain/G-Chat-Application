package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentGroupChatBinding

class GroupChatFragment : Fragment() {

    private lateinit var binding: FragmentGroupChatBinding
    private var groupName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the group name from the arguments using getArguments()
        arguments?.let {
            groupName = it.getString("group_name")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGroupChatBinding.inflate(inflater, container, false)

        // Set the group name to the TextView
        binding.groupNameTextview.text = groupName


        binding.groupchatBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }
}