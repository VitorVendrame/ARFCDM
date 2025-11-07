package com.example.arfcdm

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.arfcdm.database.AppDatabase
import com.example.arfcdm.databinding.ActivityToolDetailBinding
import kotlinx.coroutines.launch

class ToolDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityToolDetailBinding
    private lateinit var database: AppDatabase
    private var toolId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        toolId = intent.getLongExtra("TOOL_ID", -1)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadToolDetails()

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, EditToolActivity::class.java)
            intent.putExtra("TOOL_ID", toolId)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadToolDetails()
    }

    private fun loadToolDetails() {
        lifecycleScope.launch {
            val tool = database.toolDao().getToolById(toolId)
            tool?.let {
                binding.tvToolName.text = it.name
                binding.tvToolCategory.text = "Categoria: ${it.category}"
                binding.tvToolDescription.text = it.description
                binding.tvToolUsage.text = "Como usar:\n${it.usage}"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}