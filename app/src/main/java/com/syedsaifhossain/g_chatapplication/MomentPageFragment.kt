package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnScrollChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.MomentAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentMomentPageBinding
import com.syedsaifhossain.g_chatapplication.models.Moment

class MomentPageFragment : Fragment() {

    private var _binding: FragmentMomentPageBinding? = null
    private var scrollListener: OnScrollChangedListener? = null

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
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding?.apply {
            momentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            momentRecyclerView.adapter = MomentAdapter(momentList)

            momentBackImg.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            // Hide initially
            momentTitle.visibility = View.GONE
            momentMoreBtn.visibility = View.GONE

            scrollListener = OnScrollChangedListener {
                val scrollY = momentScrollView.scrollY

                if (scrollY > 100 && momentTitle.visibility == View.GONE) {
                    momentHeaderLayout.setBackgroundColor(android.graphics.Color.parseColor("#000000"))
                    momentTitle.visibility = View.VISIBLE
                    momentMoreBtn.visibility = View.VISIBLE
                    momentTitle.alpha = 0f
                    momentMoreBtn.alpha = 0f
                    momentTitle.animate().alpha(1f).setDuration(200).start()
                    momentMoreBtn.animate().alpha(1f).setDuration(200).start()
                } else if (scrollY <= 100 && momentTitle.visibility == View.VISIBLE) {
                    momentTitle.animate().alpha(0f).setDuration(200).withEndAction {
                        momentTitle.visibility = View.GONE
                    }.start()
                    momentMoreBtn.animate().alpha(0f).setDuration(200).withEndAction {
                        momentMoreBtn.visibility = View.GONE
                    }.start()

                    // Reset background to transparent
                    momentHeaderLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }

            momentScrollView.viewTreeObserver.addOnScrollChangedListener(scrollListener)
        }
    }

    override fun onDestroyView() {
        _binding?.momentScrollView?.viewTreeObserver?.removeOnScrollChangedListener(scrollListener)
        scrollListener = null
        _binding = null
        super.onDestroyView()
    }
}