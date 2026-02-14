const BACKEND = "http://localhost:8080/s3";
const TEN_MB = 10 * 1024 * 1024;

const dropZone = document.getElementById("dropZone");
const fileInput = document.getElementById("fileInput");
const uploadContent = document.getElementById("uploadContent");
const filePreview = document.getElementById("filePreview");
const uploadButton = document.getElementById("uploadButton");
const progressSection = document.getElementById("progressSection");
const successMessage = document.getElementById("successMessage");
const removeFile = document.getElementById("removeFile");
const uploadAnother = document.getElementById("uploadAnother");

let selectedFile = null;

function init() {
    setupEventListeners();
}

function setupEventListeners() {
    dropZone.addEventListener("click", (e) => {
        if (!e.target.closest(".btn-remove") && !selectedFile) {
            fileInput.click();
        }
    });

    fileInput.addEventListener("change", handleFileSelect);

    dropZone.addEventListener("dragover", handleDragOver);
    dropZone.addEventListener("dragleave", handleDragLeave);
    dropZone.addEventListener("drop", handleDrop);

    removeFile.addEventListener("click", (e) => {
        e.stopPropagation();
        resetUI();
    });

    uploadButton.addEventListener("click", uploadFile);

    uploadAnother.addEventListener("click", resetUI);
}

function handleDragOver(e) {
    e.preventDefault();
    e.stopPropagation();
    dropZone.classList.add("drag-over");
}

function handleDragLeave(e) {
    e.preventDefault();
    e.stopPropagation();
    dropZone.classList.remove("drag-over");
}

function handleDrop(e) {
    e.preventDefault();
    e.stopPropagation();
    dropZone.classList.remove("drag-over");

    const files = e.dataTransfer.files;
    if (files.length > 0) {
        handleFile(files[0]);
    }
}

function handleFileSelect(e) {
    const files = e.target.files;
    if (files.length > 0) {
        handleFile(files[0]);
    }
}

function handleFile(file) {
    selectedFile = file;
    displayFilePreview(file);
}

function displayFilePreview(file) {
    document.getElementById("fileName").textContent = file.name;
    document.getElementById("fileSize").textContent = formatFileSize(file.size);

    uploadContent.classList.add("hidden");
    filePreview.classList.remove("hidden");
    uploadButton.classList.remove("hidden");
    
    dropZone.style.cursor = "default";
}

function resetUI() {
    selectedFile = null;
    fileInput.value = "";

    uploadContent.classList.remove("hidden");
    filePreview.classList.add("hidden");
    uploadButton.classList.add("hidden");
    progressSection.classList.add("hidden");
    successMessage.classList.add("hidden");

    resetProgress();
    
    dropZone.style.cursor = "pointer";
}

async function uploadFile() {
    if (!selectedFile) {
        return;
    }

    uploadButton.classList.add("hidden");
    progressSection.classList.remove("hidden");

    document.getElementById("totalSize").textContent = formatFileSize(selectedFile.size);

    try {
        if (selectedFile.size <= TEN_MB) {
            await uploadSmallFile(selectedFile);
        } else {
            await uploadMultipart(selectedFile);
        }
    } catch (error) {
        console.error("Upload error:", error);
        setStatus("Upload failed ❌", "error");
        
        setTimeout(() => {
            uploadButton.classList.remove("hidden");
            progressSection.classList.add("hidden");
        }, 2000);
    }
}

async function uploadSmallFile(file) {
    setStatus("Uploading file...");

    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${BACKEND}/upload`, {
        method: "POST",
        body: formData
    });

    if (!response.ok) {
        throw new Error("Upload failed");
    }

    let progress = 0;
    const interval = setInterval(() => {
        progress += 10;
        if (progress <= 100) {
            updateProgress(file.size * (progress / 100), file.size);
        } else {
            clearInterval(interval);
        }
    }, 50);

    setTimeout(() => {
        clearInterval(interval);
        updateProgress(file.size, file.size);
        showSuccess();
    }, 600);
}

async function uploadMultipart(file) {
    setStatus("Initializing multipart upload...");

    // 1️⃣ Init
    const initResponse = await fetch(`${BACKEND}/upload/multipart`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            fileName: file.name,
            fileSize: file.size
        })
    });

    if (!initResponse.ok) {
        throw new Error("Failed to initialize upload");
    }

    const initData = await initResponse.json();
    const { uploadId, key, partSize, presignedUrls } = initData;

    let uploadedBytes = 0;
    const parts = [];

    setStatus(`Uploading ${presignedUrls.length} parts...`);

    for (const part of presignedUrls) {
        const start = (part.partNumber - 1) * partSize;
        const end = Math.min(start + partSize, file.size);
        const blob = file.slice(start, end);

        const uploadResponse = await fetch(part.url, {
            method: "PUT",
            body: blob
        });

        if (!uploadResponse.ok) {
            throw new Error(`Part ${part.partNumber} failed`);
        }

        const eTag = uploadResponse.headers.get("ETag");
        parts.push({
            partNumber: part.partNumber,
            eTag: eTag.replaceAll('"', '')
        });

        uploadedBytes += blob.size;
        updateProgress(uploadedBytes, file.size);
        
        setStatus(`Uploading part ${part.partNumber}/${presignedUrls.length}...`);
    }

    setStatus("Finalizing upload...");

    const completeResponse = await fetch(`${BACKEND}/upload/multipart/complete`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            uploadId: uploadId,
            key: key,
            parts: parts
        })
    });

    if (!completeResponse.ok) {
        throw new Error("Failed to complete upload");
    }

    showSuccess();
}

function updateProgress(uploaded, total) {
    const percent = Math.round((uploaded / total) * 100);
    const progressBar = document.getElementById("progressBar");
    const progressPercent = document.getElementById("progressPercent");
    const uploadedSize = document.getElementById("uploadedSize");

    progressBar.style.width = percent + "%";
    progressPercent.textContent = percent + "%";
    uploadedSize.textContent = formatFileSize(uploaded);
}

function resetProgress() {
    updateProgress(0, 100);
    document.getElementById("totalSize").textContent = "0 MB";
}

function setStatus(text, type = "info") {
    const statusLabel = document.getElementById("statusLabel");
    statusLabel.textContent = text;
    
    if (type === "error") {
        statusLabel.style.color = "var(--error)";
    } else {
        statusLabel.style.color = "var(--text-secondary)";
    }
}

function showSuccess() {
    progressSection.classList.add("hidden");
    filePreview.classList.add("hidden");
    dropZone.classList.add("hidden");
    
    successMessage.classList.remove("hidden");
}

function formatFileSize(bytes) {
    if (bytes === 0) return "0 Bytes";
    
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
}

init();