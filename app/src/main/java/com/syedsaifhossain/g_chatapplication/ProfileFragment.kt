package com.syedsaifhossain.g_chatapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
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
import com.journeyapps.barcodescanner.BarcodeEncoder

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference
    private val IMAGE_PICK_CODE = 1000
    private val QR_CODE_WIDTH = 400
    private val QR_CODE_HEIGHT = 400

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

        binding.nameArrow.setOnClickListener {
            showNameEditDialog()
        }

        binding.phoneArrow.setOnClickListener {
            showPhoneEditDialog()
        }

        binding.genderArrow.setOnClickListener {
            showGenderEditDialog()
        }

        binding.qrcodeArrow.setOnClickListener {
            showQRCodeEditDialog()
        }


        binding.regionArrow.setOnClickListener { showRegionEditDialog() }
    }

    private fun fetchUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: "No phone number"
                    val gender = snapshot.child("gender").getValue(String::class.java) ?: "Not Set" // Fetch gender
                    val qrCodeUrl = snapshot.child("qrCodeUrl").getValue(String::class.java) ?: ""
                    val region = snapshot.child("region").getValue(String::class.java) ?: ""
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                        ?: snapshot.child("avatarUrl").getValue(String::class.java)

                    binding.userNameTxt.text = name
                    binding.phoneNameTxt.text = phone
                    binding.genderNameTxt.text = gender
                    binding.regionNameTxt.text = region
                    if (qrCodeUrl.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(qrCodeUrl)
                            .into(binding.myqrCodeImg)
                    }

                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.default_avatar)
                        .override(70, 70)
                        .into(binding.profilePhotoImg)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    // Handle the result of image selection (profile photo)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val imageUri = data?.data // URI of the selected image
            imageUri?.let {
                uploadProfileImage(it)  // Upload selected image to Firebase
            }
        }
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        // Get a reference to Firebase Storage where the profile image will be stored
        val fileRef: StorageReference = FirebaseStorage.getInstance().reference.child("profile_images/${userId}.jpg")

        // Upload the image to Firebase Storage
        fileRef.putFile(imageUri).addOnSuccessListener {
            // After upload, get the image URL
            fileRef.downloadUrl.addOnSuccessListener { uri ->
                updateProfileImageUrl(uri.toString())
                Glide.with(requireContext())
                    .load(uri)
                    .into(binding.profilePhotoImg)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfileImageUrl(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        val updates = mapOf("profileImageUrl" to imageUrl)

        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile image updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile image", Toast.LENGTH_SHORT).show()
            }
    }

    // Edit Name
    private fun showNameEditDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.setText(binding.userNameTxt.text.toString())

        builder.setTitle("Edit Name")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateNameInDatabase(newName)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    private fun updateNameInDatabase(newName: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("name" to newName)
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                binding.userNameTxt.text = newName
                Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
            }
    }

    // Edit Phone
    private fun showPhoneEditDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.setText(binding.phoneNameTxt.text.toString())

        builder.setTitle("Edit Phone Number")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newPhone = input.text.toString().trim()
                if (newPhone.isNotEmpty()) {
                    updatePhoneInDatabase(newPhone)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Phone number cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    private fun updatePhoneInDatabase(newPhone: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("phone" to newPhone)
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                binding.phoneNameTxt.text = newPhone
                Toast.makeText(requireContext(), "Phone number updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update phone number", Toast.LENGTH_SHORT).show()
            }
    }

    // Edit Gender
    private fun showGenderEditDialog() {
        val genderOptions = arrayOf("Male", "Female", "Other")
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle("Edit Gender")
            .setItems(genderOptions) { dialog, which ->
                val newGender = genderOptions[which]
                updateGenderInDatabase(newGender)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    private fun updateGenderInDatabase(newGender: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("gender" to newGender)
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                binding.genderNameTxt.text = newGender
                Toast.makeText(requireContext(), "Gender updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update gender", Toast.LENGTH_SHORT).show()
            }
    }

    // Edit QR Code
    private fun showQRCodeEditDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.setText(binding.qrcodeTxt.text.toString()) // Placeholder

        builder.setTitle("Edit QR Code")
            .setView(input)
            .setPositiveButton("Generate") { dialog, _ ->
                val newQRCodeData = input.text.toString().trim()
                if (newQRCodeData.isNotEmpty()) {
                    generateQRCode(newQRCodeData)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Input cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    // Generate QR Code
    private fun generateQRCode(data: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(data, com.google.zxing.BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT)
            binding.myqrCodeImg.setImageBitmap(bitmap) // Set the QR code in ImageView
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showRegionEditDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.setText(binding.regionNameTxt.text.toString())

        builder.setTitle("Edit Region")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newRegion = input.text.toString().trim()
                if (newRegion.isNotEmpty()) {
                    val userId = auth.currentUser?.uid ?: return@setPositiveButton
                    val updates = mapOf("region" to newRegion)
                    database.child("users").child(userId).updateChildren(updates)
                        .addOnSuccessListener {
                            binding.regionNameTxt.text = newRegion
                            Toast.makeText(requireContext(), "Region updated", Toast.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}