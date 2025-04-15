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


class SelectGroupFragment : Fragment() {
    private lateinit var binding: FragmentSelectGroupBinding
    private lateinit var groupAdapter: GroupAdapter

    private val groupList = arrayListOf(
        GroupItem(R.drawable.cityimg, "Group 1", "Description for group 1"),
        GroupItem(R.drawable.cityimg, "Group 2", "Description for group 2"),
        GroupItem(R.drawable.cityimg, "Group 3", "Description for group 3")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectGroupBinding.inflate(inflater, container, false)

        // Initialize adapter using lambda instead of interface
        groupAdapter = GroupAdapter(groupList) { groupItem ->
            // Navigate to GroupChatFragment when a group is clicked
            val bundle = Bundle().apply {
                putString("group_name", groupItem.title)
            }
            findNavController().navigate(R.id.action_selectGroupFragment_to_groupChatFragment, bundle)
        }

        binding.selectGroupRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupAdapter
        }

        binding.selectGroupBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }
}