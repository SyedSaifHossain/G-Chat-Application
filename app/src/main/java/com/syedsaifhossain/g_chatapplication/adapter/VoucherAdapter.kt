package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.ItemVoucherBinding
import com.syedsaifhossain.g_chatapplication.models.Voucher

class VoucherAdapter : RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder>() {

    private var vouchers = listOf<Voucher>()

    inner class VoucherViewHolder(private val binding: ItemVoucherBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(voucher: Voucher) {
            binding.apply {
                tvTitle.text = voucher.title
                tvDescription.text = voucher.description
                tvValidDate.text = "Valid until: ${voucher.validUntil}"
                tvStatus.text = voucher.status
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val binding = ItemVoucherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VoucherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(vouchers[position])
    }

    override fun getItemCount() = vouchers.size

    fun submitList(newVouchers: List<Voucher>) {
        vouchers = newVouchers
        notifyDataSetChanged()
    }
}