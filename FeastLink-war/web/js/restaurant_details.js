document.addEventListener('DOMContentLoaded', function () {
    // ================== HELPERS ==================
    const qs = (sel, ctx = document) => ctx.querySelector(sel);
    const qsa = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

    const createIcons = () => {
        if (window.lucide) {
            window.lucide.createIcons();
        }
    };

    createIcons();

    // Read restaurantId from query string (e.g., ?restaurantId=1)
    const params = new URLSearchParams(window.location.search);
    const restaurantId = params.get('restaurantId');

    const isLoggedIn = !!(window.FEASTLINK && window.FEASTLINK.isLoggedIn);

    function handleBookingRedirect(qsParams) {
        const url = 'booking.xhtml?' + qsParams.toString();

        if (isLoggedIn) {
            window.location.href = url;
            return;
        }

        const banner = qs('#login-required-banner');
        if (banner) {
            banner.classList.remove('hidden');
            banner.scrollIntoView({behavior: 'smooth', block: 'center'});
        }

        alert(
                'Please sign in or create a FeastLink account to complete your booking and unlock your booking history and faster checkout.'
                );
    }


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

        // Leading empty cells
        for (let i = 0; i < firstDayOfMonth; i++) {
            const div = document.createElement('div');
            div.className = 'aspect-square';
            daysContainer.appendChild(div);
        }
        
        // === Constraint: không cho chọn ngày quá khứ + phải cách hiện tại tối thiểu N ngày ===
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        // Số ngày tối thiểu cách hiện tại để được đặt tiệc
        const MIN_LEAD_DAYS = 3;  // muốn 1-2-7 ngày thì sửa con số này

        const minDate = new Date(today);
        minDate.setDate(today.getDate() + MIN_LEAD_DAYS);


       for (let d = 1; d <= daysInMonth; d++) {
            const dateObj = new Date(year, month, d);

            // Trạng thái demo cũ (booked/limited/available)
            let status = getDateStatus(d);

            // Nếu ngày này trước minDate -> xem như booked (đỏ, không click được)
            if (dateObj < minDate) {
                status = 'booked';
            }

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
                'Available Time Slots for ' +
                monthNames[selectedDateMonth] +
                ' ' +
                selectedDate;

        timeSlotsList.innerHTML = '';

        timeSlots.forEach(slot => {
            const wrapper = document.createElement('div');
            wrapper.classList.add(
                    'flex', 'items-center', 'justify-between',
                    'p-4', 'rounded-xl', 'border-2', 'transition-all'
                    );

            if (slot.status === 'available') {
                wrapper.classList.add(
                        'border-[#22C55E]/30',
                        'bg-[#22C55E]/5',
                        'hover:border-[#22C55E]'
                        );
            } else if (slot.status === 'limited') {
                wrapper.classList.add(
                        'border-[#EAB308]/30',
                        'bg-[#EAB308]/5',
                        'hover:border-[#EAB308]'
                        );
            } else {
                wrapper.classList.add(
                        'border-[#EF4444]/30',
                        'bg-[#EF4444]/5',
                        'opacity-60'
                        );
            }

            const left = document.createElement('div');
            left.className = 'flex items-center gap-3';

            const icon = document.createElement('i');
            icon.setAttribute('data-lucide', 'clock');
            icon.className = 'w-5 h-5';
            if (slot.status === 'available') {
                icon.classList.add('text-[#22C55E]');
            } else if (slot.status === 'limited') {
                icon.classList.add('text-[#EAB308]');
            } else {
                icon.classList.add('text-[#EF4444]');
            }

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

                // Book Now in a specific time slot (available)
                btn.addEventListener('click', () => {
                    if (!selectedDate)
                        return;

                    const year = selectedDateYear;
                    const month = selectedDateMonth + 1; // 1–12
                    const day = selectedDate;

                    const dateStr =
                            year +
                            '-' +
                            String(month).padStart(2, '0') +
                            '-' +
                            String(day).padStart(2, '0');

                    const qsParams = new URLSearchParams();
                    if (restaurantId) {
                        qsParams.set('restaurantId', restaurantId);
                    }
                    qsParams.set('date', dateStr);
                    qsParams.set('slot', slot.time);

                    if (selectedPackageName) {
                        qsParams.set('package', selectedPackageName);
                    }

                    handleBookingRedirect(qsParams);

                });

                right.appendChild(badge);
                right.appendChild(btn);
            } else if (slot.status === 'limited') {
                const badge = document.createElement('span');
                badge.className =
                        'px-3 py-1 bg-[#EAB308] text-white text-sm font-medium rounded-full';
                badge.textContent = 'Limited slots';

                const btn = document.createElement('button');
                btn.className =
                        'px-4 py-2 bg-gradient-to-r from-[#F97316] to-[#EAB308] text-white text-sm font-semibold rounded-lg hover:shadow-lg transition-all';
                btn.textContent = 'Book Now';

                // Book Now in a specific time slot (limited)
                btn.addEventListener('click', () => {
                    if (!selectedDate)
                        return;

                    const year = selectedDateYear;
                    const month = selectedDateMonth + 1;
                    const day = selectedDate;

                    const dateStr =
                            year +
                            '-' +
                            String(month).padStart(2, '0') +
                            '-' +
                            String(day).padStart(2, '0');

                    const qsParams = new URLSearchParams();
                    if (restaurantId) {
                        qsParams.set('restaurantId', restaurantId);
                    }
                    qsParams.set('date', dateStr);
                    qsParams.set('slot', slot.time);

                    if (selectedPackageName) {
                        qsParams.set('package', selectedPackageName);
                    }

                    handleBookingRedirect(qsParams);

                });

                right.appendChild(badge);
                right.appendChild(btn);
            } else {
                const badge = document.createElement('span');
                badge.className =
                        'px-3 py-1 bg-[#EF4444] text-white text-sm font-medium rounded-full flex items-center gap-1';
                const xIcon = document.createElement('i');
                xIcon.setAttribute('data-lucide', 'x');
                xIcon.className = 'w-4 h-4';
                const text = document.createElement('span');
                text.textContent = 'Fully booked';
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

    // Proceed with selected date & package (bottom button)
    const proceedBtn = qs('#proceed-button');
    if (proceedBtn) {
        proceedBtn.addEventListener('click', () => {
            if (!selectedDate) {
                alert('Please select a date first.');
                return;
            }

            if (!selectedPackageName) {
                alert('Please select a package before continuing.');
                return;
            }

            const year = selectedDateYear;
            const month = selectedDateMonth + 1;
            const day = selectedDate;

            const dateStr =
                    year +
                    '-' +
                    String(month).padStart(2, '0') +
                    '-' +
                    String(day).padStart(2, '0');

            const qsParams = new URLSearchParams();
            if (restaurantId) {
                qsParams.set('restaurantId', restaurantId);
            }
            qsParams.set('date', dateStr);

            if (selectedPackageName) {
                qsParams.set('package', selectedPackageName);
            }

            handleBookingRedirect(qsParams);

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

        // Default: first FAQ open
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

    // Bottom bar "Book Now" on mobile: open modal (or scroll to availability as fallback)
    if (bottomBookBtn) {
        bottomBookBtn.addEventListener('click', () => {
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
    let selectedCard = null;
    let selectedPackageName = null;
    const packagesSection = qs('#packages');

    function getCardFromButton(btn) {
        return btn.closest('.group');
    }

    function getPackageNameFromCard(card) {
        const title = card ? qs('h3', card) : null;
        return title ? title.textContent.trim() : 'this package';
    }

    function resetAllPackageCards() {
        const cards = qsa('#packages .group');
        cards.forEach(card => {
            card.classList.remove('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');

            const btn = qs('button[data-role="select-package"]', card);
            if (btn) {
                btn.dataset.selected = 'false';

                const span = btn.querySelector('span');
                if (span)
                    span.textContent = 'Select Package';

                // back to default (orange) style
                btn.classList.remove('bg-white', 'text-[#111827]', 'border', 'border-[#D4AF37]');
                btn.classList.add(
                        'bg-gradient-to-r',
                        'from-[#F97316]',
                        'to-[#FACC6B]',
                        'text-white'
                        );
            }
        });
    }

    function applySelectedState(card) {
        if (!card)
            return;

        card.classList.add('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');

        const btn = qs('button[data-role="select-package"]', card);
        if (btn) {
            btn.dataset.selected = 'true';

            const span = btn.querySelector('span');
            if (span)
                span.textContent = 'Selected';

            btn.classList.remove(
                    'bg-gradient-to-r',
                    'from-[#F97316]',
                    'to-[#FACC6B]',
                    'text-white'
                    );
            btn.classList.add('bg-white', 'text-[#111827]', 'border', 'border-[#D4AF37]');
        }
    }

    const compareSet = new Set();

    if (packagesSection) {
        packagesSection.addEventListener('click', (e) => {
            const target = e.target;
            if (!(target instanceof HTMLElement))
                return;

            const btn = target.closest('button');
            if (!btn)
                return;

            const role = btn.dataset.role;

            // SELECT / DESELECT PACKAGE
            if (role === 'select-package') {
                const card = getCardFromButton(btn);
                if (!card)
                    return;

                if (selectedCard === card) {
                    resetAllPackageCards();
                    selectedCard = null;
                    selectedPackageName = null;
                    alert('Package selection has been cleared.');
                } else {
                    resetAllPackageCards();
                    selectedCard = card;
                    selectedPackageName = getPackageNameFromCard(card);
                    applySelectedState(card);
                    alert(
                            'Selected package: ' +
                            selectedPackageName +
                            '. You can now choose a date in the Availability section to continue your booking.'
                            );
                }
            }

            // COMPARE
            if (role === 'compare-package') {
                const card = getCardFromButton(btn);
                const name = getPackageNameFromCard(card);

                if (compareSet.has(name)) {
                    compareSet.delete(name);
                    alert('Removed from comparison list: ' + name);
                } else {
                    compareSet.add(name);
                    alert(
                            'Added to comparison list: ' +
                            name +
                            '\nCurrently comparing: ' +
                            Array.from(compareSet).join(', ')
                            );
                }
            }

            // VIEW MENU DETAILS (demo)
            if (role === 'view-menu') {
                const card = getCardFromButton(btn);
                const name = getPackageNameFromCard(card);
                alert(
                        'Demo: A full menu details page or popup for "' +
                        name +
                        '" will open here in the final version.'
                        );
            }
        });
    }

    // ================== REVIEWS: WRITE & LOAD MORE ==================
    const writeReviewBtn = qs('#reviews button', qs('#reviews'));
    const loadMoreBtn = qs('#reviews button:nth-of-type(2)'); // second button inside #reviews

    if (writeReviewBtn) {
        writeReviewBtn.addEventListener('click', () => {
            alert('Demo: The review form will be displayed here in the full version.');
        });
    }

    const loadMoreReviewsBtn = qs('#reviews .mt-6.text-center button');
    if (loadMoreReviewsBtn) {
        loadMoreReviewsBtn.addEventListener('click', () => {
            alert(
                    'Demo: There are no more reviews. In the real version, more reviews will be loaded from the backend.'
                    );
        });
    }

    // ================== DESKTOP CHECK AVAILABILITY BUTTON ==================
    // "Check Availability & Pricing" button in the right-hand booking card (desktop)
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
            alert('Demo: This button will navigate to the restaurant listing page.');
        });
    }

    qsa('#similar-venues .group button').forEach(btn => {
        if (btn.textContent.trim() === 'View') {
            btn.addEventListener('click', () => {
                alert('Demo: This button will navigate to the detail page of the selected venue.');
            });
        }
    });
});
