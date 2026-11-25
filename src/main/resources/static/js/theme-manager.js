/**
 * JOBPORTAL Global Theme Manager
 * Supports: Light, Dark, System Default
 * Persists in localStorage and handles system dark mode events with zero FOUC
 */

(function () {
    const STORAGE_KEY = 'jobportal_theme';

    // 1. Get stored or default theme ('light' | 'dark' | 'system')
    function getStoredTheme() {
        return localStorage.getItem(STORAGE_KEY) || 'system';
    }

    // 2. Resolve effective theme based on user choice & OS preference
    function resolveEffectiveTheme(theme) {
        if (theme === 'system') {
            return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
                ? 'dark'
                : 'light';
        }
        return theme;
    }

    // 3. Apply theme to HTML DOM
    function applyTheme(theme, save = true) {
        const effective = resolveEffectiveTheme(theme);
        const root = document.documentElement;

        if (effective === 'dark') {
            root.classList.add('dark');
            root.setAttribute('data-theme', 'dark');
        } else {
            root.classList.remove('dark');
            root.setAttribute('data-theme', 'light');
        }

        if (save) {
            localStorage.setItem(STORAGE_KEY, theme);
        }

        updateToggleUI(theme, effective);

        // Notify other components if needed
        window.dispatchEvent(new CustomEvent('jobportal-theme-changed', {
            detail: { theme, effective }
        }));
    }

    // 4. Update the visual indicators on Navbar Theme Toggle
    function updateToggleUI(selectedTheme, effectiveTheme) {
        const currentIconContainer = document.getElementById('themeCurrentIcon');
        if (currentIconContainer) {
            let iconSvg = '';
            if (effectiveTheme === 'dark') {
                iconSvg = '<i data-lucide="moon" class="w-[18px] h-[18px] text-blue-400"></i>';
            } else {
                iconSvg = '<i data-lucide="sun" class="w-[18px] h-[18px] text-amber-500"></i>';
            }
            currentIconContainer.innerHTML = iconSvg;
            if (typeof lucide !== 'undefined') {
                lucide.createIcons();
            }
        }

        // Update active checkmarks in dropdown menu if open
        const options = ['light', 'dark', 'system'];
        options.forEach(opt => {
            const el = document.getElementById('theme-opt-' + opt);
            if (el) {
                if (selectedTheme === opt) {
                    el.classList.add('bg-blue-50', 'text-blue-600', 'font-bold', 'dark:bg-blue-900/30', 'dark:text-blue-400');
                } else {
                    el.classList.remove('bg-blue-50', 'text-blue-600', 'font-bold', 'dark:bg-blue-900/30', 'dark:text-blue-400');
                }
            }
        });
    }

    // 5. Expose Global Helper Methods
    window.JobPortalTheme = {
        getTheme: getStoredTheme,
        setTheme: function (theme) {
            applyTheme(theme, true);
            closeThemeDropdown();
        },
        toggleQuick: function () {
            const current = getStoredTheme();
            const next = current === 'dark' ? 'light' : 'dark';
            applyTheme(next, true);
        }
    };

    // 6. Listen to OS system changes if user is on 'system' mode
    if (window.matchMedia) {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function (e) {
            if (getStoredTheme() === 'system') {
                applyTheme('system', false);
            }
        });
    }

    // 7. Initial sync when DOM is loaded
    document.addEventListener('DOMContentLoaded', function () {
        const current = getStoredTheme();
        const effective = resolveEffectiveTheme(current);
        updateToggleUI(current, effective);
    });

    // 8. Run immediate check before first paint
    applyTheme(getStoredTheme(), false);
})();

// Helper to open/close theme dropdown
function toggleThemeDropdown(event) {
    if (event) event.stopPropagation();
    const dropdown = document.getElementById('themeDropdownMenu');
    const userMenu = document.getElementById('userDropdownContent');
    if (userMenu) userMenu.classList.add('hidden');
    if (dropdown) {
        dropdown.classList.toggle('hidden');
    }
}

function closeThemeDropdown() {
    const dropdown = document.getElementById('themeDropdownMenu');
    if (dropdown) {
        dropdown.classList.add('hidden');
    }
}

// Helper to open/close user dropdown
function toggleUserDropdown(event) {
    if (event) event.stopPropagation();
    const userMenu = document.getElementById('userDropdownContent');
    const themeDropdown = document.getElementById('themeDropdownMenu');
    if (themeDropdown) themeDropdown.classList.add('hidden');
    if (userMenu) {
        userMenu.classList.toggle('hidden');
        if (!userMenu.classList.contains('hidden') && typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
    }
}

function closeUserDropdown() {
    const userMenu = document.getElementById('userDropdownContent');
    if (userMenu) {
        userMenu.classList.add('hidden');
    }
}

// Helper to open/close mobile navigation drawer
function toggleMobileMenu() {
    const drawer = document.getElementById('mobileMenuDrawer');
    if (drawer) {
        drawer.classList.toggle('hidden');
        if (!drawer.classList.contains('hidden') && typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
    }
}

// Global click listener to close dropdowns
document.addEventListener('click', function (e) {
    const themeBtn = document.getElementById('themeToggleBtn');
    const themeDropdown = document.getElementById('themeDropdownMenu');
    if (themeDropdown && !themeDropdown.classList.contains('hidden')) {
        if (themeBtn && !themeBtn.contains(e.target) && !themeDropdown.contains(e.target)) {
            closeThemeDropdown();
        }
    }

    const userBtn = document.getElementById('userDropdownBtn');
    const userMenu = document.getElementById('userDropdownContent');
    if (userMenu && !userMenu.classList.contains('hidden')) {
        if (userBtn && !userBtn.contains(e.target) && !userMenu.contains(e.target)) {
            closeUserDropdown();
        }
    }
});
