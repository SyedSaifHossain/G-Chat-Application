package com.syedsaifhossain.g_chatapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide // For loading images
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignupNextBinding
import java.io.File
import java.io.IOException

class SignupNextFragment : Fragment() {

    private lateinit var binding: FragmentSignupNextBinding
    private var imageUri: Uri? = null // URI of the selected/captured image
    private lateinit var currentPhotoPath: String // Path for camera captured image

    private val REQUEST_CODE_GALLERY = 1000
    private val REQUEST_CODE_CAMERA = 1001

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSignupNextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load existing profile data if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Load display name into EditText
            currentUser.displayName?.let { name ->
                binding.nameEdit.setText(name)
            }

            // Load profile image into CircleImageView
            currentUser.photoUrl?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.profile) // Default image while loading or if error
                    .into(binding.profileImage)
                imageUri = uri // Set imageUri to existing photoUrl so it's preserved if not changed
            }
        }

        binding.addImageButton.setOnClickListener {
            // Display options to pick image from gallery or camera
            val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Change Profile Picture")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> takePhoto()  // Take photo using camera
                    1 -> chooseFromGallery() // Choose from gallery
                    2 -> dialog.dismiss()  // Cancel
                }
            }
            builder.show()
        }

        binding.completeButton.setOnClickListener {
            handleCompleteButtonClick()
        }
    }

    // Removed isPasswordValid function as it's no longer needed

    private fun handleCompleteButtonClick() {
        val username = binding.nameEdit.text.toString().trim()
        // Removed password and confirmPassword variables

        // Validate the inputs (only username now)
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "请输入您的姓名", Toast.LENGTH_SHORT) // Please enter your name
                .show()
            return
        }

        // All validations passed, proceed with Firebase profile update
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "用户未认证，请重新登录", Toast.LENGTH_LONG).show() // User not authenticated, please log in again
            findNavController().popBackStack() // Go back to previous screen if no user
            return
        }

        // Disable button and show loading (if you have a ProgressBar)
        binding.completeButton.isEnabled = false
        // binding.progressBar.visibility = View.VISIBLE // Uncomment if you have a ProgressBar

        // Only upload if a new image is selected AND it's different from the current photoUrl
        if (imageUri != null && imageUri != currentUser.photoUrl) {
            uploadProfileImage(currentUser.uid, username, imageUri!!) // Removed password parameter
        } else {
            // No new image selected, just update name and save to Firestore
            // Pass the current photoUrl from Firebase Auth if no new image was selected
            updateUserProfileAndFirestore(currentUser.uid, username, currentUser.photoUrl?.toString()) // Removed password parameter
        }
    }

    private fun takePhoto() {
        // Check if the camera permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Create the file to store the photo
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("SignupNext", "Error creating image file: ${ex.message}", ex)
                Toast.makeText(requireContext(), "Error creating image file.", Toast.LENGTH_SHORT).show()
                null
            }

            // Open the camera
            photoFile?.also {
                // IMPORTANT: "com.yourapp.fileprovider" must match your AndroidManifest.xml authority
                imageUri = FileProvider.getUriForFile(requireContext(), "com.yourapp.fileprovider", it)
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                @Suppress("DEPRECATION")
                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA)
            }
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CAMERA), 1)
        }
    }

    private fun chooseFromGallery() {
        // Intent to open the gallery
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        @Suppress("DEPRECATION")
        startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    // Get the image URI from the gallery
                    imageUri = data?.data
                    imageUri?.let { uri ->
                        Glide.with(this)
                            .load(uri)
                            .into(binding.profileImage)
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    // Display the image that was captured by the camera
                    // imageUri is already set in takePhoto()
                    imageUri?.let { uri ->
                        Glide.with(this)
                            .load(uri)
                            .into(binding.profileImage)
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Failed to get image", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create a file to store the image
        val storageDir: File = requireContext().getExternalFilesDir(null) ?: throw IOException("External storage is not available")
        return File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",  /* prefix */
            ".jpg",  /* suffix */
            storageDir      /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    // Function to upload profile image to Firebase Storage
    private fun uploadProfileImage(userId: String, name: String, imageUri: Uri) { // Removed password parameter
        val storageRef = storage.reference.child("profile_images").child("$userId.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d("ProfileUpdate", "Profile image uploaded. Download URL: $downloadUri")
                    // Now update user profile in Auth and Firestore with the new image URL
                    updateUserProfileAndFirestore(userId, name, downloadUri.toString()) // Removed password parameter
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileUpdate", "Error uploading profile image: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_LONG).show()
                binding.completeButton.isEnabled = true // Re-enable button
                // binding.progressBar.visibility = View.GONE
            }
    }

    // Function to update user profile in Firebase Auth and Firestore
    private fun updateUserProfileAndFirestore(userId: String, name: String, profileImageUrl: String?) { // Removed password parameter
        val currentUser = auth.currentUser ?: run {
            Log.e("ProfileUpdate", "Current user is null during profile update attempt.")
            Toast.makeText(requireContext(), "Authentication error.", Toast.LENGTH_SHORT).show()
            binding.completeButton.isEnabled = true
            return
        }

        // 1. Update Firebase Authentication displayName and photoUrl
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .apply {
                // Set photo URL only if a new image was uploaded or an existing one is passed
                if (profileImageUrl != null) {
                    setPhotoUri(Uri.parse(profileImageUrl))
                } else if (currentUser.photoUrl != null && imageUri == currentUser.photoUrl) {
                    // If no new image, but user already has one and it's the same as current imageUri, keep it
                    setPhotoUri(currentUser.photoUrl)
                }
                // If profileImageUrl is null AND currentUser.photoUrl is null, then photoUrl will be set to null in Auth
            }
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    Log.d("ProfileUpdate", "Firebase Auth profile updated.")
                    // 2. Save/Update user profile in Cloud Firestore
                    saveUserProfileToFirestore(userId, name, profileImageUrl) // Removed password parameter
                } else {
                    Log.e("ProfileUpdate", "Error updating Firebase Auth profile: ${authTask.exception?.message}", authTask.exception)
                    Toast.makeText(requireContext(), "Failed to update profile: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                    binding.completeButton.isEnabled = true
                    // binding.progressBar.visibility = View.GONE
                }
            }
    }

    // Function to save/update user profile in Cloud Firestore
    private fun saveUserProfileToFirestore(userId: String, name: String, profileImageUrl: String?) { // Removed password parameter
        val userProfileData = hashMapOf(
            "uid" to userId,
            "name" to name,
            "email" to auth.currentUser?.email, // Get email from Auth
            "profileImageUrl" to (profileImageUrl ?: auth.currentUser?.photoUrl?.toString() ?: ""), // Use new URL or existing or empty
            // Removed "password" field from Firestore data
            "lastUpdated" to System.currentTimeMillis() // Timestamp
            // Add other fields as needed, e.g., "bio", "status"
        )

        firestore.collection("users").document(userId)
            .set(userProfileData) // Use .set() to create or overwrite
            .addOnSuccessListener {
                Log.d("ProfileUpdate", "User profile saved to Firestore for UID: $userId")
                Toast.makeText(requireContext(), "Profile setup complete!", Toast.LENGTH_SHORT).show()

                // Pass username and profile image URL to the next fragment
                val bundle = Bundle().apply {
                    putString("userName", name)
                    // Removed password from bundle
                    putString("profileImageUrl", profileImageUrl) // Pass the image URL
                }
                // Assuming signupPageVerificationFragment is still the next step for phone verification
                findNavController().navigate(R.id.action_signupNextFragment_to_signupPageVerificationFragment, bundle)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileUpdate", "Error saving user profile to Firestore: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to save profile data: ${e.message}", Toast.LENGTH_LONG).show()
                binding.completeButton.isEnabled = true
                // binding.progressBar.visibility = View.GONE
            }
    }

}