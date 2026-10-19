// =====================================================
// UI POLISH: hiệu ứng bằng JavaScript cho các trang job
// Đặt file này ở: src/main/resources/static/js/ui-polish.js
// Nhúng ở cuối <body>: <script th:src="@{/js/ui-polish.js}"></script>
// =====================================================
(function () {
    'use strict';

    // Nếu người dùng tắt hiệu ứng trong hệ điều hành thì bỏ qua phần chuyển động
    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // ---------- 1) Hiệu ứng gợn sóng khi bấm nút ----------
    // Các phần tử được coi là "nút"
    var RIPPLE_SELECTOR = '.btn, .slide-cta, .discovery-search-button, .page-link, .cd-search button, button[type="submit"]';

    document.addEventListener('click', function (event) {
        if (reduceMotion) return;

        // Tìm nút gần nhất mà người dùng vừa bấm
        var target = event.target.closest(RIPPLE_SELECTOR);
        if (!target) return;

        // Tạo vòng tròn bắt đầu từ đúng điểm bấm
        var rect = target.getBoundingClientRect();
        var size = Math.max(rect.width, rect.height) * 2;
        var ripple = document.createElement('span');

        ripple.className = 'ripple';
        ripple.style.width = ripple.style.height = size + 'px';
        ripple.style.left = (event.clientX - rect.left - size / 2) + 'px';
        ripple.style.top = (event.clientY - rect.top - size / 2) + 'px';

        target.appendChild(ripple);

        // Xóa vòng tròn sau khi chạy xong
        setTimeout(function () { ripple.remove(); }, 650);
    });

    // ---------- 2) Đổ bóng header khi cuộn + nút lên đầu trang ----------
    var header = document.querySelector('.site-header');

    // Nút "lên đầu trang"
    var toTop = document.createElement('button');
    toTop.type = 'button';
    toTop.className = 'to-top';
    toTop.setAttribute('aria-label', 'Lên đầu trang');
    toTop.textContent = '↑';
    toTop.addEventListener('click', function () {
        window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' });
    });
    document.body.appendChild(toTop);

    function onScroll() {
        var y = window.scrollY || document.documentElement.scrollTop;

        // Cuộn xuống quá 8px thì đổ bóng header
        if (header) header.classList.toggle('is-scrolled', y > 8);

        // Cuộn xuống quá 420px thì hiện nút lên đầu trang
        toTop.classList.toggle('show', y > 420);
    }

    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();

    // ---------- 3) Trạng thái "đang gửi" cho các form POST ----------
    // Hiện vòng xoay trên nút và chặn bấm đúp (tránh gửi hai lần)
    document.addEventListener('submit', function (event) {
        var form = event.target;

        // Chỉ áp dụng cho form POST
        if (!form || (form.method || '').toLowerCase() !== 'post') return;

        // Nếu form đã được gửi rồi thì chặn lần gửi thứ hai
        if (form.dataset.submitting === 'true') {
            event.preventDefault();
            return;
        }

        // Bị confirm(...) hủy thì thôi (onsubmit trả về false sẽ làm defaultPrevented = true)
        if (event.defaultPrevented) return;

        form.dataset.submitting = 'true';

        var button = event.submitter || form.querySelector('button[type="submit"]');
        if (button) button.classList.add('is-loading');
    });

    // Quay lại trang bằng nút Back (bfcache): gỡ trạng thái đang gửi
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('form[data-submitting]').forEach(function (form) {
            delete form.dataset.submitting;
        });
        document.querySelectorAll('.is-loading').forEach(function (el) {
            el.classList.remove('is-loading');
        });
    });
})();