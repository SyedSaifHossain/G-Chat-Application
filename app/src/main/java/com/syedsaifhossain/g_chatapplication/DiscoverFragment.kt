package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout // Import TabLayout
import com.syedsaifhossain.g_chatapplication.adapter.DealsAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentDiscoverBinding
import com.syedsaifhossain.g_chatapplication.models.Deal
import com.syedsaifhossain.g_chatapplication.FavoriteManager

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private lateinit var dealsAdapter: DealsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup UI components
        setupTabs() // Call function to set up tabs
        setupRecyclerView() // Call function to set up RecyclerView
        loadDealsForCategory("All") // Load initial data for the "All" category
    }

    // Sets up the category tabs
    private fun setupTabs() {
        // Make sure to use the correct ID from the layout file: categoryTabs
        binding.categoryTabs.addTab(binding.categoryTabs.newTab().setText("All"))
        binding.categoryTabs.addTab(binding.categoryTabs.newTab().setText("Restaurant"))
        binding.categoryTabs.addTab(binding.categoryTabs.newTab().setText("Hotel"))
        binding.categoryTabs.addTab(binding.categoryTabs.newTab().setText("Beauty"))

        // Add a listener to handle tab selections
        binding.categoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    // Load data based on the selected tab's text
                    loadDealsForCategory(it.text.toString())
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) { /* Do nothing */ }
            override fun onTabReselected(tab: TabLayout.Tab?) { /* Do nothing */ }
        })
    }

    // Sets up the RecyclerView to display deals
    private fun setupRecyclerView() {
        // Initialize the adapter, passing the favorite click handler
        dealsAdapter = DealsAdapter { deal ->
            handleFavoriteClick(deal)
        }
        // Make sure to use the correct RecyclerView ID from the layout file: dealsRecyclerView
        binding.dealsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2) // Use a 2-column grid
            adapter = dealsAdapter // Set the adapter for the RecyclerView
        }
    }

    // Handles clicks on the favorite button within the adapter
    private fun handleFavoriteClick(deal: Deal) {
        // Save the new favorite status using FavoriteManager
        context?.let { ctx ->
            FavoriteManager.setFavorite(ctx, deal.id, deal.isFavorite)

            // Show a confirmation message (optional)
            val message = if (deal.isFavorite) {
                "Added ${deal.title} to favorites"
            } else {
                "Removed ${deal.title} from favorites"
            }
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
            Log.d("DiscoverFragment", "Favorite status saved for ${deal.id}: ${deal.isFavorite}")
        } ?: run {
            // Log an error if the context is unexpectedly null
            Log.e("DiscoverFragment", "Context is null, cannot save favorite status.")
        }
    }

    // Loads deals based on the selected category
    private fun loadDealsForCategory(category: String) {
        Log.d("DiscoverFragment", "Loading deals for category: $category")
        val rawTestData = getRawTestData() // Get the base data

        // Update data with favorite status loaded from SharedPreferences
        val updatedTestData = rawTestData.map { deal ->
            val isFav = context?.let { FavoriteManager.isFavorite(it, deal.id) } ?: false
            deal.copy(isFavorite = isFav) // Create a copy with the updated status
        }

        // Filter the updated data based on the category and submit to the adapter
        // Note: The filter logic needs to match the tab texts
        dealsAdapter.submitList(updatedTestData.filter { deal -> // 在这里添加了 "deal ->"
            category == "All" || deal.title.contains(category, ignoreCase = true)
        })
    }

    // Provides raw test data (without initial favorite state)
    private fun getRawTestData(): List<Deal> {
        // Replace with your actual data source later
        return listOf(
            Deal("1", "Luxury Buffet", "Amazing buffet experience", 199.0, 399.0, "https://via.placeholder.com/300/FF0000/FFFFFF?text=Buffet"),
            Deal("2", "Premium SPA Package", "Relax and rejuvenate", 299.0, 599.0, "https://via.placeholder.com/300/00FF00/FFFFFF?text=SPA"),
            Deal("3", "Seafood Hotpot", "Fresh seafood hotpot", 258.0, 458.0, "https://via.placeholder.com/300/0000FF/FFFFFF?text=Hotpot"),
            Deal("4", "City Center Hotel", "Comfortable stay downtown", 150.0, 250.0, "https://via.placeholder.com/300/FFFF00/000000?text=Hotel"), // Ensure title contains "Hotel" for filtering
            Deal("5", "Beauty Salon Discount", "Discount on beauty treatments", 50.0, 100.0, "https://via.placeholder.com/300/FF00FF/FFFFFF?text=Beauty") // Ensure title contains "Beauty" for filtering
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}