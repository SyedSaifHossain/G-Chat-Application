package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.GroupAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSelectGroupBinding
import com.syedsaifhossain.g_chatapplication.models.GroupItem

class SelectGroupFragment : Fragment() {
    private lateinit var binding: FragmentSelectGroupBinding

    private lateinit var groupAdapter: GroupAdapter

    // Sample data for GroupItem
    private val groupList = listOf(
        GroupItem(selectImg = R.drawable.cityimg, title = "Group 1", description = "Description for group 1"),
        GroupItem(selectImg = R.drawable.cityimg, title = "Group 2", description = "Description for group 2"),
        GroupItem(selectImg = R.drawable.cityimg, title = "Group 3", description = "Description for group 3")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for the fragment using ViewBinding
        binding = FragmentSelectGroupBinding.inflate(inflater, container, false)

        groupAdapter = GroupAdapter(groupList)

        binding.selectGroupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
binding.selectGroupBackImg.setOnClickListener{
    findNavController().popBackStack()
}
        return binding.root
    }

}