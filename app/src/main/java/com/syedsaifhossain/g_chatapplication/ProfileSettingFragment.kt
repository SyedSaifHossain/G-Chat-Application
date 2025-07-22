package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.databinding.FragmentProfileSettingBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ProfileSettingFragment : Fragment() {

    private var _binding: FragmentProfileSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.profileSettingBackArrow.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.addImageButton.setOnClickListener {
            requestImagePermission()
        }

        binding.profileSettingNextButton.setOnClickListener {
            validateAndSaveProfile()
        }
    }

    private fun requestImagePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openGallery()
        } else {
            requestPermissions(arrayOf(permission), 1234)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1234 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data?.data != null) {
                val sourceUri = data.data!!
                val destUri = Uri.fromFile(
                    File(
                        requireContext().cacheDir,
                        "cropped_${System.currentTimeMillis()}.jpg"
                    )
                )


                val options = UCrop.Options().apply {
                    setToolbarColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    setStatusBarColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.black
                        )
                    )
                    setActiveControlsWidgetColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.white
                        )
                    )
                    setToolbarWidgetColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.white
                        )
                    )
                    setToolbarTitle("")
                    setFreeStyleCropEnabled(true)
                    setHideBottomControls(true)
                    setShowCropFrame(true)
                    setShowCropGrid(false)
                    setCircleDimmedLayer(false)
                }

                UCrop.of(sourceUri, destUri)
                    .withAspectRatio(1f,1f)
                    .withMaxResultSize(1080, 1080)
                    .withOptions(options)
                    .start(requireContext(), this)
            } else if (requestCode == UCrop.REQUEST_CROP) {
                val resultUri = UCrop.getOutput(data!!)
                resultUri?.let {
                    selectedImageUri = it
                    binding.profileImage.setImageURI(it)
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Toast.makeText(
                requireContext(),
                "Crop error: ${cropError?.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
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
            uploadImage(userId, firstName, lastName, selectedImageUri!!)
        } else {
            saveProfile(userId, firstName, lastName, null)
        }
    }

    private fun uploadImage(userId: String, firstName: String, lastName: String, uri: Uri) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val size = minOf(bitmap.width, bitmap.height, 512)
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, size, size)
        bitmap.recycle()

        val file = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
        FileOutputStream(file).use { fos ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        }
        cropped.recycle()

        val fileUri = Uri.fromFile(file)
        val path = "profile_images/$userId/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(path)

        ref.putFile(fileUri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                saveProfile(userId, firstName, lastName, url.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun saveProfile(
        userId: String,
        firstName: String,
        lastName: String,
        imageUrl: String?,
    ) {
        val userMap = mapOf(
            "uid" to userId,
            "firstName" to firstName,
            "lastName" to lastName,
            "name" to "$firstName $lastName",
            "profileImageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId).update(userMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.homeFragment)
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}