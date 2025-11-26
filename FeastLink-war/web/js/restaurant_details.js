document.addEventListener('DOMContentLoaded', function () {
    // ================== HELPERS ==================
    const qs = (sel, ctx = document) => ctx.querySelector(sel);
    const qsa = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

    const createIcons = () => {
        if (window.lucide)
            window.lucide.createIcons();
    };

    createIcons();
    // Lấy restaurantId từ query string (vd: ?restaurantId=1)
    const params = new URLSearchParams(window.location.search);
    const restaurantId = params.get('restaurantId');

    // ================== TABS + SCROLL ==================
    const tabButtons = qsa('.tab-btn');
    const sectionIds = ['overview', 'packages', 'gallery', 'reviews', 'availability'];

    function setActiveTab(id) {
        tabButtons.forEach(btn => {
            const section = btn.getAttribute('data-section');
            const isActive = section === id;
            const baseClasses =
                    'tab-btn group relative px-4 lg:px-8 py-2.5 lg:py-3 rounded-xl lg:rounded-2xl text-xs lg:text-sm font-bold whitespace-nowrap transition-all duration-300';

            if (isActive) {
                btn.className =
                        baseClasses +
                        ' bg-gradient-to-r from-[#0B1120] to-[#020617] text-white shadow-xl shadow-[#0B1120]/30';
            } else {
                btn.className =
                        baseClasses +
                        ' bg-white text-[#4B5563] border-2 border-[#E5E7EB] hover:border-[#D4AF37] hover:text-[#D4AF37] hover:bg-[#D4AF37]/5';
            }
        });
    }

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const target = btn.getAttribute('data-section');
            if (!target)
                return;
            const el = document.getElementById(target);
            if (el) {
                const offset = 120;
                const rect = el.getBoundingClientRect();
                const offsetTop = rect.top + window.pageYOffset - offset;
                window.scrollTo({top: offsetTop, behavior: 'smooth'});
            }
        });
    });

    window.addEventListener('scroll', () => {
        const offsetTrigger = 150;
        for (const id of sectionIds) {
            const el = document.getElementById(id);
            if (!el)
                continue;
            const rect = el.getBoundingClientRect();
            const top = rect.top;
            const bottom = rect.bottom;
            if (top <= offsetTrigger && bottom >= offsetTrigger) {
                setActiveTab(id);
                break;
            }
        }
    });

    // ================== AVAILABILITY CALENDAR ==================
    const monthNames = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    let currentMonth = new Date();
    currentMonth.setDate(1);
    let selectedDate = null;
    let selectedDateMonth = currentMonth.getMonth();
    let selectedDateYear = currentMonth.getFullYear();

    const monthLabel = qs('#calendar-month-label');
    const daysContainer = qs('#calendar-days');
    const prevBtn = qs('#calendar-prev');
    const nextBtn = qs('#calendar-next');
    const timeSlotsContainer = qs('#time-slots-container');
    const timeSlotsTitle = qs('#time-slots-title');
    const timeSlotsList = qs('#time-slots-list');

    const timeSlots = [
        {time: 'Lunch (11:00–14:00)', status: 'available'},
        {time: 'Afternoon (15:00–18:00)', status: 'limited'},
        {time: 'Dinner (18:00–22:00)', status: 'booked'}
    ];

    function getDateStatus(day) {
        if (day % 7 === 0)
            return 'booked';
        if (day % 5 === 0)
            return 'limited';
        return 'available';
    }

    function renderCalendar() {
        if (!monthLabel || !daysContainer)
            return;

        const year = currentMonth.getFullYear();
        const month = currentMonth.getMonth();

        monthLabel.textContent = monthNames[month] + ' ' + year;

        daysContainer.innerHTML = '';

        const firstDayOfMonth = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();

        // empty cells
        for (let i = 0; i < firstDayOfMonth; i++) {
            const div = document.createElement('div');
            div.className = 'aspect-square';
            daysContainer.appendChild(div);
        }

        for (let d = 1; d <= daysInMonth; d++) {
            const status = getDateStatus(d);
            const isSelected =
                    selectedDate === d &&
                    selectedDateMonth === month &&
                    selectedDateYear === year;

            const btn = document.createElement('button');
            btn.textContent = String(d);
            btn.classList.add(
                    'relative', 'aspect-square', 'rounded-xl', 'text-sm', 'font-medium',
                    'transition-all', 'flex', 'items-center', 'justify-center'
                    );

            if (status === 'available') {
                if (isSelected) {
                    btn.classList.add(
                            'bg-[#0B1120]', 'text-white', 'ring-2',
                            'ring-[#D4AF37]', 'ring-offset-2'
                            );
                } else {
                    btn.classList.add(
                            'border-2', 'border-[#22C55E]/30',
                            'text-[#111827]', 'hover:border-[#22C55E]', 'hover:bg-[#22C55E]/5'
                            );
                }
            } else if (status === 'limited') {
                if (isSelected) {
                    btn.classList.add(
                            'bg-[#0B1120]', 'text-white', 'ring-2',
                            'ring-[#D4AF37]', 'ring-offset-2'
                            );
                } else {
                    btn.classList.add(
                            'border-2', 'border-[#EAB308]/30',
                            'text-[#111827]', 'hover:border-[#EAB308]', 'hover:bg-[#EAB308]/5'
                            );
                    const dot = document.createElement('span');
                    dot.className = 'absolute top-1 right-1 w-1.5 h-1.5 bg-[#EAB308] rounded-full';
                    btn.appendChild(dot);
                }
            } else {
                btn.classList.add(
                        'bg-[#EF4444]/10', 'text-[#EF4444]',
                        'border', 'border-[#EF4444]/20', 'cursor-not-allowed'
                        );
                btn.disabled = true;
            }

            if (status !== 'booked') {
                btn.addEventListener('click', () => {
                    selectedDate = d;
                    selectedDateMonth = month;
                    selectedDateYear = year;
                    renderCalendar();
                    renderTimeSlots();
                });
            }

            daysContainer.appendChild(btn);
        }
    }

    function renderTimeSlots() {
        if (!timeSlotsContainer || !timeSlotsTitle || !timeSlotsList)
            return;

        if (!selectedDate) {
            timeSlotsContainer.classList.add('hidden');
            return;
        }

        timeSlotsContainer.classList.remove('hidden');
        timeSlotsTitle.textContent =
                'Available Time Slots for ' + monthNames[selectedDateMonth] + ' ' + selectedDate;

        timeSlotsList.innerHTML = '';
        timeSlots.forEach(slot => {
            const wrapper = document.createElement('div');
            wrapper.classList.add(
                    'flex', 'items-center', 'justify-between',
                    'p-4', 'rounded-xl', 'border-2', 'transition-all'
                    );

            if (slot.status === 'available') {
                wrapper.classList.add('border-[#22C55E]/30', 'bg-[#22C55E]/5', 'hover:border-[#22C55E]');
            } else if (slot.status === 'limited') {
                wrapper.classList.add('border-[#EAB308]/30', 'bg-[#EAB308]/5', 'hover:border-[#EAB308]');
            } else {
                wrapper.classList.add('border-[#EF4444]/30', 'bg-[#EF4444]/5', 'opacity-60');
            }

            const left = document.createElement('div');
            left.className = 'flex items-center gap-3';

            const icon = document.createElement('i');
            icon.setAttribute('data-lucide', 'clock');
            icon.className = 'w-5 h-5';
            if (slot.status === 'available')
                icon.classList.add('text-[#22C55E]');
            else if (slot.status === 'limited')
                icon.classList.add('text-[#EAB308]');
            else
                icon.classList.add('text-[#EF4444]');

            const span = document.createElement('span');
            span.className = 'font-medium text-[#111827]';
            span.textContent = slot.time;

            left.appendChild(icon);
            left.appendChild(span);

            const right = document.createElement('div');
            right.className = 'flex items-center gap-2';

            if (slot.status === 'available') {
                const badge = document.createElement('span');
                badge.className =
                        'px-3 py-1 bg-[#22C55E] text-white text-sm font-medium rounded-full flex items-center gap-1';
                const checkIcon = document.createElement('i');
                checkIcon.setAttribute('data-lucide', 'check');
                checkIcon.className = 'w-4 h-4';
                const text = document.createElement('span');
                text.textContent = 'Available';
                badge.appendChild(checkIcon);
                badge.appendChild(text);

                const btn = document.createElement('button');
                btn.className =
                        'px-4 py-2 bg-gradient-to-r from-[#F97316] to-[#EAB308] text-white text-sm font-semibold rounded-lg hover:shadow-lg transition-all';
                btn.textContent = 'Book Now';

                // Hành vi Book Now trong time slot
                btn.addEventListener('click', () => {
                    if (!selectedDate)
                        return;

                    var year = selectedDateYear;
                    var month = selectedDateMonth + 1; // 1–12
                    var day = selectedDate;

                    var dateStr =
                            year + '-' +
                            String(month).padStart(2, '0') + '-' +
                            String(day).padStart(2, '0');

                    var qsParams = new URLSearchParams();
                    if (restaurantId)
                        qsParams.set('restaurantId', restaurantId);
                    qsParams.set('date', dateStr);
                    qsParams.set('slot', slot.time);

                    window.location.href = 'booking.xhtml?' + qsParams.toString();
                });

            } else if (slot.status === 'limited') {
                const badge = document.createElement('span');
                badge.className = 'px-3 py-1 bg-[#EAB308] text-white text-sm font-medium rounded-full';
                badge.textContent = 'Limited Slots';

                const btn = document.createElement('button');
                btn.className =
                        'px-4 py-2 bg-gradient-to-r from-[#F97316] to-[#EAB308] text-white text-sm font-semibold rounded-lg hover:shadow-lg transition-all';
                btn.textContent = 'Book Now';
                btn.addEventListener('click', () => {
                    if (!selectedDate)
                        return;

                    var year = selectedDateYear;
                    var month = selectedDateMonth + 1;
                    var day = selectedDate;

                    var dateStr =
                            year + '-' +
                            String(month).padStart(2, '0') + '-' +
                            String(day).padStart(2, '0');

                    var qsParams = new URLSearchParams();
                    if (restaurantId)
                        qsParams.set('restaurantId', restaurantId);
                    qsParams.set('date', dateStr);
                    qsParams.set('slot', slot.time);

                    window.location.href = 'booking.xhtml?' + qsParams.toString();
                });

            } else {
                const badge = document.createElement('span');
                badge.className =
                        'px-3 py-1 bg-[#EF4444] text-white text-sm font-medium rounded-full flex items-center gap-1';
                const xIcon = document.createElement('i');
                xIcon.setAttribute('data-lucide', 'x');
                xIcon.className = 'w-4 h-4';
                const text = document.createElement('span');
                text.textContent = 'Fully Booked';
                badge.appendChild(xIcon);
                badge.appendChild(text);
                right.appendChild(badge);
            }

            wrapper.appendChild(left);
            wrapper.appendChild(right);
            timeSlotsList.appendChild(wrapper);
        });

        createIcons();
    }

    if (prevBtn) {
        prevBtn.addEventListener('click', () => {
            currentMonth.setMonth(currentMonth.getMonth() - 1);
            renderCalendar();
            createIcons();
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener('click', () => {
            currentMonth.setMonth(currentMonth.getMonth() + 1);
            renderCalendar();
            createIcons();
        });
    }

    renderCalendar();

    const proceedBtn = qs('#proceed-button');
    if (proceedBtn) {
        proceedBtn.addEventListener('click', () => {
            if (!selectedDate) {
                alert('Vui lòng chọn ngày trước.');
                return;
            }

            var year = selectedDateYear;
            var month = selectedDateMonth + 1;
            var day = selectedDate;

            var dateStr =
                    year + '-' +
                    String(month).padStart(2, '0') + '-' +
                    String(day).padStart(2, '0');

            var qsParams = new URLSearchParams();
            if (restaurantId)
                qsParams.set('restaurantId', restaurantId);
            qsParams.set('date', dateStr);

            window.location.href = 'booking.xhtml?' + qsParams.toString();
        });
    }


    // ================== FAQ TOGGLE ==================
    const faqItems = qsa('.faq-item');
    faqItems.forEach((item, index) => {
        const toggleBtn = qs('.faq-toggle', item);
        const content = qs('.faq-content', item);
        const icon = qs('[data-lucide="chevron-down"]', item);

        if (!toggleBtn || !content || !icon)
            return;

        // Mặc định: FAQ 1 mở
        if (index === 0) {
            content.classList.remove('hidden');
            icon.classList.add('rotate-180');
        }

        toggleBtn.addEventListener('click', () => {
            const isHidden = content.classList.contains('hidden');

            faqItems.forEach(i => {
                const c = qs('.faq-content', i);
                const ic = qs('[data-lucide="chevron-down"]', i);
                if (c)
                    c.classList.add('hidden');
                if (ic)
                    ic.classList.remove('rotate-180');
            });

            if (isHidden) {
                content.classList.remove('hidden');
                icon.classList.add('rotate-180');
            }
        });
    });

    // ================== PHOTO LIGHTBOX ==================
    const lightbox = qs('#lightbox');
    const lightboxClose = qs('#lightbox-close');
    const lightboxPrev = qs('#lightbox-prev');
    const lightboxNext = qs('#lightbox-next');
    const lightboxCounter = qs('#lightbox-counter');
    const heroTrigger = qs('#hero-gallery-trigger');
    const thumbs = qsa('.thumb');
    const totalImages = 12;
    let currentImageIndex = 0;

    function updateLightboxCounter() {
        if (!lightboxCounter)
            return;
        lightboxCounter.textContent = (currentImageIndex + 1) + ' / ' + totalImages;
    }

    function openLightbox(index) {
        if (!lightbox)
            return;
        currentImageIndex = index;
        updateLightboxCounter();
        lightbox.classList.remove('hidden');
        lightbox.classList.add('flex');
    }

    function closeLightbox() {
        if (!lightbox)
            return;
        lightbox.classList.add('hidden');
        lightbox.classList.remove('flex');
    }

    if (heroTrigger) {
        heroTrigger.addEventListener('click', () => openLightbox(0));
    }
    thumbs.forEach(thumb => {
        thumb.addEventListener('click', () => {
            const idx = parseInt(thumb.getAttribute('data-index') || '0', 10);
            openLightbox(idx);
        });
    });

    if (lightboxClose) {
        lightboxClose.addEventListener('click', closeLightbox);
    }
    if (lightboxPrev) {
        lightboxPrev.addEventListener('click', () => {
            currentImageIndex = (currentImageIndex - 1 + totalImages) % totalImages;
            updateLightboxCounter();
        });
    }
    if (lightboxNext) {
        lightboxNext.addEventListener('click', () => {
            currentImageIndex = (currentImageIndex + 1) % totalImages;
            updateLightboxCounter();
        });
    }

    // ================== FLOATING ACTIONS ==================
    const scrollTopBtn = qs('#scroll-top-btn');
    const whatsappBtn = qs('#whatsapp-btn');
    const callBtn = qs('#call-btn');
    const bottomBookBtn = qs('#bottom-book-btn');
    const bottomCallBtn = qs('#bottom-call-btn');

    window.addEventListener('scroll', () => {
        if (!scrollTopBtn)
            return;
        if (window.scrollY > 500) {
            scrollTopBtn.classList.remove('opacity-0', 'pointer-events-none');
        } else {
            scrollTopBtn.classList.add('opacity-0', 'pointer-events-none');
        }
    });

    if (scrollTopBtn) {
        scrollTopBtn.addEventListener('click', () => {
            window.scrollTo({top: 0, behavior: 'smooth'});
        });
    }

    if (whatsappBtn) {
        whatsappBtn.addEventListener('click', () => {
            window.open('https://wa.me/84123456789', '_blank');
        });
    }
    if (callBtn) {
        callBtn.addEventListener('click', () => {
            window.open('tel:+84123456789', '_self');
        });
    }
    if (bottomCallBtn) {
        bottomCallBtn.addEventListener('click', () => {
            window.open('tel:+84123456789', '_self');
        });
    }

    // ================== MOBILE BOOKING MODAL ==================
    const mobileBookingModal = qs('#mobile-booking-modal');
    const openMobileBooking = qs('#open-mobile-booking');
    const closeMobileBooking = qs('#close-mobile-booking');

    function openMobileModal() {
        if (!mobileBookingModal)
            return;
        mobileBookingModal.classList.remove('hidden');
        mobileBookingModal.classList.add('flex');
    }
    function closeMobileModal() {
        if (!mobileBookingModal)
            return;
        mobileBookingModal.classList.add('hidden');
        mobileBookingModal.classList.remove('flex');
    }

    if (openMobileBooking) {
        openMobileBooking.addEventListener('click', openMobileModal);
    }
    if (closeMobileBooking) {
        closeMobileBooking.addEventListener('click', closeMobileModal);
    }
    if (mobileBookingModal) {
        mobileBookingModal.addEventListener('click', (e) => {
            if (e.target === mobileBookingModal) {
                closeMobileModal();
            }
        });
    }

    // Nút "Book Now" ở bottom bar mobile: mở modal luôn (thay vì chỉ scroll)
    if (bottomBookBtn) {
        bottomBookBtn.addEventListener('click', () => {
            // Nếu có modal thì mở modal, nếu không thì scroll tới availability
            if (mobileBookingModal) {
                openMobileModal();
            } else {
                const el = document.getElementById('availability');
                if (el)
                    el.scrollIntoView({behavior: 'smooth'});
            }
        });
    }

    // ================== PACKAGE BUTTONS (Select / Compare / View menu) ==================
    let selectedPackageName = null;
    const packagesSection = qs('#packages');

    function getPackageNameFromButton(btn) {
        const card = btn.closest('.group');
        const title = card ? qs('h3', card) : null;
        return title ? title.textContent.trim() : 'this package';
    }

    // Highlight card khi chọn package
    function highlightSelectedPackage(packageName) {
        const cards = qsa('#packages .group');
        cards.forEach(card => {
            card.classList.remove('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');
            const title = qs('h3', card);
            if (title && title.textContent.trim() === packageName) {
                card.classList.add('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');
            }
        });
    }

    const compareSet = new Set();

    if (packagesSection) {
        packagesSection.addEventListener('click', (e) => {
            const target = e.target;

            if (!(target instanceof HTMLElement))
                return;

            // Select Package
            if (target.textContent.trim() === 'Select Package') {
                const pkgName = getPackageNameFromButton(target);
                selectedPackageName = pkgName;
                highlightSelectedPackage(pkgName);
                alert(
                        'Đã chọn gói: ' +
                        pkgName +
                        '. Bây giờ bạn có thể chọn ngày tại mục Availability để tiếp tục đặt tiệc.'
                        );
            }

            // Compare
            if (target.textContent.trim() === 'Compare') {
                const pkgName = getPackageNameFromButton(target);
                if (compareSet.has(pkgName)) {
                    compareSet.delete(pkgName);
                    alert('Đã bỏ gói khỏi danh sách so sánh: ' + pkgName);
                } else {
                    compareSet.add(pkgName);
                    alert(
                            'Đã thêm vào danh sách so sánh: ' +
                            pkgName +
                            '\nCác gói đang so sánh: ' +
                            Array.from(compareSet).join(', ')
                            );
                }
            }

            // View full menu details
            if (target.textContent.trim() === 'View full menu details') {
                const pkgName = getPackageNameFromButton(target);
                alert(
                        'Demo: Trang/ popup chi tiết menu cho gói "' +
                        pkgName +
                        '" sẽ được mở ở phiên bản thật.'
                        );
            }
        });
    }

    // ================== REVIEWS: WRITE & LOAD MORE ==================
    const writeReviewBtn = qs('#reviews button', qs('#reviews'));
    const loadMoreBtn = qs('#reviews button:nth-of-type(2)'); // "Load More Reviews" là nút cuối section

    if (writeReviewBtn) {
        writeReviewBtn.addEventListener('click', () => {
            alert('Demo: Form viết đánh giá sẽ được hiển thị ở phiên bản hoàn chỉnh.');
        });
    }

    const loadMoreReviewsBtn = qs('#reviews .mt-6.text-center button');
    if (loadMoreReviewsBtn) {
        loadMoreReviewsBtn.addEventListener('click', () => {
            alert('Demo: Không còn review nào nữa. (Dữ liệu sẽ được load từ backend sau này).');
        });
    }

    // ================== DESKTOP CHECK AVAILABILITY BUTTON ==================
    // Nút "Check Availability & Pricing" trong card bên phải (desktop)
    qsa('button').forEach(btn => {
        const text = btn.textContent.trim();
        if (text === 'Check Availability & Pricing' && btn.id !== 'open-mobile-booking') {
            btn.addEventListener('click', () => {
                const availabilitySection = document.getElementById('availability');
                if (availabilitySection) {
                    availabilitySection.scrollIntoView({behavior: 'smooth'});
                }
            });
        }
    });

    // ================== "View All Venues" & Similar Venues "View" buttons ==================
    const viewAllVenuesBtn = qs('#similar-venues button');

    if (viewAllVenuesBtn) {
        viewAllVenuesBtn.addEventListener('click', () => {
            alert('Demo: Nút này sẽ dẫn về trang danh sách nhà hàng (Restaurant Listing).');
        });
    }

    qsa('#similar-venues .group button').forEach(btn => {
        if (btn.textContent.trim() === 'View') {
            btn.addEventListener('click', () => {
                alert('Demo: Nút này sẽ dẫn tới trang chi tiết của venue tương ứng.');
            });
        }
    });
});
        