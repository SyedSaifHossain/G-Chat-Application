package com.syedsaifhossain.g_chatapplication.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.models.Contact
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ContactAdapter(
    private val context: Context,
    contactsList: List<Contact>,
    private val onItemClick: (Contact) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allContactsList: List<Contact> = contactsList // 全量数据
    private var filteredContactsList: List<Contact> = contactsList

    companion object {
        private const val VIEW_TYPE_GROUP = 1
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_SECTION = 2
    }

    // 普通联系人ViewHolder
    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contactName)
        val contactImg: android.widget.ImageView = itemView.findViewById(R.id.contactItemImg)
    }
    // 分组入口ViewHolder
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupIcon: android.widget.ImageView = itemView.findViewById(R.id.groupIcon)
        val groupName: TextView = itemView.findViewById(R.id.groupName)
    }

    // Section Header ViewHolder
    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
    }

    override fun getItemViewType(position: Int): Int {
        return when (filteredContactsList[position].type) {
            Contact.TYPE_NEW_FRIENDS, Contact.TYPE_GROUP_CHATS -> VIEW_TYPE_GROUP
            Contact.TYPE_SECTION -> VIEW_TYPE_SECTION
            else -> VIEW_TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GROUP -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_contact_group, parent, false)
                GroupViewHolder(view)
            }
            VIEW_TYPE_SECTION -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_contact_section, parent, false)
                SectionViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false)
                ContactViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val contact = filteredContactsList[position]
        when (holder) {
            is GroupViewHolder -> {
                holder.groupName.text = contact.name
                when (contact.type) {
                    Contact.TYPE_NEW_FRIENDS -> holder.groupIcon.setImageResource(R.drawable.addcontact)
                    Contact.TYPE_GROUP_CHATS -> holder.groupIcon.setImageResource(R.drawable.addcontacticon)
                }
                holder.itemView.setOnClickListener { onItemClick(contact) }
            }
            is SectionViewHolder -> {
                holder.sectionTitle.text = contact.name
            }
            is ContactViewHolder -> {
                // 实时从users节点获取头像和名字
                val usersRef = FirebaseDatabase.getInstance().getReference("users")
                usersRef.child(contact.id).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: contact.name
                        val avatarUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                            ?: snapshot.child("avatarUrl").getValue(String::class.java)
                            ?: ""
                        holder.contactName.text = name
                        Glide.with(holder.itemView.context)
                            .load(avatarUrl)
                            .placeholder(R.drawable.default_avatar)
                            .transform(RoundedCorners(dpToPx(16, holder.itemView)))
                            .into(holder.contactImg)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
                holder.itemView.setOnClickListener { onItemClick(contact) }
            }
        }
    }

    override fun getItemCount(): Int = filteredContactsList.size

    fun updateData(newContactsList: List<Contact>) {
        allContactsList = newContactsList
        filteredContactsList = groupAndSortContacts(newContactsList)
        notifyDataSetChanged()
    }

    fun filterContacts(query: String) {
        val filtered = if (query.isEmpty()) {
            allContactsList
        } else {
            allContactsList.filter { it.name.contains(query, ignoreCase = true) || it.type != Contact.TYPE_NORMAL }
        }
        filteredContactsList = groupAndSortContacts(filtered)
        notifyDataSetChanged()
    }

    // 分组排序并插入Section Header
    private fun groupAndSortContacts(list: List<Contact>): List<Contact> {
        val result = mutableListOf<Contact>()
        // 保留前面的分组入口
        val special = list.filter { it.type != Contact.TYPE_NORMAL }
        result.addAll(special)
        // 普通联系人分组
        val normalContacts = list.filter { it.type == Contact.TYPE_NORMAL }
        val grouped = normalContacts.groupBy {
            val c = it.name.firstOrNull()?.uppercaseChar() ?: '#'
            if (c in 'A'..'Z') c else '#'
        }
        val azKeys = grouped.keys.filter { it != '#' }.sorted()
        val otherKey = grouped.keys.filter { it == '#' }
        // 先A-Z分组
        for (key in azKeys) {
            result.add(Contact(name = key.toString(), type = Contact.TYPE_SECTION))
            result.addAll(grouped[key]!!.sortedBy { it.name.uppercase() })
        }
        // 最后特殊符号分组
        for (key in otherKey) {
            result.add(Contact(name = key.toString(), type = Contact.TYPE_SECTION))
            result.addAll(grouped[key]!!.sortedBy { it.name.uppercase() })
        }
        return result
    }

    // dp转px工具
    private fun dpToPx(dp: Int, view: View): Int {
        val density = view.context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
