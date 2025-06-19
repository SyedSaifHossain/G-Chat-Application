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
import com.bumptech.glide.Glide
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

    // TAG for logging
    private val TAG = "SignupNextFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Fragment created.")
        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView: Layout inflated.")
        binding = FragmentSignupNextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Views created.")

        // Load existing profile data if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "onViewCreated: Current user is logged in (UID: ${currentUser.uid})")
            currentUser.displayName?.let { name ->
                binding.nameEdit.setText(name)
                Log.d(TAG, "onViewCreated: Loaded existing display name: $name")
            }

            currentUser.photoUrl?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.profile)
                    .into(binding.profileImage)
                imageUri = uri // Set imageUri to existing photoUrl so it's preserved if not changed
                Log.d(TAG, "onViewCreated: Loaded existing photoUrl: $uri")
            }
        } else {
            Log.d(TAG, "onViewCreated: No current user logged in. This might be an issue if signup just completed.")
            Toast.makeText(requireContext(), "User not logged in. Please restart verification.", Toast.LENGTH_LONG).show()
            // Consider navigating back to verification if no user is found here
            // findNavController().navigate(R.id.action_signupNextFragment_to_signupPageVerificationFragment)
        }


        binding.addImageButton.setOnClickListener {
            Log.d(TAG, "addImageButton clicked.")
            val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Change Profile Picture")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> chooseFromGallery()
                    2 -> dialog.dismiss()
                }
            }
            builder.show()
        }

        binding.completeButton.setOnClickListener {
            Log.d(TAG, "completeButton clicked. Calling handleCompleteButtonClick().")
            handleCompleteButtonClick()
        }
    }

    private fun handleCompleteButtonClick() {
        Log.d(TAG, "handleCompleteButtonClick() called.")
        val username = binding.nameEdit.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "handleCompleteButtonClick: Username is empty. Returning.")
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in. Please restart verification.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "handleCompleteButtonClick: Current user is NULL. Cannot proceed.")
            findNavController().popBackStack() // Go back to previous screen if no user
            return
        }

        binding.completeButton.isEnabled = false // Disable button to prevent multiple clicks
        Log.d(TAG, "handleCompleteButtonClick: Button disabled.")
        // binding.progressBar.visibility = View.VISIBLE // Uncomment if you have a ProgressBar

        // Only upload if a new image is selected AND it's different from the current photoUrl
        // or if currentUser.photoUrl is null and imageUri is not null (new image for user without one)
        if (imageUri != null && imageUri != currentUser.photoUrl) {
            Log.d(TAG, "handleCompleteButtonClick: New imageUri selected or different from current. Uploading image.")
            uploadProfileImage(currentUser.uid, username, imageUri!!)
        } else {
            Log.d(TAG, "handleCompleteButtonClick: No new image selected or image is same as current. Updating profile directly.")
            // Pass the current photoUrl from Firebase Auth if no new image was selected or if it's the same
            updateUserProfileAndFirestore(currentUser.uid, username, currentUser.photoUrl?.toString())
        }
    }

    private fun takePhoto() {
        Log.d(TAG, "takePhoto() called. Checking camera permission.")
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e(TAG, "Error creating image file: ${ex.message}", ex)
                Toast.makeText(requireContext(), "Error creating image file.", Toast.LENGTH_SHORT).show()
                null
            }

            photoFile?.also {
                // IMPORTANT: "com.syedsaifhossain.g_chatapplication.fileprovider" must match your AndroidManifest.xml authority
                imageUri = FileProvider.getUriForFile(requireContext(), "com.syedsaifhossain.g_chatapplication.fileprovider", it)
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                @Suppress("DEPRECATION")
                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA)
                Log.d(TAG, "takePhoto: Starting camera intent.")
            }
        } else {
            Log.d(TAG, "takePhoto: Camera permission not granted. Requesting permission.")
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CAMERA), 1)
        }
    }

    private fun chooseFromGallery() {
        Log.d(TAG, "chooseFromGallery() called. Starting gallery intent.")
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        @Suppress("DEPRECATION")
        startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    imageUri = data?.data
                    imageUri?.let { uri ->
                        Glide.with(this).load(uri).into(binding.profileImage)
                        Log.d(TAG, "onActivityResult: Gallery image selected: $uri")
                    } ?: Log.w(TAG, "onActivityResult: Gallery data or URI is null.")
                }
                REQUEST_CODE_CAMERA -> {
                    imageUri?.let { uri ->
                        Glide.with(this).load(uri).into(binding.profileImage)
                        Log.d(TAG, "onActivityResult: Camera image captured: $uri")
                    } ?: Log.w(TAG, "onActivityResult: Camera imageUri is null after capture.")
                }
            }
        } else {
            Toast.makeText(requireContext(), "Failed to get image", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "onActivityResult: Image selection cancelled or failed.")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir: File = requireContext().getExternalFilesDir(null) ?: throw IOException("External storage is not available")
        val imageFileName = "JPEG_${System.currentTimeMillis()}_"
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        currentPhotoPath = imageFile.absolutePath
        Log.d(TAG, "createImageFile: Created temp file: $currentPhotoPath")
        return imageFile
    }

    private fun uploadProfileImage(userId: String, name: String, imageUri: Uri) {
        Log.d(TAG, "uploadProfileImage: Uploading image for userId: $userId, imageUri: $imageUri")
        val storageRef = storage.reference.child("profile_images").child("$userId.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "uploadProfileImage: Image upload successful. Getting download URL.")
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d(TAG, "uploadProfileImage: Download URL received: $downloadUri")
                    updateUserProfileAndFirestore(userId, name, downloadUri.toString())
                }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "uploadProfileImage: Failed to get download URL: ${e.message}", e)
                        Toast.makeText(requireContext(), "Failed to get image URL.", Toast.LENGTH_LONG).show()
                        binding.completeButton.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "uploadProfileImage: Error uploading profile image: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_LONG).show()
                binding.completeButton.isEnabled = true
                // binding.progressBar.visibility = View.GONE
            }
    }

    private fun updateUserProfileAndFirestore(userId: String, name: String, avatarUrl: String?) {
        Log.d(TAG, "updateUserProfileAndFirestore: Updating profile for userId: $userId, name: $name, avatarUrl: $avatarUrl")
        val currentUser = auth.currentUser ?: run {
            Log.e(TAG, "updateUserProfileAndFirestore: Current user is null during profile update attempt.")
            Toast.makeText(requireContext(), "Authentication error.", Toast.LENGTH_SHORT).show()
            binding.completeButton.isEnabled = true
            return
        }

        // 1. Update Firebase Authentication displayName and photoUrl
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .apply {
                if (avatarUrl != null) {
                    setPhotoUri(Uri.parse(avatarUrl))
                    Log.d(TAG, "updateUserProfileAndFirestore: Setting Auth photoUri to $avatarUrl")
                } else {
                    setPhotoUri(null) // If avatarUrl is null, set photoUri to null in Auth
                    Log.d(TAG, "updateUserProfileAndFirestore: Setting Auth photoUri to NULL")
                }
            }
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    Log.d(TAG, "updateUserProfileAndFirestore: Firebase Auth profile updated successfully.")
                    // 2. Save/Update user profile in Cloud Firestore
                    saveUserProfileToFirestore(userId, name, avatarUrl)
                } else {
                    Log.e(TAG, "updateUserProfileAndFirestore: Error updating Firebase Auth profile: ${authTask.exception?.message}", authTask.exception)
                    Toast.makeText(requireContext(), "Failed to update profile: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                    binding.completeButton.isEnabled = true
                    // binding.progressBar.visibility = View.GONE
                }
            }
    }

    private fun saveUserProfileToFirestore(userId: String, name: String, avatarUrl: String?) {
        Log.d(TAG, "saveUserProfileToFirestore: Saving profile to Firestore for UID: $userId, name: $name, avatarUrl: $avatarUrl")
        val userProfileData = hashMapOf(
            "uid" to userId,
            "name" to name,
            "phone" to (auth.currentUser?.phoneNumber ?: ""),
            "email" to (auth.currentUser?.email ?: ""),
            "avatarUrl" to (avatarUrl ?: auth.currentUser?.photoUrl?.toString() ?: ""),
            "status" to "Hey there! I'm using G-Chat",
            "isOnline" to true,
            "lastSeen" to System.currentTimeMillis(),
            "lastUpdated" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId)
            .set(userProfileData) // Use .set() to create or overwrite
            .addOnSuccessListener {
                Log.d(TAG, "saveUserProfileToFirestore: User profile saved to Firestore successfully for UID: $userId")
                Toast.makeText(requireContext(), "Profile setup complete!", Toast.LENGTH_SHORT).show()

                // Navigate to homeFragment after successful Firestore update
                Log.d(TAG, "saveUserProfileToFirestore: Navigating to homeFragment.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "saveUserProfileToFirestore: Error saving user profile to Firestore: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to save profile data: ${e.message}", Toast.LENGTH_LONG).show()
                binding.completeButton.isEnabled = true
                // binding.progressBar.visibility = View.GONE
            }
    }
}