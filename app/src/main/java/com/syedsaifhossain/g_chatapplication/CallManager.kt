package com.syedsaifhossain.g_chatapplication

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object CallManager {
    fun initiateVoiceCall(fragment: Fragment, otherUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        val callRef = firestore.collection("calls").document()
        val callId = callRef.id
        
        Log.d("CallManager", "Initiating voice call: callId=$callId, from=$currentUserId, to=$otherUserId")
        
        val callRequest = mapOf(
            "callId" to callId,
            "from" to currentUserId,
            "to" to otherUserId,
            "callType" to "voice",
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )
        callRef.set(callRequest)
        showWaitingDialog(fragment, callId)
    }

    fun initiateVideoCall(fragment: Fragment, otherUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        val callRef = firestore.collection("calls").document()
        val callId = callRef.id
        
        Log.d("CallManager", "Initiating video call: callId=$callId, from=$currentUserId, to=$otherUserId")
        
        val callRequest = mapOf(
            "callId" to callId,
            "from" to currentUserId,
            "to" to otherUserId,
            "callType" to "video",
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )
        callRef.set(callRequest)
        showWaitingDialog(fragment, callId)
    }

    private fun showWaitingDialog(fragment: Fragment, callId: String) {
        val context = fragment.context ?: return
        Log.d("CallManager", "Showing waiting dialog: callId=$callId")
        
        val dialog = AlertDialog.Builder(context)
            .setTitle("Calling...")
            .setMessage("Waiting for the other user to accept")
            .setNegativeButton("Cancel") { d, _ ->
                Log.d("CallManager", "User cancelled call: callId=$callId")
                FirebaseFirestore.getInstance().collection("calls").document(callId)
                    .update("status", "ended")
                d.dismiss()
            }
            .setCancelable(false)
            .create()
        dialog.show()

        val callRef = FirebaseFirestore.getInstance().collection("calls").document(callId)
        callRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("CallManager", "Call listener error: ${error.message}")
                Toast.makeText(context, "Call error: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getString("status")
                val callType = snapshot.getString("callType")
                
                Log.d("CallManager", "Call status updated: callId=$callId, status=$status, callType=$callType")
                
                if (status == "accepted") {
                    Log.d("CallManager", "Call accepted, preparing to navigate to call screen: callId=$callId")
                    dialog.dismiss()
                    val bundle = Bundle().apply { putString("callId", callId) }
                    try {
                        if (callType == "voice") {
                            Log.d("CallManager", "Navigating to voice call screen")
                            fragment.findNavController().navigate(
                                R.id.action_chatScreenPageMoreOptionFragment_to_voiceCallFragment,
                                bundle
                            )
                        } else if (callType == "video") {
                            Log.d("CallManager", "Navigating to video call screen")
                            fragment.findNavController().navigate(
                                R.id.action_chatScreenPageMoreOptionFragment_to_videoCallFragment,
                                bundle
                            )
                        } else {
                            Log.e("CallManager", "Unknown call type: $callType")
                        }
                    } catch (e: Exception) {
                        Log.e("CallManager", "Navigation failed: ${e.message}")
                        Toast.makeText(context, "Navigation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else if (status == "rejected" || status == "ended") {
                    Log.d("CallManager", "Call ended: callId=$callId, status=$status")
                    dialog.dismiss()
                    val message = if (status == "rejected") "Call rejected" else "Call ended"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("CallManager", "Call status: $status")
                }
            }
        }
    }
} 