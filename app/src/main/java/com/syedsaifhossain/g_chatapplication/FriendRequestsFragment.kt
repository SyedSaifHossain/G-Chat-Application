package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapters.FriendRequestAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentFriendRequestsBinding
import com.syedsaifhossain.g_chatapplication.managers.FriendManager

class FriendRequestsFragment : Fragment() {
    private lateinit var binding: FragmentFriendRequestsBinding
    private lateinit var friendManager: FriendManager
    private lateinit var adapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        friendManager = FriendManager()
        setupRecyclerView()
        loadFriendRequests()
    }

    private fun setupRecyclerView() {
        adapter = FriendRequestAdapter(emptyList(), friendManager)
        binding.friendRequestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FriendRequestsFragment.adapter
        }
    }

    private fun loadFriendRequests() {
        friendManager.getPendingFriendRequests { requests ->
            adapter.updateRequests(requests)
        }
    }
} 