package com.syedsaifhossain.g_chatapplication.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.R

class ContactAdapter(private val context: Context, private var contactsList: List<String>) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    private var filteredContactsList: List<String> = contactsList

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contactName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        // Bind the contact name to the TextView
        holder.contactName.text = filteredContactsList[position]
    }

    override fun getItemCount(): Int {
        return filteredContactsList.size
    }

    // Method to update the contact list dynamically
    fun updateData(newContactsList: List<String>) {
        contactsList = newContactsList
        filteredContactsList = newContactsList
        notifyDataSetChanged()
    }

    // Method to filter contacts based on search query
    fun filterContacts(query: String): List<String> {
        return if (query.isEmpty()) {
            contactsList
        } else {
            contactsList.filter { it.contains(query, ignoreCase = true) }
        }
    }
}