package com.xa.rv0

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Import for AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.xa.rv0.databinding.ActivityBackupRestoreBinding
import com.xa.rv0.model.BackupRestoreViewModel

class BackupRestoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupRestoreBinding
    private lateinit var viewModel: BackupRestoreViewModel

    // Launcher for creating a backup file (ACTION_CREATE_DOCUMENT)
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            viewModel.backupContacts(it)
        } ?: run {
            Snackbar.make(binding.root, "Backup cancelled.", Snackbar.LENGTH_SHORT).show()
            viewModel.setLoading(false) // Ensure loading state is reset if cancelled
        }
    }

    // Launcher for opening a backup file (ACTION_OPEN_DOCUMENT)
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            // Prompt user for confirmation before restoring
            showRestoreConfirmationDialog(it)
        } ?: run {
            Snackbar.make(binding.root, "Restore cancelled.", Snackbar.LENGTH_SHORT).show()
            viewModel.setLoading(false) // Ensure loading state is reset if cancelled
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(BackupRestoreViewModel::class.java)

        // Observe ViewModel's status messages
        viewModel.backupRestoreStatus.observe(this) { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnBackup.isEnabled = !isLoading
            binding.btnRestore.isEnabled = !isLoading
        }

        binding.btnBackup.setOnClickListener {
            viewModel.setLoading(true) // Set loading true immediately
            // Prompt user to choose a location and filename for the backup
            // Suggest a default filename like "contacts_backup.json"
            createDocumentLauncher.launch("contacts_backup.json")
        }

        binding.btnRestore.setOnClickListener {
            viewModel.setLoading(true) // Set loading true immediately
            // Prompt user to choose a backup file to restore from
            openDocumentLauncher.launch(arrayOf("application/json")) // Filter for JSON files
        }
    }

    /**
     * Shows a confirmation dialog to the user before proceeding with the restore operation.
     * Informs the user that existing data will be overwritten.
     * @param uri The URI of the selected backup file.
     */
    private fun showRestoreConfirmationDialog(uri: Uri) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Restore")
            .setMessage("Restoring from backup will OVERWRITE all existing contacts. Are you sure you want to proceed?")
            .setPositiveButton("Restore") { dialog, _ ->
                viewModel.restoreContacts(uri) // Proceed with restore if confirmed
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Snackbar.make(binding.root, "Restore cancelled.", Snackbar.LENGTH_SHORT).show()
                viewModel.setLoading(false) // Reset loading state if cancelled
                dialog.dismiss()
            }
            .show()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left, // Adjust animations as needed
                R.anim.slide_out_right // Adjust animations as needed
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
