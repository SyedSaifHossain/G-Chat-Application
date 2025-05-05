package com.syedsaifhossain.g_chatapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignupNextBinding
import java.io.File
import java.io.IOException


class SignupNextFragment : Fragment() {

    private lateinit var binding : FragmentSignupNextBinding
    private var imageUri: Uri? = null
    private lateinit var currentPhotoPath: String
    private val REQUEST_CODE_GALLERY = 1000
    private val REQUEST_CODE_CAMERA = 1001
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSignupNextBinding.inflate(inflater, container, false)

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

            val username = binding.nameEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString().trim()
            val confirmPassword = binding.comfirmPasswordEdit.text.toString().trim()

            // Validate the inputs
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please provide the cridential",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else if (username.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            } else if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your password", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            } else if (confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please confirm your password", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            } else if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (!isPasswordValid(password)) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 8 characters long and contain both letters and numbers",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            } else {
                // Proceed with the sign-up process (e.g., call your API or save data locally)
                Toast.makeText(requireContext(), "Sign Up Successful!", Toast.LENGTH_SHORT).show()

                // For now, just clear the fields
                binding.nameEdit.text.clear()
                binding.passwordEdit.text.clear()
                binding.comfirmPasswordEdit.text.clear()

            }


        }
        return binding.root
    }

    private fun isPasswordValid(password: String): Boolean {
        // Check password length
        if (password.length < 8) {
            return false
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        return hasLetter && hasDigit
    }


    private fun takePhoto() {
        // Check if the camera permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Create the file to store the photo
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }

            // Open the camera
            photoFile?.also {
                imageUri = FileProvider.getUriForFile(requireContext(), "com.yourapp.fileprovider", it)
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA)
            }
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CAMERA), 1)
        }
    }

    private fun chooseFromGallery() {
        // Intent to open the gallery
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    // Get the image URI from the gallery
                    imageUri = data?.data

                    binding.profileImage.setImageURI(imageUri)
                }
                REQUEST_CODE_CAMERA -> {
                    // Display the image that was captured by the camera
                    binding.profileImage.setImageURI(imageUri)
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

}