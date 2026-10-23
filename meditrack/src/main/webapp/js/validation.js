/**
 * validation.js
 * --------------
 * Lightweight client-side form validation. Server-side validation in the
 * servlets remains the source of truth (never trust the client alone) -
 * this just gives immediate feedback before a round trip.
 */

function showFieldError(input, message) {
    clearFieldError(input);
    const err = document.createElement('div');
    err.className = 'field-error';
    err.style.color = '#C0392B';
    err.style.fontSize = '0.78rem';
    err.style.marginTop = '4px';
    err.textContent = message;
    input.insertAdjacentElement('afterend', err);
    input.style.borderColor = '#C0392B';
}

function clearFieldError(input) {
    input.style.borderColor = '';
    const next = input.nextElementSibling;
    if (next && next.classList.contains('field-error')) next.remove();
}

function validateRequired(form, fieldNames) {
    let valid = true;
    fieldNames.forEach(name => {
        const input = form.querySelector(`[name="${name}"]`);
        if (!input) return;
        clearFieldError(input);
        if (!input.value || input.value.trim() === '') {
            showFieldError(input, 'This field is required.');
            valid = false;
        }
    });
    return valid;
}

function validateEmail(form, fieldName) {
    const input = form.querySelector(`[name="${fieldName}"]`);
    if (!input) return true;
    clearFieldError(input);
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(input.value)) {
        showFieldError(input, 'Enter a valid email address.');
        return false;
    }
    return true;
}

function validatePasswordMatch(form, passwordField, confirmField) {
    const pw = form.querySelector(`[name="${passwordField}"]`);
    const cpw = form.querySelector(`[name="${confirmField}"]`);
    if (!pw || !cpw) return true;
    clearFieldError(cpw);
    if (pw.value !== cpw.value) {
        showFieldError(cpw, 'Passwords do not match.');
        return false;
    }
    return true;
}

function validatePositiveNumber(form, fieldName) {
    const input = form.querySelector(`[name="${fieldName}"]`);
    if (!input) return true;
    clearFieldError(input);
    const val = parseFloat(input.value);
    if (isNaN(val) || val <= 0) {
        showFieldError(input, 'Enter a quantity greater than 0.');
        return false;
    }
    return true;
}

// --- Wire up known forms on page load ---
document.addEventListener('DOMContentLoaded', () => {
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', (e) => {
            const ok1 = validateRequired(registerForm, ['fullName', 'email', 'password', 'role']);
            const ok2 = validateEmail(registerForm, 'email');
            if (!ok1 || !ok2) e.preventDefault();
        });
    }

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            const ok = validateRequired(loginForm, ['email', 'password']);
            if (!ok) e.preventDefault();
        });
    }

    const hospitalForm = document.getElementById('hospitalRegisterForm');
    if (hospitalForm) {
        hospitalForm.addEventListener('submit', (e) => {
            const ok = validateRequired(hospitalForm, ['name', 'address', 'latitude', 'longitude']);
            if (!ok) {
                e.preventDefault();
                alert('Please click on the map to select the hospital location before submitting.');
            }
        });
    }

    const wasteForm = document.getElementById('wasteSubmitForm');
    if (wasteForm) {
        wasteForm.addEventListener('submit', (e) => {
            const ok1 = validateRequired(wasteForm, ['wasteType', 'quantityKg']);
            const ok2 = validatePositiveNumber(wasteForm, 'quantityKg');
            if (!ok1 || !ok2) e.preventDefault();
        });
    }
});
