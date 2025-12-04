package com.example.frontnodus.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontnodus.ui.adapters.CommentAdapter
import com.example.frontnodus.databinding.FragmentCommentsBinding
import com.example.frontnodus.domain.models.Comment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.example.frontnodus.data.repository.TaskRepository
import org.json.JSONArray
import com.example.frontnodus.utils.DateUtils

class CommentsFragment : Fragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var commentAdapter: CommentAdapter
    private val comments = mutableListOf<Comment>()
    private val taskRepository: TaskRepository by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCommentsFromBackend()
        setupButtons()
    }

    private fun applyCommentsJSONArray(arr: org.json.JSONArray?) {
        if (arr == null) return
        comments.clear()
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            val text = c.optString("text", "")
            val createdAt = c.optString("createdAt", "")
            val commenter = c.optJSONObject("commenter")
            val profile = commenter?.optJSONObject("profile")
            val userName = profile?.optString("firstName")?.plus(" ")?.plus(profile.optString("lastName"))
                ?: commenter?.optString("email") ?: "Usuario"
            val cid = c.optString("id", "${i + 1}")
            comments.add(Comment(cid, userName, text, DateUtils.formatToDate(createdAt) ?: createdAt))
        }
        if (::commentAdapter.isInitialized) {
            commentAdapter.notifyDataSetChanged()
        }
    }

    private fun loadCommentsFromBackend() {
        comments.clear()
        val taskId = arguments?.getString("TASK_ID")
        if (taskId.isNullOrBlank()) {
            commentAdapter = CommentAdapter(comments,
                onCommentClick = { comment -> Toast.makeText(requireContext(), "Comentario de: ${comment.userName}", Toast.LENGTH_SHORT).show() },
                onEditClick = { /* noop */ },
                onDeleteClick = { /* noop */ }
            )
            binding.rvComments.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = commentAdapter
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val taskJson = taskRepository.getTaskById(taskId)
                val arr: JSONArray? = taskJson?.optJSONArray("comments")
                applyCommentsJSONArray(arr)
            } catch (e: Exception) {
                // ignore
            }

            commentAdapter = CommentAdapter(comments,
                onCommentClick = { comment ->
                    // comments are editable via the Edit button; main click can show options
                    Toast.makeText(requireContext(), "Usa Editar o Eliminar para modificar el comentario", Toast.LENGTH_SHORT).show()
                },
                onEditClick = { comment ->
                    // show edit dialog
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Editar comentario")
                    val input = EditText(requireContext())
                    input.setText(comment.commentText)
                    input.setPadding(50, 40, 50, 40)
                    builder.setView(input)
                    builder.setPositiveButton("Guardar") { dialog, _ ->
                        val newText = input.text.toString().trim()
                        if (newText.isNotEmpty() && newText != comment.commentText) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                try {
                                    val resp = taskRepository.editTaskComment(taskId ?: "", comment.id, newText)
                                    val updated = resp?.optJSONArray("comments")
                                    if (updated != null) {
                                        applyCommentsJSONArray(updated)
                                        Toast.makeText(requireContext(), "Comentario actualizado", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // fallback: refresh full task
                                        val fresh = taskRepository.getTaskById(taskId ?: "")
                                        val freshArr = fresh?.optJSONArray("comments")
                                        applyCommentsJSONArray(freshArr)
                                    }
                                } catch (e: Exception) {
                                    val msg = e.message ?: "Error editando comentario"
                                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        dialog.dismiss()
                    }
                    builder.setNegativeButton("Cancelar") { d, _ -> d.cancel() }
                    builder.show()
                },
                onDeleteClick = { comment ->
                    val confirm = AlertDialog.Builder(requireContext())
                    confirm.setTitle("Eliminar comentario")
                    confirm.setMessage("¿Estás seguro que quieres eliminar este comentario?")
                    confirm.setPositiveButton("Sí") { dialog, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val resp = taskRepository.deleteTaskComment(taskId ?: "", comment.id)
                                val updated = resp?.optJSONArray("comments")
                                if (updated != null) {
                                    applyCommentsJSONArray(updated)
                                    Toast.makeText(requireContext(), "Comentario eliminado", Toast.LENGTH_SHORT).show()
                                } else {
                                    val fresh = taskRepository.getTaskById(taskId ?: "")
                                    val freshArr = fresh?.optJSONArray("comments")
                                    applyCommentsJSONArray(freshArr)
                                }
                            } catch (e: Exception) {
                                val msg = e.message ?: "Error eliminando comentario"
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                        dialog.dismiss()
                    }
                    confirm.setNegativeButton("Cancelar") { d, _ -> d.cancel() }
                    confirm.show()
                }
            )

            binding.rvComments.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = commentAdapter
            }
        }
    }

    private fun setupButtons() {
        binding.btnAddComment.setOnClickListener {
            showAddCommentDialog()
        }

        binding.tvViewAllComments.setOnClickListener {
            Toast.makeText(requireContext(), "Ver todos los comentarios", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAddCommentDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.hint = "Escribe tu comentario..."
        input.setPadding(50, 40, 50, 40)
        
        builder.setTitle("Agregar Comentario")
        builder.setView(input)
        
        builder.setPositiveButton("Agregar") { dialog, _ ->
            val commentText = input.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addNewComment(commentText)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "El comentario no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        
        builder.show()
    }
    
    private fun addNewComment(commentText: String) {
        val taskId = arguments?.getString("TASK_ID")
        if (taskId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "No se encontró la tarea", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
                try {
                val resp = taskRepository.addTaskComment(taskId, commentText)
                val updatedComments = resp?.optJSONArray("comments")
                if (updatedComments != null) {
                    applyCommentsJSONArray(updatedComments)
                    binding.rvComments.scrollToPosition(0)
                    Toast.makeText(requireContext(), "Comentario agregado", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            } catch (e: Exception) {
                // ignore and fall back to local insert
            }

            // fallback local insert if network fails
            val newComment = Comment(
                id = "${comments.size + 1}",
                userName = "Usuario Actual",
                commentText = commentText,
                timeAgo = "Justo ahora"
            )

            comments.add(0, newComment)
            commentAdapter.notifyItemInserted(0)
            binding.rvComments.scrollToPosition(0)
            Toast.makeText(requireContext(), "Comentario agregado (offline)", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
