package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.MessagesAdapter
import com.syedsaifhossain.g_chatapplication.api.RetrofitInstance
import com.syedsaifhossain.g_chatapplication.databinding.FragmentGroupChatBinding
import com.syedsaifhossain.g_chatapplication.models.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GroupChatFragment : Fragment() {

    private lateinit var binding: FragmentGroupChatBinding
    private var groupName: String? = null
    private val messagesList = mutableListOf<Message>()
    private lateinit var messagesAdapter: MessagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the group name from the arguments
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

        // Setup RecyclerView and adapter for displaying messages
        setupRecyclerView()

        // Listen for changes in the EditText
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Optionally, you could update the TextView immediately as the text changes
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed here
            }
        })

        // Handle when the user presses the "Enter" key (submit the message)
        binding.messageInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                val messageContent = binding.messageInput.text.toString()

                if (messageContent.isNotEmpty()) {
                    // Create a Message object
                    val message = Message(
                        groupName = groupName ?: "Unknown Group", // Add the group name from arguments
                        sender = "User", // Replace with actual user info
                        content = messageContent
                    )

                    // Add the message to the RecyclerView using the adapter
                    messagesAdapter.addMessage(message)

                    // Scroll to the latest message
                    binding.groupChatRecyclerView.scrollToPosition(messagesList.size - 1)

                    // Send the message using Retrofit
                    sendMessageToWebhook(message)

                    // Clear the EditText after submitting
                    binding.messageInput.text.clear()

                    // Optionally hide the keyboard
                    hideKeyboard()
                }

                true // Indicate that the event was handled
            }
            false // Return false if you don't want to handle other actions
        }

        // Set back button listener
        binding.groupchatBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        // Setup the RecyclerView to display the chat messages
        messagesAdapter = MessagesAdapter(messagesList)

        binding.groupChatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messagesAdapter
        }
    }

    private fun sendMessageToWebhook(message: Message) {
        // Make the network request in a background thread using Kotlin Coroutines
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.sendMessage(message)

                // Check if the response was successful
                if (response.isSuccessful) {
                    // Once the message is sent successfully, notify the user on the main thread
                    withContext(Dispatchers.Main) {
                        Log.d("Webhook", "Message sent successfully!")
                    }
                } else {
                    // If sending the message fails, log the error
                    withContext(Dispatchers.Main) {
                        Log.e("Webhook", "Failed to send message. Response code: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                // Catch any exceptions, such as network errors
                withContext(Dispatchers.Main) {
                    Log.e("Webhook", "Error: ${e.message}")
                }
            }
        }
    }

    // Utility function to hide the keyboard
    private fun hideKeyboard() {
        val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
    }

    companion object {

        @JvmStatic
        fun newInstance(groupName: String) = GroupChatFragment().apply {
            arguments = Bundle().apply {
                putString("group_name", groupName)
            }
        }
    }
}