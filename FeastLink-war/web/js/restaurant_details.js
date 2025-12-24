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

    // ================== A-LA-CARTE MENU STATE ==================
    const selectedMenuItemIds = new Set();
    let selectedMenuTotalPerPerson = 0;

// panel Selected dishes cho desktop
    let selectedDrawerVisible = false;          // ban đầu: ẩn
    let hasAutoOpenedSelectedDrawer = false;    // đã auto mở lần đầu chưa
    function showToast(message, type = 'info', opts = {}) {
        const stack = document.getElementById('feast-toast-stack');
        if (!stack) {
            // fallback nếu quên add div
            alert(message);
            return;
        }

        const titleMap = {
            success: 'Success',
            info: 'Info',
            warn: 'Notice',
            error: 'Error'
        };

        const iconMap = {
            success: 'check-circle',
            info: 'info',
            warn: 'alert-triangle',
            error: 'x-circle'
        };

        const toast = document.createElement('div');
        toast.className = 'feast-toast';
        toast.dataset.type = type;

        toast.innerHTML = `
    <div class="feast-toast__icon">
      <i data-lucide="${iconMap[type] || 'info'}" class="w-4 h-4"></i>
    </div>
    <div class="min-w-0">
      <div class="feast-toast__title">${opts.title || titleMap[type] || 'Info'}</div>
      <p class="feast-toast__msg">${message}</p>
    </div>
    <button type="button" class="feast-toast__close" aria-label="Close">
      <i data-lucide="x" class="w-4 h-4"></i>
    </button>
  `;

        stack.appendChild(toast);
        if (window.lucide)
            window.lucide.createIcons();

        const closeBtn = toast.querySelector('.feast-toast__close');
        const remove = () => {
            toast.style.animation = 'feastToastOut .18s ease-in forwards';
            setTimeout(() => toast.remove(), 180);
        };
        if (closeBtn)
            closeBtn.addEventListener('click', remove);

        const duration = opts.duration ?? 3200;
        if (duration > 0)
            setTimeout(remove, duration);
    }


    function updateMenuSummary() {
        const count = selectedMenuItemIds.size;
        const total = selectedMenuTotalPerPerson;

        const countDesktop = document.getElementById('menu-selected-count');
        const countMobile = document.getElementById('menu-selected-count-mobile');
        const totalDesktop = document.getElementById('menu-selected-total');
        const totalMobile = document.getElementById('menu-selected-total-mobile');

        if (countDesktop)
            countDesktop.textContent = count;
        if (countMobile)
            countMobile.textContent = count;

        if (totalDesktop)
            totalDesktop.textContent = total.toFixed(2);
        if (totalMobile)
            totalMobile.textContent = total.toFixed(2);

        const boxDesktop = document.getElementById('menu-selected-summary');
        const boxMobile = document.getElementById('menu-selected-summary-mobile');

        [boxDesktop, boxMobile].forEach(box => {
            if (!box)
                return;
            if (count > 0) {
                box.classList.remove('hidden');
            } else if (box.id === 'menu-selected-summary') {
                box.classList.add('hidden');
            }
        });

        // cập nhật floating drawer
        renderSelectedDrawer();
    }

    function renderSelectedDrawer() {
        const drawer = document.getElementById('selected-menu-drawer');
        const listEl = document.getElementById('selected-menu-list');
        const menuRoot = document.getElementById('menu');
        const toggleBtn = document.getElementById('selected-menu-toggle');
        const toggleCount = document.getElementById('selected-menu-toggle-count');

        if (!drawer || !listEl || !menuRoot)
            return;

        const count = selectedMenuItemIds.size;

        // Cập nhật nút toggle "Selected dishes"
        if (toggleBtn && toggleCount) {
            toggleCount.textContent = count;
            if (count > 0) {
                toggleBtn.classList.remove('hidden');
            } else {
                toggleBtn.classList.add('hidden');
            }
        }

        // Không có món nào -> ẩn panel & reset auto open
        if (count === 0) {
            selectedDrawerVisible = false;
            hasAutoOpenedSelectedDrawer = false;

            drawer.style.display = 'none';
            drawer.classList.add('hidden');
            listEl.innerHTML = '';
            return;
        }

        // Có món lần đầu tiên -> tự mở panel
        if (!hasAutoOpenedSelectedDrawer) {
            selectedDrawerVisible = true;
            hasAutoOpenedSelectedDrawer = true;
        }

        // Build list items
        let html = '';
        selectedMenuItemIds.forEach((id) => {
            const card = menuRoot.querySelector('[data-menu-card="' + id + '"]');
            if (!card)
                return;

            const name = card.getAttribute('data-menu-name') || ('Dish #' + id);
            const category = card.getAttribute('data-menu-category') || '';
            const price = card.getAttribute('data-menu-price') || '';

            html += `
            <div class="flex w-full items-center justify-between rounded-xl bg-white/5 px-2.5 py-1.5
                        hover:bg-white/10 transition-all"
                 data-selected-goto="${id}">
                <div class="flex flex-col text-left">
                    <span class="text-[11px] font-medium text-white line-clamp-1">${name}</span>
                    ${category ? `<span class="text-[10px] text-gray-300">${category}</span>` : ""}
                </div>
                <div class="flex items-center gap-2">
                    ${price ? `<span class="text-[11px] font-semibold text-[#FACC6B]">$${price}</span>` : ""}
                    <button type="button"
                            class="inline-flex h-5 w-5 items-center justify-center rounded-full border border-white/30
                                   text-[10px] text-gray-200 hover:bg-white/10 hover:text-white"
                            data-selected-remove="${id}">
                        ✕
                    </button>
                </div>
            </div>
        `;
        });

        listEl.innerHTML = html;

        // Hiện / ẩn panel theo state
        if (selectedDrawerVisible) {
            drawer.classList.remove('hidden');
            drawer.style.display = '';   // dùng display mặc định (flex)
        } else {
            drawer.style.display = 'none';
            drawer.classList.add('hidden');
        }
    }


    function hasMenuOrPackageSelection() {
        const hasPkg = !!selectedPackageId || !!selectedPackageName;
        const hasMenu = selectedMenuItemIds && selectedMenuItemIds.size > 0;
        return hasPkg || hasMenu;
    }

    function scrollToPackagesOrMenu() {
        const pkg = document.getElementById('packages');
        const menu = document.getElementById('menu');
        (pkg || menu)?.scrollIntoView({behavior: 'smooth', block: 'start'});
    }

    function scrollToAvailability() {
        const el = document.getElementById('availability');
        el?.scrollIntoView({behavior: 'smooth', block: 'start'});
    }






    function handleBookingRedirect(qsParams) {

        // ✅ NEW RULE: must choose a package OR choose at least 1 menu item
        if (!hasMenuOrPackageSelection()) {
            showToast('Please select a package or add at least one dish from the menu before continuing.', 'warn');
            const menuEl = document.getElementById('menu');
            if (menuEl)
                menuEl.scrollIntoView({behavior: 'smooth', block: 'start'});
            return;
        }
        if (selectedPackageId) {
            qsParams.set('comboId', selectedPackageId);            // 👈 NEW
        }
        if (selectedPackageName) {
            qsParams.set('package', selectedPackageName);
        }
        // ✅ đẩy giá package theo khách qua booking
        if (selectedPackagePricePerPerson != null && !isNaN(selectedPackagePricePerPerson) && selectedPackagePricePerPerson > 0) {
            qsParams.set('pkgPricePerPerson', selectedPackagePricePerPerson.toFixed(2));
        }

        // Gắn menu items (nếu có chọn) vào query string để booking.xhtml xử lý
        if (selectedMenuItemIds.size > 0) {
            qsParams.set('menuItems', Array.from(selectedMenuItemIds).join(','));
        }

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

        showToast('Please sign in or create a FeastLink account to complete your booking and unlock booking history and faster checkout.', 'info', {duration: 4500});

    }
// ================== JSF CALENDAR (calendar-days-jsf) → select date + Book button ==================
    let selectedDateIso = null;

    window.FEASTLINK_RD_selectDate = function (btn) {
        if (!btn)
            return;
        const iso = btn.getAttribute('data-date-iso');
        if (!iso)
            return;

        selectedDateIso = iso;

        // highlight selected day (optional)
        const root = document.getElementById('calendar-days-jsf');
        if (root) {
            root.querySelectorAll('button[data-date-iso]').forEach(b => {
                b.classList.remove('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');
            });
        }
        btn.classList.add('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');

        const hint = document.getElementById('selected-date-hint');
        if (hint)
            hint.textContent = 'Selected: ' + selectedDateIso;
    };

    const bookBtn = document.getElementById('book-date-btn');
    if (bookBtn) {
        bookBtn.addEventListener('click', () => {
            if (!selectedDateIso) {
                showToast('Please select a date first.', 'warn', {title: 'Missing date'});
                scrollToAvailability();
                return;
            }
            if (!hasMenuOrPackageSelection()) {
                showToast('Please select a package or add at least one dish from the menu before continuing.', 'warn', {title: 'Missing selection'});
                scrollToPackagesOrMenu();
                return;
            }



            const qsParams = new URLSearchParams();
            if (restaurantId)
                qsParams.set('restaurantId', restaurantId);

            // gửi cả 2 key để booking bean/booking.js nhận kiểu nào cũng ok
            qsParams.set('date', selectedDateIso);
            qsParams.set('eventDate', selectedDateIso);

            if (selectedPackageName)
                qsParams.set('package', selectedPackageName);

            // handleBookingRedirect sẽ tự append comboId + menuItems
            handleBookingRedirect(qsParams);
        });
    }



    // ================== TABS + SCROLL ==================
    const tabButtons = qsa('.tab-btn');
    const sectionIds = ['overview', 'packages', 'menu', 'reviews', 'availability'];

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
    let selectedSlotTime = null;

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
                    selectedSlotTime = null; // reset time slot when change date
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
            // base style
            const wrapper = document.createElement('div');
            wrapper.classList.add(
                    'flex', 'items-center', 'justify-between',
                    'p-4', 'rounded-xl', 'border-2', 'transition-all'
                    );

// màu theo trạng thái
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
                // booked
                wrapper.classList.add(
                        'border-[#EF4444]/30',
                        'bg-[#EF4444]/5',
                        'opacity-60'
                        );
            }

// Highlight selected slot (không có else nữa)
            if (selectedSlotTime === slot.time && slot.status !== 'booked') {
                wrapper.classList.add('ring-2', 'ring-[#F97316]', 'ring-offset-2');
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
                    selectedSlotTime = slot.time;
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
                    selectedSlotTime = slot.time;
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
            // Click on row (outside buttons) to select slot
            if (slot.status !== 'booked') {
                wrapper.addEventListener('click', (e) => {
                    if (e.target instanceof HTMLElement && e.target.closest('button')) {
                        // clicking on inner buttons will be handled separately
                        return;
                    }
                    selectedSlotTime = slot.time;
                    renderTimeSlots(); // re-render to update highlight
                });
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
                showToast('Please select a date first.', 'warn', {title: 'Missing date'});
                scrollToAvailability();
                return;
            }

            if (!hasMenuOrPackageSelection()) {
                showToast('Please select a package or add at least one dish from the menu before continuing.', 'warn', {title: 'Missing selection'});
                scrollToPackagesOrMenu();
                return;
            }

            if (!selectedSlotTime) {
                showToast('Please select a time slot before continuing.', 'warn', {title: 'Missing time slot'});
                scrollToAvailability();
                return;
            }


            const year = selectedDateYear;
            const month = selectedDateMonth + 1;
            const day = selectedDate;

            const dateStr =
                    year + '-' +
                    String(month).padStart(2, '0') + '-' +
                    String(day).padStart(2, '0');

            const qsParams = new URLSearchParams();
            if (restaurantId) {
                qsParams.set('restaurantId', restaurantId);
            }
            qsParams.set('date', dateStr);
            qsParams.set('slot', selectedSlotTime); // always send chosen slot

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
    const lightboxImage = qs('#lightbox-image');

// Danh sách URL ảnh lấy từ DOM
    const imageSources = [];

// Ảnh hero (đầu tiên)
    if (heroTrigger) {
        const heroSrc = heroTrigger.getAttribute('data-src');
        if (heroSrc) {
            imageSources.push(heroSrc);
        }
    }

// Các thumbnail
    thumbs.forEach(thumb => {
        const src = thumb.getAttribute('data-src');
        if (src) {
            imageSources.push(src);
        }
    });

    let currentImageIndex = 0;

    function updateLightboxImage() {
        if (!lightboxImage || !imageSources.length)
            return;
        lightboxImage.src = imageSources[currentImageIndex];
    }

    function updateLightboxCounter() {
        if (!lightboxCounter)
            return;
        const total = imageSources.length || 0;
        if (total === 0) {
            lightboxCounter.textContent = '';
        } else {
            lightboxCounter.textContent = (currentImageIndex + 1) + ' / ' + total;
        }
    }

    function openLightbox(index) {
        if (!lightbox || !imageSources.length)
            return;

        currentImageIndex = index;
        if (currentImageIndex < 0 || currentImageIndex >= imageSources.length) {
            currentImageIndex = 0;
        }

        updateLightboxImage();
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

// Click hero -> mở ảnh đầu tiên
    if (heroTrigger) {
        heroTrigger.addEventListener('click', () => openLightbox(0));
    }

// Click thumbnail -> mở đúng index (hero là index 0, thumb bắt đầu từ 1)
    thumbs.forEach((thumb, idx) => {
        thumb.addEventListener('click', () => {
            // hero = 0, nên thumb đầu tiên = 1
            openLightbox(idx + 1);
        });
    });

// Nút close
    if (lightboxClose) {
        lightboxClose.addEventListener('click', closeLightbox);
    }

// Prev / Next
    if (lightboxPrev) {
        lightboxPrev.addEventListener('click', () => {
            if (!imageSources.length)
                return;
            currentImageIndex = (currentImageIndex - 1 + imageSources.length) % imageSources.length;
            updateLightboxImage();
            updateLightboxCounter();
        });
    }

    if (lightboxNext) {
        lightboxNext.addEventListener('click', () => {
            if (!imageSources.length)
                return;
            currentImageIndex = (currentImageIndex + 1) % imageSources.length;
            updateLightboxImage();
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
    let selectedPackageId = null;
    let selectedPackagePricePerPerson = null;
    const packagesSection = qs('#packages');

    function getPackageIdFromCard(card) {
        if (!card)
            return null;
        return card.getAttribute('data-combo-id') || null; // lấy từ data-combo-id trên card
    }

    function getPackagePricePerPersonFromCard(card) {
        if (!card)
            return null;

        const total = parseFloat(card.getAttribute('data-price-total') || '0');
        if (isNaN(total) || total <= 0)
            return null;

        // ✅ PriceTotal được hiểu là price per person
        return total;
    }






    function getCardFromButton(btn) {
        return btn.closest('[data-combo-id]') || btn.closest('.group');
    }


    function getPackageNameFromCard(card) {
        const title = card ? qs('h3', card) : null;
        return title ? title.textContent.trim() : 'this package';
    }

    function money(n) {
        const x = Number(n || 0);
        return x.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
    }

    function closePackageMenuModal() {
        const modal = document.getElementById('package-menu-modal');
        if (!modal)
            return;
        modal.classList.add('hidden');
        document.body.classList.remove('overflow-hidden');
    }

    function openPackageMenuModal(card) {
        const modal = document.getElementById('package-menu-modal');
        const titleEl = document.getElementById('package-menu-title');
        const subEl = document.getElementById('package-menu-subtitle');
        const itemsEl = document.getElementById('package-menu-items');
        if (!modal || !titleEl || !subEl || !itemsEl)
            return;

        const name = getPackageNameFromCard(card) || 'Package details';
        const minGuests = card ? (card.getAttribute('data-min-guests') || '') : '';
        const total = card ? (card.getAttribute('data-price-total') || '') : '';

        titleEl.textContent = name;
        subEl.textContent = `${minGuests ? `Minimum ${minGuests} guests • ` : ''}$${money(total)} total`;

        itemsEl.innerHTML = '';

        const src = card ? card.querySelector('[data-combo-menu-source="true"]') : null;
        const rawItems = src ? Array.from(src.querySelectorAll('.combo-menu-item')) : [];

        if (!rawItems.length) {
            itemsEl.innerHTML = `<div class="text-sm text-[#6B7280]">No menu items found for this package.</div>`;
        } else {
            rawItems.forEach(el => {
                const itemName = el.getAttribute('data-name') || '';
                const itemDesc = el.getAttribute('data-desc') || '';
                const qty = el.getAttribute('data-qty') || '1';
                const veg = (el.getAttribute('data-veg') || '') === 'true';
                const img = el.getAttribute('data-img') || '';
                const price = el.getAttribute('data-price') || '';

                const vegBadge = veg
                        ? `<span class="text-[11px] font-semibold px-2 py-1 rounded-full bg-[#ECFDF5] text-[#065F46]">Vegetarian</span>`
                        : '';

                itemsEl.insertAdjacentHTML('beforeend', `
        <div class="flex gap-4 p-4 rounded-2xl border border-[#E5E7EB]">
          <div class="w-16 h-16 rounded-xl bg-[#F3F4F6] overflow-hidden flex items-center justify-center shrink-0">
            ${img ? `<img src="${img}" alt="" class="w-full h-full object-cover" onerror="this.style.display='none'">` : ''}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="font-semibold text-[#111827] truncate">${itemName}</div>
                ${itemDesc ? `<div class="text-sm text-[#6B7280] mt-1">${itemDesc}</div>` : ''}
              </div>
              <div class="text-right shrink-0">
                <div class="text-sm font-semibold text-[#111827]">x${qty}</div>
                ${price ? `<div class="text-xs text-[#6B7280]">$${money(price)} / person</div>` : ''}
              </div>
            </div>
            <div class="mt-2 flex items-center gap-2">${vegBadge}</div>
          </div>
        </div>
      `);
            });
        }

        modal.classList.remove('hidden');
        document.body.classList.add('overflow-hidden');

        if (window.lucide)
            window.lucide.createIcons();
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
                    // bỏ chọn
                    resetAllPackageCards();
                    selectedCard = null;
                    selectedPackageName = null;
                    selectedPackageId = null;
                    selectedPackagePricePerPerson = null;
                    showToast('Package selection has been cleared.', 'info', {title: 'Selection cleared'});
                } else {
                    // chọn mới
                    resetAllPackageCards();
                    selectedCard = card;
                    selectedPackageName = getPackageNameFromCard(card);
                    selectedPackageId = getPackageIdFromCard(card);
                    selectedPackagePricePerPerson = getPackagePricePerPersonFromCard(card);
                    console.log('[RD] selected pkg pricePerPerson=', selectedPackagePricePerPerson);
                    if (!selectedPackageId) {
                        showToast('Package selected but missing combo id (data-combo-id). Please check restaurant-details.xhtml.', 'error', {title: 'Missing data', duration: 5000});
                    }

                    applySelectedState(card);
                    showToast(
                            `Selected package: ${selectedPackageName}. You can now choose a date in the Availability section to continue.`,
                            'success',
                            {title: 'Package selected', duration: 4200}
                    );

                }
            }


            // COMPARE
            if (role === 'compare-package') {
                const card = getCardFromButton(btn);
                const name = getPackageNameFromCard(card);

                if (compareSet.has(name)) {
                    compareSet.delete(name);
                    showToast(`Removed from comparison list: ${name}`, 'info', {title: 'Compare'});
                } else {
                    compareSet.add(name);
                    showToast(`Added to comparison list: ${name}.`, 'info', {title: 'Compare', duration: 3500});

                }
            }

            // VIEW MENU DETAILS
            if (role === 'view-menu') {
                const card = getCardFromButton(btn);
                openPackageMenuModal(card);
            }

        });
    }

    // ================== A-LA-CARTE MENU: ACCORDION, SHOW MORE & SELECTION ==================
    const menuRoot = document.getElementById('menu');

    if (menuRoot) {
        const categoryBlocks = qsa('[data-menu-category]', menuRoot);
        // Drawer: handle close & jump to dish
        // Drawer: handle close & jump to dish
        const selectedDrawer = document.getElementById('selected-menu-drawer');
        const toggleDrawerBtn = document.getElementById('selected-menu-toggle');
        const closeDrawerBtns = qsa('#selected-menu-drawer-close');

// Toggle show / hide drawer (nếu có nút toggle)
        if (toggleDrawerBtn && selectedDrawer) {
            toggleDrawerBtn.addEventListener('click', () => {
                // chỉ cho bật/tắt khi đã có món
                if (selectedMenuItemIds.size === 0)
                    return;

                selectedDrawerVisible = !selectedDrawerVisible;
                renderSelectedDrawer();
            });
        }

// Close button (X) trong drawer
        if (closeDrawerBtns.length && selectedDrawer) {
            closeDrawerBtns.forEach(btn => {
                btn.addEventListener('click', () => {
                    selectedDrawerVisible = false;
                    renderSelectedDrawer();
                });
            });
        }




        // Click trong drawer: xoá món hoặc scroll tới card
        if (selectedDrawer && menuRoot) {
            selectedDrawer.addEventListener('click', (e) => {
                const target = e.target;
                if (!(target instanceof HTMLElement))
                    return;

                // 1) Nếu bấm nút ✕ => remove món
                const removeBtn = target.closest('[data-selected-remove]');
                if (removeBtn) {
                    const removeId = removeBtn.getAttribute('data-selected-remove');
                    if (!removeId)
                        return;

                    const card = menuRoot.querySelector('[data-menu-card="' + removeId + '"]');
                    const toggleBtn = card ? card.querySelector('button[data-role="toggle-menu-item"]') : null;

                    // Giả lập click lại nút "Add to menu" để reuse toàn bộ logic
                    if (toggleBtn) {
                        toggleBtn.click();
                    }
                    return;
                }

                // 2) Nếu bấm vào row => scroll tới card
                const goBtn = target.closest('[data-selected-goto]');
                if (!goBtn)
                    return;

                const id = goBtn.getAttribute('data-selected-goto');
                if (!id)
                    return;

                const card = menuRoot.querySelector('[data-menu-card="' + id + '"]');
                if (!card)
                    return;

                card.scrollIntoView({behavior: 'smooth', block: 'center'});
                card.classList.add('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');
            });
        }


        // Initial accordion + show-more state
        categoryBlocks.forEach((block, index) => {
            const body = qs('[data-role="menu-category-body"]', block);
            if (!body)
                return;

            const chevron = qs('.menu-category-chevron', block);

            // Open first category, collapse others
            if (index === 0) {
                block.dataset.open = 'true';
                body.style.display = '';
                if (chevron)
                    chevron.classList.remove('rotate-180');
            } else {
                block.dataset.open = 'false';
                body.style.display = 'none';
                if (chevron)
                    chevron.classList.add('rotate-180');
            }

            // Hide items beyond index 2 (show-more)
            const items = qsa('[data-menu-item-index]', body);
            items.forEach(item => {
                const idx = parseInt(item.getAttribute('data-menu-item-index') || '0', 10);
                if (idx >= 3) {
                    item.classList.add('hidden');
                }
            });

            const showMoreBtn = qs('button[data-role="menu-category-show-more"]', block);
            if (showMoreBtn) {
                const hasHidden = items.some(it => it.classList.contains('hidden'));
                if (!hasHidden) {
                    showMoreBtn.classList.add('hidden');
                } else {
                    showMoreBtn.dataset.expanded = 'false';
                }
            }
        });

        // Category pill navigation
        qsa('button[data-role="menu-category-pill"]').forEach(pill => {
            pill.addEventListener('click', () => {
                const targetKey = pill.getAttribute('data-menu-category-id');
                const targetBlock = menuRoot.querySelector('[data-menu-category="' + targetKey + '"]');
                if (!targetBlock)
                    return;

                targetBlock.scrollIntoView({behavior: 'smooth', block: 'start'});

                qsa('button[data-role="menu-category-pill"]').forEach(p => {
                    p.classList.remove('border-[#FACC6B]', 'text-[#111827]', 'bg-[#FFF7E6]');
                });
                pill.classList.add('border-[#FACC6B]', 'text-[#111827]', 'bg-[#FFF7E6]');
            });
        });

        // Single click handler for: toggle item + show more + accordion header
        menuRoot.addEventListener('click', e => {
            const target = e.target;
            if (!(target instanceof HTMLElement))
                return;

            // 1) Toggle menu item select
            const toggleBtn = target.closest('button[data-role="toggle-menu-item"]');
            if (toggleBtn) {
                const menuId = toggleBtn.getAttribute('data-menu-id');
                const priceStr = toggleBtn.getAttribute('data-menu-price') || '0';
                const price = parseFloat(priceStr) || 0;

                if (!menuId)
                    return;

                const card = toggleBtn.closest('[data-menu-card]');
                const selectedBadge = card ? card.querySelector('[data-badge-selected]') : null;
                const labelSpan = toggleBtn.querySelector('span');

                const isSelected = selectedMenuItemIds.has(menuId);

                if (isSelected) {
                    selectedMenuItemIds.delete(menuId);
                    selectedMenuTotalPerPerson -= price;

                    toggleBtn.classList.remove('bg-[#020617]', 'text-white', 'border-[#D4AF37]');
                    toggleBtn.classList.add('border-[#E5E7EB]', 'bg-white', 'text-[#111827]');

                    if (labelSpan)
                        labelSpan.textContent = 'Add to menu';
                    if (selectedBadge)
                        selectedBadge.classList.add('hidden');
                    if (card)
                        card.classList.remove('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');
                } else {
                    selectedMenuItemIds.add(menuId);
                    selectedMenuTotalPerPerson += price;

                    toggleBtn.classList.remove('border-[#E5E7EB]', 'bg-white', 'text-[#111827]');
                    toggleBtn.classList.add('bg-[#020617]', 'text-white', 'border-[#D4AF37]');

                    if (labelSpan)
                        labelSpan.textContent = 'Added';
                    if (selectedBadge)
                        selectedBadge.classList.remove('hidden');
                    if (card)
                        card.classList.add('ring-2', 'ring-[#D4AF37]', 'ring-offset-2');
                }

                if (selectedMenuTotalPerPerson < 0)
                    selectedMenuTotalPerPerson = 0;

                updateMenuSummary();
                createIcons(); // redraw lucide icons if needed

                return;
            }

            // 2) Show more / show less
            const showMoreBtn = target.closest('button[data-role="menu-category-show-more"]');
            if (showMoreBtn) {
                const catKey = showMoreBtn.getAttribute('data-menu-category-id');
                const block = menuRoot.querySelector('[data-menu-category="' + catKey + '"]');
                if (!block)
                    return;

                const body = qs('[data-role="menu-category-body"]', block);
                if (!body)
                    return;

                const items = qsa('[data-menu-item-index]', body);
                const expanded = showMoreBtn.dataset.expanded === 'true';

                if (!expanded) {
                    // Show all
                    items.forEach(it => it.classList.remove('hidden'));
                    showMoreBtn.dataset.expanded = 'true';
                    showMoreBtn.querySelector('span') && (showMoreBtn.querySelector('span').textContent = 'Show less');
                    const icon = showMoreBtn.querySelector('[data-lucide="chevron-down"]');
                    if (icon)
                        icon.classList.add('rotate-180');
                } else {
                    // Collapse back to 3
                    items.forEach(it => {
                        const idx = parseInt(it.getAttribute('data-menu-item-index') || '0', 10);
                        if (idx >= 3) {
                            it.classList.add('hidden');
                        }
                    });
                    showMoreBtn.dataset.expanded = 'false';
                    showMoreBtn.querySelector('span') && (showMoreBtn.querySelector('span').textContent = 'Show all dishes');
                    const icon = showMoreBtn.querySelector('[data-lucide="chevron-down"]');
                    if (icon)
                        icon.classList.remove('rotate-180');
                }

                createIcons();
                return;
            }

            // 3) Accordion header
            const headerBtn = target.closest('button[data-role="menu-category-toggle"]');
            if (headerBtn) {
                const catKey = headerBtn.getAttribute('data-menu-category-id');
                const block = menuRoot.querySelector('[data-menu-category="' + catKey + '"]');
                if (!block)
                    return;

                const body = qs('[data-role="menu-category-body"]', block);
                const chevron = qs('.menu-category-chevron', block);
                const isOpen = block.dataset.open === 'true';

                if (isOpen) {
                    block.dataset.open = 'false';
                    if (body)
                        body.style.display = 'none';
                    if (chevron)
                        chevron.classList.add('rotate-180');
                } else {
                    block.dataset.open = 'true';
                    if (body)
                        body.style.display = '';
                    if (chevron)
                        chevron.classList.remove('rotate-180');
                }

                return;
            }
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

    window.scrollToReviewForm = function () {
        const el = document.getElementById('reviewsForm:reviewFormCard');
        if (el)
            el.scrollIntoView({behavior: 'smooth', block: 'start'});
    };

    (function () {
        function findHiddenRating() {
            return document.querySelector("[id$=':ratingVal']");
        }

        function paintStars(val) {
            const stars = document.querySelectorAll(".feast-star-btn");
            stars.forEach(btn => {
                const n = parseInt(btn.dataset.star, 10);
                if (!isNaN(val) && n <= val) {
                    btn.classList.remove("text-[#E5E7EB]");
                    btn.classList.add("text-[#D4AF37]");
                } else {
                    btn.classList.remove("text-[#D4AF37]");
                    btn.classList.add("text-[#E5E7EB]");
                }
            });
        }

        // Event delegation: không sợ bị rerender
        document.addEventListener("click", function (e) {
            const btn = e.target.closest(".feast-star-btn");
            if (!btn)
                return;

            const val = parseInt(btn.dataset.star, 10);
            const hidden = findHiddenRating();
            if (!hidden || isNaN(val))
                return;

            hidden.value = String(val);
            paintStars(val);
        });

        // Khi page load (hoặc rerender), đồng bộ màu theo hidden value
        function syncFromHidden() {
            const hidden = findHiddenRating();
            if (!hidden)
                return;
            const val = parseInt(hidden.value, 10);
            paintStars(isNaN(val) ? 0 : val);
        }
        window.syncReviewStars = syncFromHidden;

        // sync on load
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", syncFromHidden);
        } else {
            syncFromHidden();
        }

        // sync after any JSF ajax update
        if (window.jsf && jsf.ajax) {
            jsf.ajax.addOnEvent(function (data) {
                if (data.status === "success")
                    syncFromHidden();
            });
        }
    })();
    const pkgModal = document.getElementById('package-menu-modal');
    if (pkgModal) {
        const backdrop = document.getElementById('package-menu-backdrop');
        const close1 = document.getElementById('package-menu-close');
        const close2 = document.getElementById('package-menu-close2');

        [backdrop, close1, close2].forEach(el => {
            if (el)
                el.addEventListener('click', closePackageMenuModal);
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape')
                closePackageMenuModal();
        });
    }


});
