// JavaScript for the file list page

document.addEventListener('DOMContentLoaded', function () {
    // Handle file deletion
    const deleteButtons = document.querySelectorAll('.delete-file');

    deleteButtons.forEach(button => {
        button.addEventListener('click', function () {
            const fileId = this.getAttribute('data-id');
            const fileName = this.getAttribute('data-name');

            if (confirm(`Are you sure you want to delete "${fileName}"?`)) {
                deleteFile(fileId);
            }
        });
    });

    function deleteFile(fileId) {
        const xhr = new XMLHttpRequest();
        xhr.open('DELETE', `/api/files/${fileId}`, true);

        xhr.onload = function () {
            if (xhr.status === 200) {
                const response = JSON.parse(xhr.responseText);

                if (response.status === 'success') {
                    // Remove file item from the list
                    const fileItem = document.querySelector(`.file-item[data-id="${fileId}"]`);

                    if (fileItem) {
                        fileItem.remove();

                        // Show success message
                        showMessage('File deleted successfully', 'success');
                    }
                } else {
                    showMessage('Failed to delete file: ' + response.message, 'error');
                }
            } else {
                showMessage('Failed to delete file. Please try again.', 'error');
            }
        };

        xhr.onerror = function () {
            showMessage('Failed to delete file. Please check your connection.', 'error');
        };

        xhr.send();
    }

    // Handle file edit
    const editButtons = document.querySelectorAll('.edit-file');

    editButtons.forEach(button => {
        button.addEventListener('click', function () {
            const fileId = this.getAttribute('data-id');
            const fileName = this.getAttribute('data-name');

            // Implement edit file functionality
            // This could open a modal for editing the file name or uploading a new version
            const newName = prompt('Enter new name for the file:', fileName);

            if (newName !== null && newName.trim() !== '') {
                // Here you would implement the actual update
                // For now, we'll just show a message
                showMessage('File edit functionality to be implemented', 'info');
            }
        });
    });

    // Helper function to show messages
    function showMessage(message, type) {
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type}`;
        alertDiv.textContent = message;

        const container = document.querySelector('.container');
        container.insertBefore(alertDiv, container.firstChild);

        // Remove message after 3 seconds
        setTimeout(() => {
            alertDiv.remove();
        }, 3000);
    }

    // Format date to a more readable format
    const dateElements = document.querySelectorAll('.file-date');

    dateElements.forEach(element => {
        const dateString = element.textContent;
        const date = new Date(dateString);

        if (!isNaN(date.getTime())) {
            element.textContent = formatDate(date);
        }
    });

    function formatDate(date) {
        const options = {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        };

        return date.toLocaleDateString('en-US', options);
    }

    // Format file size
    const sizeElements = document.querySelectorAll('.file-size');

    sizeElements.forEach(element => {
        const size = parseInt(element.textContent);

        if (!isNaN(size)) {
            element.textContent = formatFileSize(size);
        }
    });

    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
});