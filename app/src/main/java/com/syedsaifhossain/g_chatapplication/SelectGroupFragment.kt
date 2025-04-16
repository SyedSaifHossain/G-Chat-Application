package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.GroupAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSelectGroupBinding
import com.syedsaifhossain.g_chatapplication.models.GroupItem

class SelectGroupFragment : Fragment(), GroupAdapter.OnItemClickListener {
    private lateinit var binding: FragmentSelectGroupBinding
    private lateinit var groupAdapter: GroupAdapter

    // Sample data for GroupItem
    private val groupList = arrayListOf(
        GroupItem(selectImg = R.drawable.cityimg, title = "Group 1", description = "Description for group 1"),
        GroupItem(selectImg = R.drawable.cityimg, title = "Group 2", description = "Description for group 2"),
        GroupItem(selectImg = R.drawable.cityimg, title = "Group 3", description = "Description for group 3")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectGroupBinding.inflate(inflater, container, false)

        groupAdapter = GroupAdapter(groupList, this) // Passing the listener to the adapter

        binding.selectGroupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        binding.selectGroupBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }

    // Implementing onGroupItemClick method to navigate to GroupChatFragment and pass data using Bundle
    override fun onGroupItemClick(groupItem: GroupItem) {
        findNavController().navigate(R.id.action_selectGroupFragment_to_groupChatFragment)
    }

}