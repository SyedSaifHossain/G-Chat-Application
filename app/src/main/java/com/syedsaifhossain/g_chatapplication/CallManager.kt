package com.syedsaifhossain.g_chatapplication

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object CallManager {
    fun initiateVoiceCall(fragment: Fragment, otherUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val callId = FirebaseDatabase.getInstance().getReference("calls").push().key ?: return
        val callRequest = mapOf(
            "from" to currentUserId,
            "to" to otherUserId,
            "callType" to "voice",
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )
        FirebaseDatabase.getInstance().getReference("calls").child(callId).setValue(callRequest)
        showWaitingDialog(fragment, callId)
    }

    private fun showWaitingDialog(fragment: Fragment, callId: String) {
        val context = fragment.context ?: return
        val dialog = AlertDialog.Builder(context)
            .setTitle("Calling...")
            .setMessage("Waiting for the other user to accept")
            .setNegativeButton("Cancel") { d, _ ->
                FirebaseDatabase.getInstance().getReference("calls").child(callId).child("status")
                    .setValue("ended")
                d.dismiss()
            }
            .setCancelable(false)
            .create()
        dialog.show()

        val callRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        callRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                val callType = snapshot.child("callType").getValue(String::class.java)
                if (status == "accepted") {
                    dialog.dismiss()
                    val bundle = Bundle().apply { putString("callId", callId) }
                    try {
                        if (callType == "voice") {
                            fragment.findNavController().navigate(
                                R.id.action_chatScreenPageMoreOptionFragment_to_voiceCallFragment,
                                bundle
                            )
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Navigation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else if (status == "rejected" || status == "ended") {
                    dialog.dismiss()
                    val message = if (status == "rejected") "Call rejected" else "Call ended"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(context, "Call cancelled: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
} 