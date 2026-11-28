package com.pusacoffee.pusatrack.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pusacoffee.pusatrack.databinding.BottomSheetExportReadyBinding
import com.pusacoffee.pusatrack.utils.IntentHelper
import java.io.File

class ExportReadyBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetExportReadyBinding? = null
    private val binding get() = _binding!!

    private var sourceFile: File? = null
    private var fileName: String = "Laporan_PUSATRACK.xlsx"

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(IntentHelper.MIME_XLSX)
    ) { destUri ->
        val file = sourceFile
        if (destUri != null && file != null && file.exists()) {
            try {
                requireContext().contentResolver.openOutputStream(destUri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                Toast.makeText(
                    requireContext(),
                    "Laporan berhasil disimpan ke perangkat.",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Gagal menyimpan file ke lokasi yang dipilih.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetExportReadyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val path = arguments?.getString(ARG_FILE_PATH).orEmpty()
        fileName = arguments?.getString(ARG_FILE_NAME).orEmpty().ifBlank { "Laporan_PUSATRACK.xlsx" }
        val sheetCount = arguments?.getInt(ARG_SHEET_COUNT, 0) ?: 0
        val sizeLabel = arguments?.getString(ARG_FILE_SIZE).orEmpty()

        sourceFile = if (path.isNotEmpty()) File(path) else null

        binding.tvFileName.text = fileName
        binding.tvFileMeta.text = "$sheetCount sheet · $sizeLabel"

        binding.btnSaveToDevice.setOnClickListener {
            createDocumentLauncher.launch(fileName)
        }

        binding.btnShareFile.setOnClickListener {
            val file = sourceFile
            if (file != null && file.exists()) {
                IntentHelper.shareXlsxFile(requireContext(), file)
            } else {
                Toast.makeText(requireContext(), "File laporan tidak ditemukan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ExportReadyBottomSheet"
        private const val ARG_FILE_PATH = "arg_file_path"
        private const val ARG_FILE_NAME = "arg_file_name"
        private const val ARG_SHEET_COUNT = "arg_sheet_count"
        private const val ARG_FILE_SIZE = "arg_file_size"

        fun newInstance(
            filePath: String,
            fileName: String,
            sheetCount: Int,
            fileSizeLabel: String
        ): ExportReadyBottomSheet {
            return ExportReadyBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILE_PATH, filePath)
                    putString(ARG_FILE_NAME, fileName)
                    putInt(ARG_SHEET_COUNT, sheetCount)
                    putString(ARG_FILE_SIZE, fileSizeLabel)
                }
            }
        }
    }
}
