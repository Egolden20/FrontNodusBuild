package com.example.frontnodus.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.frontnodus.adapter.CommentAdapter
import com.example.frontnodus.databinding.FragmentCommentsBinding
import com.example.frontnodus.model.Comment

class CommentsFragment : Fragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var commentAdapter: CommentAdapter
    private val comments = mutableListOf<Comment>()

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
        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        // Static comments data
        comments.clear()
        comments.addAll(listOf(
            Comment(
                1,
                "Frank Justo",
                "Verificar la mezcla, revisar no iniciar la fundación",
                "Hace 5 horas",
                "Resuelto"
            ),
            Comment(
                2,
                "Frank Justo",
                "Verificar la mezcla antes de iniciar la fundación",
                "Hace 3 horas",
                "Pendiente"
            )
        ))

        commentAdapter = CommentAdapter(comments) { comment ->
            Toast.makeText(requireContext(), "Comentario de: ${comment.userName}", Toast.LENGTH_SHORT).show()
        }

        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
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
        val newComment = Comment(
            id = comments.size + 1,
            userName = "Usuario Actual",
            commentText = commentText,
            timeAgo = "Justo ahora",
            status = "Pendiente"
        )
        
        comments.add(0, newComment)
        commentAdapter.notifyItemInserted(0)
        binding.rvComments.scrollToPosition(0)
        
        Toast.makeText(requireContext(), "Comentario agregado", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
