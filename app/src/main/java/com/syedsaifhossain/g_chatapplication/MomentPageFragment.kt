package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.MomentAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentMomentPageBinding
import com.syedsaifhossain.g_chatapplication.models.Moment

class MomentPageFragment : Fragment() {

    private var _binding: FragmentMomentPageBinding? = null
    private val binding get() = _binding!!

    private val momentList = listOf(
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMomentPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.momentRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = MomentAdapter(momentList)
        }

        binding.momentBackImg.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}