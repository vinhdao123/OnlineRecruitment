    document.addEventListener('DOMContentLoaded', function () {
        if (window.lucide) {
            window.lucide.createIcons();
        }

        // CAROUSEL
        const carousel = document.getElementById('discoveryCarousel');

        if (carousel) {
            const slides = Array.from(
                carousel.querySelectorAll('.discovery-slide')
            );
            const dots = Array.from(
                carousel.querySelectorAll('.carousel-dot')
            );
            const previous = carousel.querySelector('.carousel-prev');
            const next = carousel.querySelector('.carousel-next');

            let current = 0;
            let timer = null;

            function showSlide(index) {
                if (!slides.length) return;

                current = (index + slides.length) % slides.length;

                slides.forEach((slide, i) => {
                    slide.classList.toggle('active', i === current);
                });

                dots.forEach((dot, i) => {
                    dot.classList.toggle('active', i === current);
                });
            }

            function stopAutoplay() {
                if (timer !== null) {
                    clearInterval(timer);
                    timer = null;
                }
            }

            function startAutoplay() {
                stopAutoplay();

                if (window.matchMedia(
                    '(prefers-reduced-motion: reduce)'
                ).matches) {
                    return;
                }

                timer = setInterval(function () {
                    showSlide(current + 1);
                }, 5000);
            }

            previous?.addEventListener('click', function () {
                showSlide(current - 1);
                startAutoplay();
            });

            next?.addEventListener('click', function () {
                showSlide(current + 1);
                startAutoplay();
            });

            dots.forEach((dot, index) => {
                dot.addEventListener('click', function () {
                    showSlide(index);
                    startAutoplay();
                });
            });

            carousel.addEventListener('mouseenter', stopAutoplay);
            carousel.addEventListener('mouseleave', startAutoplay);

            showSlide(0);
            startAutoplay();
        }

        // DANH MỤC NGHỀ NGHIỆP
        const categoryItems = Array.from(
            document.querySelectorAll('.category-item')
        );
        const detailTitle =
            document.getElementById('categoryDetailTitle');
        const detailDescription =
            document.getElementById('categoryDetailDescription');
        const categorySearchButton =
            document.getElementById('categorySearchButton');
        const keywordInput = document.querySelector(
            '.discovery-search input[name="keyword"]'
        );
        const discoveryForm =
            document.querySelector('.discovery-search');

        let selectedKeyword = 'Developer';

        function activateCategory(item) {
            categoryItems.forEach(category => {
                category.classList.toggle('active', category === item);
            });

            selectedKeyword = item.dataset.keyword || '';

            const count = document.querySelector('.category-count');

            if (count) {
                const index = categoryItems.indexOf(item) + 1;
                count.textContent =
                    String(index).padStart(2, '0') + ' / ' +
                    String(categoryItems.length).padStart(2, '0');
            }

            if (detailTitle) {
                detailTitle.textContent = item.dataset.title || '';
            }

            if (detailDescription) {
                detailDescription.textContent =
                    item.dataset.description || '';
            }
        }

        categoryItems.forEach(item => {
            item.addEventListener(
                'mouseenter',
                () => activateCategory(item)
            );
            item.addEventListener(
                'focus',
                () => activateCategory(item)
            );
            item.addEventListener(
                'click',
                () => activateCategory(item)
            );
        });

        categorySearchButton?.addEventListener('click', function () {
            if (keywordInput && discoveryForm) {
                keywordInput.value = selectedKeyword;
                discoveryForm.requestSubmit();
            }
        });

        // THẺ TIN: trên thiết bị cảm ứng (không có chuột) thì chạm vào thẻ để mở/đóng chi tiết.
        // Trên máy tính, chi tiết tự mở khi di chuột vào và tự đóng khi di chuột ra (làm bằng CSS).
        const jobCards = Array.from(document.querySelectorAll('.job-card'));

        if (window.matchMedia('(hover: none)').matches) {
            jobCards.forEach(card => {
                card.addEventListener('click', function (event) {
                    // Chạm vào link, nút hoặc form thì để chúng hoạt động bình thường
                    if (event.target.closest('a, button, form')) return;

                    const willOpen = !card.classList.contains('is-open');

                    // Đóng tất cả thẻ khác, chỉ mở thẻ vừa chạm
                    jobCards.forEach(other => other.classList.remove('is-open'));

                    if (willOpen) card.classList.add('is-open');
                });
            });

            // Chạm ra ngoài thẻ thì đóng chi tiết
            document.addEventListener('click', function (event) {
                if (!event.target.closest('.job-card')) {
                    jobCards.forEach(card => card.classList.remove('is-open'));
                }
            });
        }

        // GỢI Ý TÌM KIẾM
        document.querySelectorAll('.suggestion-chip').forEach(button => {
            button.addEventListener('click', function () {
                if (keywordInput && discoveryForm) {
                    keywordInput.value = button.dataset.keyword || '';
                    discoveryForm.requestSubmit();
                }
            });
        });
    });
