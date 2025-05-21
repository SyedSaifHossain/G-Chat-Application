package com.syedsaifhossain.g_chatapplication.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.models.Contact

class ContactAdapter(
    private val context: Context,
    private var contactsList: List<Contact>,
    private val onItemClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    private var filteredContactsList: List<Contact> = contactsList

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contactName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = filteredContactsList[position]
        holder.contactName.text = contact.name

        holder.itemView.setOnClickListener {
            onItemClick(contact)
        }
    }

    override fun getItemCount(): Int = filteredContactsList.size

    fun updateData(newContactsList: List<Contact>) {
        contactsList = newContactsList
        filteredContactsList = newContactsList
        notifyDataSetChanged()
    }

    fun filterContacts(query: String): List<Contact> {
        return if (query.isEmpty()) {
            contactsList
        } else {
            contactsList.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
}
