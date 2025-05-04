// JavaScript for file upload with progress tracking

document.addEventListener('DOMContentLoaded', function() {
    const uploadForm = document.getElementById('uploadForm');
    const fileInput = document.getElementById('fileInput');
    const uploadArea = document.getElementById('uploadArea');
    const progressContainer = document.getElementById('progressContainer');
    const progressBar = document.getElementById('progressBar');
    const progressText = document.getElementById('progressText');
    const successMessage = document.getElementById('successMessage');
    const uploadTimeInfo = document.getElementById('uploadTimeInfo');

    // Handle drag and drop events
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        uploadArea.addEventListener(eventName, preventDefaults, false);
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    ['dragenter', 'dragover'].forEach(eventName => {
        uploadArea.addEventListener(eventName, highlight, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        uploadArea.addEventListener(eventName, unhighlight, false);
    });

    function highlight() {
        uploadArea.classList.add('dragover');
    }

    function unhighlight() {
        uploadArea.classList.remove('dragover');
    }

    // Handle file drop
    uploadArea.addEventListener('drop', handleDrop, false);

    function handleDrop(e) {
        const dt = e.dataTransfer;
        const files = dt.files;

        if (files.length > 0) {
            fileInput.files = files;
            uploadFile(files[0]);
        }
    }

    // Handle file selection via input
    fileInput.addEventListener('change', function() {
        if (this.files.length > 0) {
            uploadFile(this.files[0]);
        }
    });

    // Click on upload area to trigger file input
    uploadArea.addEventListener('click', function() {
        fileInput.click();
    });

    // File upload function with progress tracking
    function uploadFile(file) {
        const startTime = new Date().getTime();

        // Show progress container
        progressContainer.style.display = 'block';
        progressBar.style.width = '0%';
        progressText.textContent = '0%';

        const formData = new FormData();
        formData.append('file', file);

        // Get selected storage type
        const storageType = document.querySelector('input[name="storageType"]:checked').value;
        formData.append('useCloudStorage', storageType === 'cloud');

        const xhr = new XMLHttpRequest();

        // Track upload progress
        xhr.upload.addEventListener('progress', function(e) {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                progressBar.style.width = percentComplete + '%';
                progressText.textContent = percentComplete + '%';

                // Calculate upload speed
                const currentTime = new Date().getTime();
                const elapsedTime = (currentTime - startTime) / 1000; // in seconds
                const uploadSpeed = e.loaded / elapsedTime; // bytes per second

                // Format speed for display
                let speedText;
                if (uploadSpeed > 1024 * 1024) {
                    speedText = (uploadSpeed / (1024 * 1024)).toFixed(2) + ' MB/s';
                } else if (uploadSpeed > 1024) {
                    speedText = (uploadSpeed / 1024).toFixed(2) + ' KB/s';
                } else {
                    speedText = uploadSpeed.toFixed(2) + ' B/s';
                }

                document.getElementById('uploadSpeed').textContent = speedText;
            }
        });

        // Handle upload completion
        xhr.addEventListener('load', function() {
            if (xhr.status === 200) {
                const response = JSON.parse(xhr.responseText);

                if (response.status === 'success') {
                    // Calculate total upload time
                    const endTime = new Date().getTime();
                    const totalTime = (endTime - startTime) / 1000; // in seconds

                    // Format file size
                    const fileSize = formatFileSize(response.fileInfo.size);

                    // Display success message
                    successMessage.style.display = 'block';
                    uploadTimeInfo.innerHTML = `
                        <strong>File:</strong> ${response.fileInfo.originalFilename}<br>
                        <strong>Size:</strong> ${fileSize}<br>
                        <strong>Upload time:</strong> ${totalTime.toFixed(2)} seconds
                    `;

                    // Reset progress
                    progressBar.style.width = '100%';
                    progressText.textContent = 'Upload Complete!';

                    // Redirect to list page after 3 seconds
                    setTimeout(function() {
                        window.location.href = '/api/files/list';
                    }, 3000);
                } else {
                    alert('Upload failed: ' + response.message);
                }
            } else {
                alert('Upload failed. Please try again.');
            }
        });

        // Handle errors
        xhr.addEventListener('error', function() {
            showErrorMessage('Upload failed. Please check your connection and try again.');
        });

        xhr.addEventListener('loadend', function() {
            if (xhr.status !== 200) {
                try {
                    const errorResponse = JSON.parse(xhr.responseText);
                    showErrorMessage(errorResponse.message || 'Upload failed. Please try again.');
                } catch (e) {
                    // If the response is not valid JSON or doesn't have a message
                    if (xhr.status === 413) {
                        showErrorMessage('File size exceeds the maximum allowed limit.');
                    } else {
                        showErrorMessage('Upload failed: ' + xhr.statusText);
                    }
                }
            }
        });

        function showErrorMessage(message) {
            // Create error message element
            const errorDiv = document.createElement('div');
            errorDiv.className = 'alert alert-danger';
            errorDiv.innerHTML = `<h4><i class="fas fa-exclamation-circle"></i> Upload Failed</h4>
                                <p>${message}</p>`;

            // Add it after the progress container
            progressContainer.parentNode.insertBefore(errorDiv, progressContainer.nextSibling);

            // Reset progress
            progressBar.style.width = '0%';
            progressText.textContent = 'Failed';

            // Remove the error message after 5 seconds
            setTimeout(function() {
                errorDiv.remove();
            }, 5000);
        }

        // Send the file
        xhr.open('POST', '/api/files/upload', true);
        xhr.send(formData);
    }

    // Format file size for display
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
});