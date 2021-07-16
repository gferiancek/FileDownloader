package com.example.filedownloader.adapter

import android.util.TypedValue
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.filedownloader.R

@BindingAdapter("textOrHyphen")
fun TextView.setTextOrHyphen(item: String?) {
    item?.let {
        text = when (id) {
            R.id.tv_file_path -> {
                // filepath is the only multiline item, so if it isn't blank we set it to a smaller text size,
                // otherwise we set it to the same text size as the other items.
                if (item.isNotBlank()) {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    item
                } else {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
                    "-"
                }
            }
            R.id.tv_status -> if (item.isNotBlank()) item else "-"
            R.id.tv_reason -> if (item.isNotBlank()) item else "-"
            R.id.tv_size -> if (item.isNotBlank()) item else "-"
            else -> ""
        }
    }
}