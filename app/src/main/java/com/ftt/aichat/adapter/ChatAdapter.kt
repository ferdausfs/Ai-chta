package com.ftt.aichat.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ftt.aichat.R
import com.ftt.aichat.data.Message
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin

class ChatAdapter(context: Context) :
    ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_ASSISTANT = 1
    }

    // Markwon for rendering markdown in assistant messages
    private val markwon = Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .build()

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).role == Message.ROLE_USER) VIEW_TYPE_USER
        else VIEW_TYPE_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = inflater.inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_message_assistant, parent, false)
            AssistantViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(message)
            is AssistantViewHolder -> holder.bind(message, markwon)
        }
    }

    // ── User Message ViewHolder ───────────────────────────────────
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_message_content)

        fun bind(message: Message) {
            tvContent.text = message.content

            // Long press to copy
            itemView.setOnLongClickListener {
                copyToClipboard(itemView.context, message.content)
                true
            }
        }
    }

    // ── Assistant Message ViewHolder ──────────────────────────────
    class AssistantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tv_message_content)
        private val tvTyping: TextView = itemView.findViewById(R.id.tv_typing_indicator)

        fun bind(message: Message, markwon: Markwon) {
            if (message.isStreaming && message.content.isEmpty()) {
                // Show typing indicator
                tvTyping.visibility = View.VISIBLE
                tvContent.visibility = View.GONE
            } else {
                tvTyping.visibility = View.GONE
                tvContent.visibility = View.VISIBLE

                if (message.isError) {
                    // Error messages: plain text, error color
                    tvContent.text = message.content
                    tvContent.setTextColor(
                        itemView.context.getColor(R.color.error_color)
                    )
                } else {
                    // Normal: render markdown
                    tvContent.setTextColor(
                        itemView.context.getColor(R.color.text_primary)
                    )
                    markwon.setMarkdown(tvContent, message.content)
                }
            }

            // Long press to copy
            itemView.setOnLongClickListener {
                if (message.content.isNotEmpty()) {
                    copyToClipboard(itemView.context, message.content)
                }
                true
            }
        }
    }

    // ── DiffUtil ──────────────────────────────────────────────────
    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}

// ── Clipboard Helper ──────────────────────────────────────────────
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
}
