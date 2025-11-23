package com.blackgrapes.smartlineman.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.ImageView
import com.blackgrapes.smartlineman.R
import com.google.android.material.button.MaterialButton

object DialogHelper {

    fun showFailureDialog(context: Context, onNavigate: () -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_need_improvement, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val icon = view.findViewById<ImageView>(R.id.dialog_icon)
        val btnKnowledgeBase = view.findViewById<MaterialButton>(R.id.btn_knowledge_base)

        // Animate Icon (Pulse/Scale)
        icon.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(800)
            .withEndAction {
                icon.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(800)
                    .start()
            }
            .start()

        btnKnowledgeBase.setOnClickListener {
            dialog.dismiss()
            onNavigate()
        }

        dialog.show()
    }
}
