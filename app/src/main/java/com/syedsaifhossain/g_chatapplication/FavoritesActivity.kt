package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager // Added import
// Make sure these imports exist or add them
import com.syedsaifhossain.g_chatapplication.adapter.DealsAdapter
import com.syedsaifhossain.g_chatapplication.databinding.ActivityFavoritesBinding
import com.syedsaifhossain.g_chatapplication.models.Deal
// Added missing import for View
import android.view.View
// Added missing import for Log
import android.util.Log
// Added missing import for Toast
import android.widget.Toast
// Added missing import for ArrayList
import java.util.ArrayList


// Step 1: Ensure inheritance from BaseActivity
class FavoritesActivity : BaseActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    // Step 1: Declare adapter variable
    private lateinit var favoritesAdapter: DealsAdapter
    // Step 1: Declare list to hold favorite deals
    private var currentFavoriteDeals: MutableList<Deal> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Consider removing or adjusting if causing issues with layout
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Call setup functions
        setupActionBar()       // Implemented in Step 2
        setupRecyclerView()    // Implemented in Step 3
        loadFavorites()        // Implemented in Step 4 (initial load)
    }

    // Step 2: Implement setupActionBar (Already done)
    private fun setupActionBar() {
        supportActionBar?.apply {
            // Use a string resource for the title
            // Make sure R.string.favorites_title exists in your strings.xml files!
            title = getString(R.string.favorites_title)
            // Show the back arrow (Up button)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    // Step 3: Implement setupRecyclerView (Already done)
    private fun setupRecyclerView() {
        // Initialize the adapter.
        // The lambda function passed here will be called when the heart icon
        // inside the adapter's item view is clicked.
        favoritesAdapter = DealsAdapter { deal ->
            // Call the method that will handle un-favoriting
            handleFavoriteClick(deal) // Implemented in Step 6
        }

        // Get the RecyclerView from the binding and configure it
        binding.recyclerView.apply {
            // Set the layout manager (how items are arranged)
            // Use LinearLayoutManager for a standard vertical list
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
            // Set the adapter we just created
            adapter = favoritesAdapter
        }
    }

    // Step 4: Implement loadFavorites (Already done)
    private fun loadFavorites() {
        // 1. Get all favorite IDs from SharedPreferences using FavoriteManager
        val favoriteIds = FavoriteManager.getAllFavoriteIds(this)
        Log.d("FavoritesActivity", "Loaded favorite IDs: $favoriteIds") // Log for debugging

        // 2. Get the full list of possible deals (using placeholder data for now)
        val allDeals = getRawTestData() // Get all possible deals

        // 3. Filter the full list to get only the deals whose IDs are in the favoriteIds set
        //    We also use map to ensure the isFavorite property is true for UI consistency here.
        currentFavoriteDeals = allDeals.filter { deal ->
            favoriteIds.contains(deal.id)
        }.map { deal ->
            deal.copy(isFavorite = true) // Ensure isFavorite is true for adapter display
        }.toMutableList() // Convert to a mutable list

        Log.d("FavoritesActivity", "Filtered favorite deals count: ${currentFavoriteDeals.size}") // Log count

        // 4. Submit the filtered list to the adapter
        //    Use ArrayList to ensure a new list is submitted for diffing
        favoritesAdapter.submitList(ArrayList(currentFavoriteDeals))

        // 5. Update the visibility of the empty state view / recyclerview
        updateEmptyStateVisibility() // Implemented in Step 5
    }

    // Step 5: Implement updateEmptyStateVisibility (This is new compared to previous step)
    private fun updateEmptyStateVisibility() {
        // Check if the list of favorite deals is empty
        if (currentFavoriteDeals.isEmpty()) {
            // If empty: Hide the RecyclerView
            binding.recyclerView.visibility = View.GONE
            // If empty: Show the "No favorites yet" layout
            binding.emptyState.visibility = View.VISIBLE
        } else {
            // If not empty: Show the RecyclerView
            binding.recyclerView.visibility = View.VISIBLE
            // If not empty: Hide the "No favorites yet" layout
            binding.emptyState.visibility = View.GONE
        }
    }


    // Step 6: Implement handleFavoriteClick (This is new)
    private fun handleFavoriteClick(deal: Deal) {
        // Make sure this import is at the top: import android.widget.Toast

        // When clicking the heart in the Favorites screen, we always assume the user wants to UNFAVORITE it.
        val newFavoriteState = false // Set favorite state to false (unfavorite)

        // 1. Update the state in SharedPreferences via FavoriteManager
        FavoriteManager.setFavorite(this, deal.id, newFavoriteState)

        // 2. Remove the item from our local list (currentFavoriteDeals)
        val removed = currentFavoriteDeals.removeIf { it.id == deal.id }

        if (removed) {
            // 3. If removal was successful, submit the updated (smaller) list to the adapter
            //    Submit a new copy of the list to help the adapter's diffing mechanism
            favoritesAdapter.submitList(ArrayList(currentFavoriteDeals))

            // 4. Update the visibility of the empty state view, in case the list became empty
            updateEmptyStateVisibility()

            // 5. Show a confirmation message to the user (optional)
            Toast.makeText(this, "Removed ${deal.title} from favorites", Toast.LENGTH_SHORT).show()
            Log.d("FavoritesActivity", "Unfavorited and removed deal from list: ${deal.id}")
        } else {
            // Log a warning if, for some reason, the item wasn't found in the list before removal
            Log.w("FavoritesActivity", "Tried to unfavorite a deal not found in the current list: ${deal.id}")
        }
    }


    // Step 4: Implement getRawTestData (Already done)
    // TODO: Replace this with a proper shared data source (Repository, ViewModel) later.
    private fun getRawTestData(): List<Deal> {
        val context = this
        return listOf(
            Deal("1", "Luxury Buffet", "Amazing buffet experience", 199.0, 399.0, "https://via.placeholder.com/300/FF0000/FFFFFF?text=Buffet", isFavorite = FavoriteManager.isFavorite(context, "1")),
            Deal("2", "Premium SPA Package", "Relax and rejuvenate", 299.0, 599.0, "https://via.placeholder.com/300/00FF00/FFFFFF?text=SPA", isFavorite = FavoriteManager.isFavorite(context, "2")),
            Deal("3", "Seafood Hotpot", "Fresh seafood hotpot", 258.0, 458.0, "https://via.placeholder.com/300/0000FF/FFFFFF?text=Hotpot", isFavorite = FavoriteManager.isFavorite(context, "3")),
            Deal("4", "City Center Hotel", "Comfortable stay downtown", 150.0, 250.0, "https://via.placeholder.com/300/FFFF00/000000?text=Hotel", isFavorite = FavoriteManager.isFavorite(context, "4")),
            Deal("5", "Beauty Salon Discount", "Discount on beauty treatments", 50.0, 100.0, "https://via.placeholder.com/300/FF00FF/FFFFFF?text=Beauty", isFavorite = FavoriteManager.isFavorite(context, "5"))
        )
    }


    // Keep the existing onSupportNavigateUp (Already done)
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Step 7: Implement onResume to refresh data (This is new)
    override fun onResume() {
        super.onResume()
        // Reload favorites when the activity comes back into view.
        // This handles cases where the user might have changed favorites
        // in another part of the app (e.g., DiscoverFragment) and then returns here.
        Log.d("FavoritesActivity", "onResume called, reloading favorites.")
        loadFavorites()
    }
}