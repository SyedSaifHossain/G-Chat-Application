package com.syedsaifhossain.g_chatapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.syedsaifhossain.g_chatapplication.databinding.FragmentProfileBinding
import com.syedsaifhossain.g_chatapplication.models.User

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference
    private val IMAGE_PICK_CODE = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference

        // Fetch and display the user data
        fetchUserProfile()

        // Back button listener
        binding.profileBackImg.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_mePageFragment)
        }

        // Open gallery to change profile image
        binding.profilePhotoArrow.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }
    }

    // Fetch user profile from Firebase Realtime Database
    private fun fetchUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: "No phone number"
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                        ?: snapshot.child("avatarUrl").getValue(String::class.java)

                    binding.userNameTxt.text = name
                    binding.phoneNameTxt.text = phone

                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.default_avatar)
                        .override(70, 70)
                        .into(binding.profilePhotoImg)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                }
            })
    }

    // Handle the result of image selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val imageUri = data?.data // URI of the selected image
            imageUri?.let {
                // Upload the selected image to Firebase Storage
                uploadProfileImage(it)
            }
        }
    }

    // Upload the selected image to Firebase Storage and update the URL in Firebase Database
    private fun uploadProfileImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        // Get a reference to Firebase Storage where the profile image will be stored
        val fileRef: StorageReference = FirebaseStorage.getInstance().reference.child("profile_images/${userId}.jpg")

        // Upload the image to Firebase Storage
        fileRef.putFile(imageUri).addOnSuccessListener {
            // After upload, get the image URL
            fileRef.downloadUrl.addOnSuccessListener { uri ->
                // Update the profile image URL in Firebase Realtime Database
                updateProfileImageUrl(uri.toString())

                // Set the image in the ImageView
                Glide.with(requireContext())
                    .load(uri)
                    .into(binding.profilePhotoImg) // Update the profile image in the UI
            }
        }.addOnFailureListener {
            // Handle errors
            Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    // Update the profile image URL in Firebase Realtime Database
    private fun updateProfileImageUrl(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        // Create a map of updated values
        val updates = mapOf("profileImageUrl" to imageUrl)

        // Update the profile image URL in Firebase
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile image updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile image", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}