document.addEventListener('DOMContentLoaded', () => {
    // --- 1. Theme Toggle Logic (Dark / Light) ---
    const themeToggleBtn = document.getElementById('themeToggle');
    const themeIcon = document.getElementById('themeIcon');
    const htmlElement = document.documentElement;

    // Load saved theme or system preference
    const savedTheme = localStorage.getItem('theme') ||
        (window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');

    setTheme(savedTheme);

    themeToggleBtn.addEventListener('click', () => {
        const currentTheme = htmlElement.getAttribute('data-bs-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        setTheme(newTheme);
    });

    function setTheme(theme) {
        htmlElement.setAttribute('data-bs-theme', theme);
        localStorage.setItem('theme', theme);
        if (theme === 'dark') {
            themeIcon.className = 'fa-solid fa-sun';
        } else {
            themeIcon.className = 'fa-solid fa-moon';
        }
    }

    // --- 2. File Upload & Drag and Drop Handling ---
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const fileSelectedInfo = document.getElementById('fileSelectedInfo');
    const fileNameDisplay = document.getElementById('fileNameDisplay');
    const fileSizeDisplay = document.getElementById('fileSizeDisplay');
    const removeFileBtn = document.getElementById('removeFileBtn');
    const fileTypeIcon = document.getElementById('fileTypeIcon');

    // Drag effects
    ['dragenter', 'dragover'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.add('dragover');
        }, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.remove('dragover');
        }, false);
    });

    // Handle File Selection
    fileInput.addEventListener('change', updateFileInfo);

    function updateFileInfo() {
        if (fileInput.files && fileInput.files[0]) {
            const file = fileInput.files[0];
            fileNameDisplay.textContent = file.name;
            fileSizeDisplay.textContent = formatBytes(file.size);

            // Set Icon based on extension
            const ext = file.name.split('.').pop().toLowerCase();
            if (ext === 'pdf') fileTypeIcon.className = 'fa-solid fa-file-pdf text-danger';
            else if (ext === 'docx') fileTypeIcon.className = 'fa-solid fa-file-word text-primary';
            else if (ext === 'xlsx') fileTypeIcon.className = 'fa-solid fa-file-excel text-success';
            else if (ext === 'pptx') fileTypeIcon.className = 'fa-solid fa-file-powerpoint text-warning';
            else fileTypeIcon.className = 'fa-solid fa-file-lines';

            fileSelectedInfo.style.display = 'flex';
        } else {
            resetFileInput();
        }
    }

    removeFileBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        resetFileInput();
    });

    function resetFileInput() {
        fileInput.value = '';
        fileSelectedInfo.style.display = 'none';
    }

    function formatBytes(bytes, decimals = 2) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    }

    // --- 3. Error Div Formatting Fix ---
    // If Thymeleaf renders error, turn standard div into nicely styled alert box
    const errorDiv = document.querySelector('.error');
    if (errorDiv && errorDiv.textContent.trim() !== '') {
        errorDiv.classList.add('error-card', 'text-danger', 'fw-medium');
        errorDiv.innerHTML = `<i class="fa-solid fa-circle-exclamation fs-5"></i> <span>${errorDiv.textContent}</span>`;
    }

    // --- 4. Markdown Textarea Counters & Copy Button ---
    const markdownArea = document.getElementById('markdownArea');
    const copyBtn = document.getElementById('copyBtn');
    const lineCount = document.getElementById('lineCount');
    const wordCount = document.getElementById('wordCount');

    if (markdownArea) {
        const updateCounters = () => {
            const text = markdownArea.value;
            const lines = text ? text.split('\n').length : 0;
            const words = text.trim() ? text.trim().split(/\s+/).length : 0;
            if (lineCount) lineCount.innerHTML = `<i class="fa-solid fa-bars me-1"></i>${lines} Líneas`;
            if (wordCount) wordCount.innerHTML = `<i class="fa-solid fa-font me-1"></i>${words} Palabras`;
        };

        markdownArea.addEventListener('input', updateCounters);
        updateCounters(); // initial count

        // Copy to Clipboard
        if (copyBtn) {
            copyBtn.addEventListener('click', () => {
                markdownArea.select();
                // execute copy fallback for iframe/cross-browser compatibility
                try {
                    document.execCommand('copy');
                    showToast('¡Markdown copiado al portapapeles!');
                } catch (err) {
                    if (navigator.clipboard) {
                        navigator.clipboard.writeText(markdownArea.value)
                            .then(() => showToast('¡Markdown copiado al portapapeles!'))
                            .catch(() => showToast('Error al copiar'));
                    }
                }
            });
        }
    }

    // Toast Helper
    function showToast(msg) {
        const toast = document.getElementById('toast');
        const toastMsg = document.getElementById('toastMsg');
        if (toast && toastMsg) {
            toastMsg.textContent = msg;
            toast.classList.add('show');
            setTimeout(() => {
                toast.classList.remove('show');
            }, 3000);
        }
    }
});