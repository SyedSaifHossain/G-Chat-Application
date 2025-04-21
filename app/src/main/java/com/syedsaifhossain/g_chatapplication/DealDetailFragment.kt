package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.databinding.FragmentDealDetailBinding
import com.syedsaifhossain.g_chatapplication.models.Deal

class DealDetailFragment : Fragment() {
    private var _binding: FragmentDealDetailBinding? = null
    private val binding get() = _binding!!
    private var deal: Deal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 获取传递过来的商品数据
        deal = arguments?.getParcelable("deal")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDealDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        deal?.let { deal ->
            binding.apply {
                titleText.text = deal.title
                descriptionText.text = deal.description
                priceText.text = "$${deal.price}"
                originalPriceText.text = "$${deal.originalPrice}"

                // 加载图片
                Glide.with(requireContext())
                    .load(deal.imageUrl)
                    .into(dealImage)

                // 购买按钮点击事件
                buyButton.setOnClickListener {
                    Toast.makeText(context, "Purchase feature coming soon!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(deal: Deal) = DealDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable("deal", deal)
            }
        }
    }
}