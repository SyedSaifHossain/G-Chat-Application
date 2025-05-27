package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentProfileSettingBinding

// Import Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID // For generating unique image filenames

class ProfileSettingFragment : Fragment() {

    private var _binding: FragmentProfileSettingBinding? = null
    private val binding get() = _binding!!

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Holds the URI of the selected image
    private var selectedImageUri: Uri? = null

    // For requesting gallery permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickImageFromGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied to access gallery", Toast.LENGTH_SHORT).show()
        }
    }

    // For handling the result of picking an image from the gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                selectedImageUri = it // Store the URI
                binding.profileSelect.setImageURI(it)
                Toast.makeText(requireContext(), "Image selected successfully!", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(requireContext(), "Failed to get image URI.", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(requireContext(), "Image selection cancelled.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Image selection failed.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase instances here
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileSettingBackArrow.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.addImageButton.setOnClickListener {
            checkPermissionsAndPickImage()
        }

        binding.profileSettingNextButton.setOnClickListener {
            validateAndSaveProfile() // New function to handle validation and saving
        }
    }

    private fun checkPermissionsAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                pickImageFromGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(requireContext(), "Permission is needed to access your photo gallery.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun validateAndSaveProfile() {
        val firstName = binding.firstNameEdt.text.toString().trim()
        val lastName = binding.lastNameEdt.text.toString().trim()

        if (firstName.isEmpty()) {
            binding.firstNameEdt.error = "First Name cannot be empty"
            binding.firstNameEdt.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            binding.lastNameEdt.error = "Last Name cannot be empty"
            binding.lastNameEdt.requestFocus()
            return
        }

        // --- Firebase Integration Starts Here ---
        // Get the current user ID
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            // Optionally, navigate back to login or handle this case
            return
        }

        // Check if an image is selected
        if (selectedImageUri != null) {
            uploadImageToFirebaseStorage(userId, firstName, lastName, selectedImageUri!!)
        } else {
            // If no image is selected, just save the names
            saveProfileNamesToFirestore(userId, firstName, lastName, null)
        }
    }

    private fun uploadImageToFirebaseStorage(userId: String, firstName: String, lastName: String, imageUri: Uri) {
        val fileName = UUID.randomUUID().toString() + ".jpg" // Generate a unique filename
        val imageRef = storage.reference.child("profile_images/$userId/$fileName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Image uploaded successfully, now get the download URL
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    Toast.makeText(requireContext(), "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    // Save profile data including the image URL to Firestore
                    saveProfileNamesToFirestore(userId, firstName, lastName, downloadUri.toString())
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to get download URL: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                // You could show a progress bar here
                Toast.makeText(requireContext(), "Uploading: $progress%", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileNamesToFirestore(userId: String, firstName: String, lastName: String, profileImageUrl: String?) {
        val userProfile = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "profileImageUrl" to profileImageUrl, // This will be null if no image is selected
            "timestamp" to System.currentTimeMillis() // Optional: add a timestamp
        )

        firestore.collection("users").document(userId)
            .set(userProfile) // Use set() to create or overwrite the document
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                // Navigate to the next screen or close this fragment
                // findNavController().navigate(R.id.action_profileSettingFragment_to_homeFragment) // Example navigation
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding when the view is destroyed
    }
}