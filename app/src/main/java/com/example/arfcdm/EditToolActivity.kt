package com.example.arfcdm

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.arfcdm.database.AppDatabase
import com.example.arfcdm.database.Tool
import com.example.arfcdm.databinding.ActivityEditToolBinding
import kotlinx.coroutines.launch

class EditToolActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditToolBinding
    private lateinit var database: AppDatabase
    private var toolId: Long = -1
    private var currentTool: Tool? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditToolBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        toolId = intent.getLongExtra("TOOL_ID", -1)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editar Ferramenta"

        loadToolData()

        binding.btnSave.setOnClickListener {
            saveTool()
        }
    }

    private fun loadToolData() {
        lifecycleScope.launch {
            currentTool = database.toolDao().getToolById(toolId)
            currentTool?.let { tool ->
                binding.etToolName.setText(tool.name)
                binding.etToolCategory.setText(tool.category)
                binding.etToolDescription.setText(tool.description)
                binding.etToolUsage.setText(tool.usage)
            }
        }
    }

    private fun saveTool() {
        val name = binding.etToolName.text.toString().trim()
        val category = binding.etToolCategory.text.toString().trim()
        val description = binding.etToolDescription.text.toString().trim()
        val usage = binding.etToolUsage.text.toString().trim()

        if (name.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            currentTool?.let { tool ->
                val updatedTool = tool.copy(
                    name = name,
                    category = category,
                    description = description,
                    usage = usage,
                    updatedAt = System.currentTimeMillis()
                )
                database.toolDao().update(updatedTool)

                Toast.makeText(this@EditToolActivity, "Ferramenta atualizada!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}