package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.databinding.FragmentProfileSettingBinding
import java.util.*

class ProfileSettingFragment : Fragment() {

    private var _binding: FragmentProfileSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageFromGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                selectedImageUri = it
                binding.profileImage.setImageURI(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
            validateAndSaveProfile()
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
                Toast.makeText(requireContext(), "Gallery permission is required", Toast.LENGTH_SHORT).show()
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
            binding.firstNameEdt.error = "First Name is required"
            return
        }
        if (lastName.isEmpty()) {
            binding.lastNameEdt.error = "Last Name is required"
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            uploadImageToFirebase(userId, firstName, lastName, selectedImageUri!!)
        } else {
            saveUserToDatabase(userId, firstName, lastName, null)
        }
    }

    private fun uploadImageToFirebase(userId: String, firstName: String, lastName: String, imageUri: Uri) {
        val fileName = UUID.randomUUID().toString() + ".jpg"
        val imageRef = storage.reference.child("profile_images/$userId/$fileName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveUserToDatabase(userId, firstName, lastName, downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserToDatabase(userId: String, firstName: String, lastName: String, imageUrl: String?) {
        val userProfile = mapOf(
            "uid" to userId,
            "firstName" to firstName,
            "lastName" to lastName,
            "profileImageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        database.child("users").child(userId).setValue(userProfile)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_profileSettingFragment_to_homeFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error saving profile: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}