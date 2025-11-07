package com.example.arfcdm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.arfcdm.databinding.ActivityMainBinding
import com.example.arfcdm.ml.ToolClassifier
import com.example.arfcdm.database.AppDatabase
import com.example.arfcdm.database.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var toolClassifier: ToolClassifier
    private lateinit var database: AppDatabase

    private var imageCapture: ImageCapture? = null
    private var isProcessing = false

    companion object {
        private const val TAG = "ToolRecognition"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar banco de dados
        database = AppDatabase.getDatabase(this)

        // Inicializar classificador
        toolClassifier = ToolClassifier(this)

        // Inicializar dados padrão
        initializeDefaultTools()

        // Verificar permissões
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Configurar botões
        binding.btnCapture.setOnClickListener {
            captureAndAnalyze()
        }

        binding.btnViewTools.setOnClickListener {
            // Navegar para lista de ferramentas
            Toast.makeText(this, "Lista de ferramentas em desenvolvimento", Toast.LENGTH_SHORT).show()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // ImageAnalysis para análise em tempo real (opcional)
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Selecionar câmera traseira
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Falha ao iniciar câmera", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndAnalyze() {
        if (isProcessing) return

        val imageCapture = imageCapture ?: return

        isProcessing = true
        binding.btnCapture.isEnabled = false
        binding.tvResult.text = "Analisando..."

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    lifecycleScope.launch {
                        analyzeImage(bitmap)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Erro ao capturar imagem: ${exception.message}", exception)
                    isProcessing = false
                    binding.btnCapture.isEnabled = true
                    binding.tvResult.text = "Erro ao capturar imagem"
                }
            }
        )
    }

    private suspend fun analyzeImage(bitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            try {
                val result = toolClassifier.classify(bitmap)

                withContext(Dispatchers.Main) {
                    if (result.confidence > 0.5f) {
                        binding.tvResult.text = "${result.label}\nConfiança: ${(result.confidence * 100).toInt()}%"

                        // Buscar informações da ferramenta no banco
                        lifecycleScope.launch {
                            val tool = database.toolDao().getToolByName(result.label)
                            if (tool != null) {
                                // Navegar para detalhes
                                val intent = Intent(this@MainActivity, ToolDetailActivity::class.java)
                                intent.putExtra("TOOL_ID", tool.id)
                                startActivity(intent)
                            }
                        }
                    } else {
                        binding.tvResult.text = "Ferramenta não reconhecida\nTente novamente"
                    }

                    isProcessing = false
                    binding.btnCapture.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao analisar imagem", e)
                withContext(Dispatchers.Main) {
                    binding.tvResult.text = "Erro ao analisar imagem"
                    isProcessing = false
                    binding.btnCapture.isEnabled = true
                }
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Rotacionar se necessário
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun initializeDefaultTools() {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = database.toolDao().getToolCount()
            if (count == 0) {
                // Adicionar ferramentas padrão
                val defaultTools = listOf(
                    Tool(
                        name = "Martelo",
                        description = "Ferramenta de percussão usada para bater pregos, quebrar objetos ou moldar materiais.",
                        usage = "Segurar pelo cabo e aplicar força de impacto na cabeça metálica.",
                        category = "Percussão"
                    ),
                    Tool(
                        name = "Alicate",
                        description = "Ferramenta manual usada para segurar, cortar ou dobrar fios e materiais.",
                        usage = "Apertar as alças para fechar as mandíbulas sobre o objeto.",
                        category = "Corte e Preensão"
                    ),
                    Tool(
                        name = "Chave de Boca",
                        description = "Ferramenta usada para apertar ou afrouxar porcas e parafusos.",
                        usage = "Encaixar a boca da chave na porca e girar no sentido desejado.",
                        category = "Aperto"
                    ),
                    Tool(
                        name = "Chave de Fenda",
                        description = "Ferramenta usada para apertar ou afrouxar parafusos com fenda reta.",
                        usage = "Encaixar a ponta na fenda do parafuso e girar.",
                        category = "Aperto"
                    ),
                    Tool(
                        name = "Chave Phillips",
                        description = "Ferramenta usada para apertar ou afrouxar parafusos com fenda cruzada.",
                        usage = "Encaixar a ponta cruzada no parafuso e girar.",
                        category = "Aperto"
                    )
                )

                defaultTools.forEach { tool ->
                    database.toolDao().insert(tool)
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissões não concedidas.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        toolClassifier.close()
    }
}