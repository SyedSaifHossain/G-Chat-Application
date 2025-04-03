package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.NewChatAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentNewChatsBinding
import com.syedsaifhossain.g_chatapplication.models.NewChatItem

class NewChatsFragment : Fragment() {

    private lateinit var binding: FragmentNewChatsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNewChatsBinding.inflate(inflater, container, false)
        // Prepare some dummy data for the adapter
        val chatList = ArrayList<NewChatItem>().apply {

            add(NewChatItem("Friend 1", R.drawable.cityimg))  // Assuming you have avatar images
        }

        // Set up the RecyclerView
        val adapter = NewChatAdapter(chatList)  // Make sure your adapter is correctly set
        binding.friendsList.layoutManager = LinearLayoutManager(requireContext())
        binding.friendsList.adapter = adapter

binding.selectGroupText.setOnClickListener{
    findNavController().navigate(R.id.action_newChatsFragment_to_selectGroupFragment)
}
        return binding.root
    }

}