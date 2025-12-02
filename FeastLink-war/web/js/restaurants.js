document.addEventListener('DOMContentLoaded', function () {
    // Init icons
    if (window.lucide) {
        window.lucide.createIcons();
    }

    // --------- LẤY DATA TỪ BACKEND ----------
    let raw = window.FEASTLINK_RESTAURANTS;
    let restaurants = [];

    try {
        if (Array.isArray(raw)) {
            restaurants = raw;
        } else if (typeof raw === 'string' && raw.trim().length > 0) {
            restaurants = JSON.parse(raw);
        }
    } catch (e) {
        console.error('Failed to parse FEASTLINK_RESTAURANTS', e, raw);
        restaurants = [];
    }

    console.log('Restaurants after parse:', restaurants);

    // ----- PHẦN CODE CŨ GIỮ NGUYÊN TỪ ĐÂY TRỞ XUỐNG -----
    const state = {
        viewMode: 'grid',
        sortBy: 'recommended',
        quickTags: new Set(),
        eventTypes: new Set(),
        capacity: '',
        rating: 0,
        priceMax: 2000000,
        city: '',
        area: '',
        searchEventType: '',
        guests: '',
        status: '',
        keyword: '',
        page: 1,
        perPage: 6
    };


    const gridEl = document.getElementById('restaurantGrid');
    const summaryEl = document.getElementById('resultsSummary');
    const paginationEl = document.getElementById('pagination');
    const matchCountEl = document.getElementById('matchCountText');

    const sortSelect = document.getElementById('sortSelect');
    const viewGridBtn = document.getElementById('viewGridBtn');
    const viewListBtn = document.getElementById('viewListBtn');

    const quickFilterButtons = document.querySelectorAll('.js-quick-filter');
    const priceRangeInputs = document.querySelectorAll('.js-price-range');
    const priceLabels = document.querySelectorAll('.js-price-label');
    const eventTypeButtons = document.querySelectorAll('.js-event-type');
    const capacityButtons = document.querySelectorAll('.js-capacity');
    const ratingButtons = document.querySelectorAll('.js-rating');
    const statusButtons = document.querySelectorAll('.js-status');

    const citySelect = document.getElementById('citySelect');
    const areaSelect = document.getElementById('areaSelect');
    const eventTypeSelect = document.getElementById('eventTypeSelect');
    const guestsInput = document.getElementById('guestsInput');
    const searchButton = document.getElementById('searchButton');
    const keywordInput = document.getElementById('keywordInput');
    const budgetInput = document.getElementById('budgetInput');
    const resetSearchBtn = document.getElementById('resetSearchButton');

    const clearDesktopBtn = document.getElementById('clearFiltersDesktop');
    const clearMobileBtn = document.getElementById('clearFiltersMobile');


    const mobileToggle = document.getElementById('mobileFilterToggle');
    const mobileSheet = document.getElementById('mobileFilterSheet');
    const mobileBackdrop = document.getElementById('mobileFilterBackdrop');
    const mobileClose = document.getElementById('mobileFilterClose');
    const mobileApply = document.getElementById('mobileFilterApply');

    // 🔁 ĐỔI SANG USD
    function formatCurrency(v) {
        return '$' + v.toLocaleString('en-US');
    }

    function parseBudgetInput(value) {
        if (!value)
            return null;
        const cleaned = value.replace(/[^0-9]/g, '');
        if (!cleaned)
            return null;
        return Number(cleaned);
    }


    function applyQuickFilterCondition(r, tag) {
        switch (tag) {
            case 'Top rated':
                return r.rating >= 4.8;
            case 'VIP venues':
                return r.badge === 'VIP';
            case 'New':
                return r.badge === 'NEW';
            case 'Big capacity 300+':
                return r.capacityMax >= 300;
            case 'Has combos':
                return true;
            default:
                return true;
        }
    }

    function matchesFilters(r) {
        if (state.keyword) {
            const kw = state.keyword.toLowerCase();
            const text = [
                r.name || '',
                r.city || '',
                r.district || '',
                r.description || ''
            ].join(' ').toLowerCase();

            if (!text.includes(kw)) {
                return false;
            }
        }

        if (state.city && r.city !== state.city)
            return false;
        if (state.area && r.district !== state.area)
            return false;

        if (state.searchEventType && !r.eventTypes.includes(state.searchEventType))
            return false;

        if (state.guests) {
            const g = Number(state.guests);
            if (!Number.isNaN(g) && g > r.capacityMax)
                return false;
        }

        for (const tag of state.quickTags) {
            if (!applyQuickFilterCondition(r, tag))
                return false;
        }

        if (state.eventTypes.size > 0) {
            let ok = false;
            for (const t of state.eventTypes) {
                if (r.eventTypes.includes(t)) {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                return false;
        }

        if (state.capacity) {
            const min = r.capacityMin;
            const max = r.capacityMax;
            if (state.capacity === '<50' && !(max < 50))
                return false;
            if (state.capacity === '50-150' && !(max >= 50 && min <= 150))
                return false;
            if (state.capacity === '150-300' && !(max >= 150 && min <= 300))
                return false;
            if (state.capacity === '300+' && !(max >= 300))
                return false;
        }

        if (state.rating > 0 && r.rating < state.rating)
            return false;

        if (state.status && r.availability !== state.status)
            return false;

        if (r.pricePerGuest > state.priceMax)
            return false;

        return true;
    }

    function sortRestaurants(list) {
        const arr = [...list];
        switch (state.sortBy) {
            case 'rating':
                arr.sort((a, b) => b.rating - a.rating || b.reviews - a.reviews);
                break;
            case 'price-low':
                arr.sort((a, b) => a.pricePerGuest - b.pricePerGuest);
                break;
            case 'price-high':
                arr.sort((a, b) => b.pricePerGuest - a.pricePerGuest);
                break;
            case 'capacity':
                arr.sort((a, b) => b.capacityMax - a.capacityMax);
                break;
            case 'newest':
                arr.sort((a, b) => {
                    const aNew = a.badge === 'NEW' ? 1 : 0;
                    const bNew = b.badge === 'NEW' ? 1 : 0;
                    if (bNew !== aNew)
                        return bNew - aNew;
                    return b.rating - a.rating;
                });
                break;
            case 'recommended':
            default:
                arr.sort((a, b) => {
                    const score = (r) =>
                        r.rating * 2 +
                                r.reviews / 100 +
                                (r.badge === 'TOP RATED' ? 1 : 0) +
                                (r.badge === 'VIP' ? 0.5 : 0);
                    return score(b) - score(a);
                });
        }
        return arr;
    }

    function renderCard(r) {
        let availabilityLabel = '';
        let availabilityClass = '';

        if (r.availability === 'available') {
            availabilityLabel = 'Available';
            availabilityClass = 'bg-[#22C55E] text-white';
        } else if (r.availability === 'limited') {
            availabilityLabel = 'Limited slots';
            availabilityClass = 'bg-[#EAB308] text-[#0B1120]';
        } else {
            availabilityLabel = 'Fully booked';
            availabilityClass = 'bg-[#EF4444] text-white';
        }

        const badgeHtml = r.badge
                ? `<span class="inline-block px-3 py-1 bg-gradient-to-r from-[#D4AF37] to-[#E6C77F] text-[#0B1120] text-xs font-bold rounded-full shadow-lg">${r.badge}</span>`
                : '';

        const eventTypesHtml = r.eventTypes
                .map(
                        (t) =>
                        `<span class="px-2.5 py-1 text-xs font-medium text-[#D4AF37] border border-[#E6C77F] rounded-full">${t}</span>`
                )
                .join('');

        return `
<article
  class="group bg-white rounded-[28px] border border-[#E5E7EB] shadow-sm hover:shadow-2xl hover:-translate-y-2 hover:shadow-[#D4AF37]/20 hover:border-[#D4AF37]/50 transition-all duration-500 ease-out relative overflow-hidden"
  data-restaurant-id="${r.id}">
 <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000 ease-out pointer-events-none z-10"></div>

  <div class="relative aspect-[16/9] rounded-t-[28px] overflow-hidden">
    <img
      src="${r.image}"
      alt="${r.name}"
      class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-out"
    />
    <div class="absolute inset-0 bg-gradient-to-t from-[#0B1120]/40 via-transparent to-transparent group-hover:from-[#0B1120]/20 transition-all duration-500"></div>

    <div class="absolute top-4 left-4 z-20">
      ${badgeHtml}
    </div>

    <div class="absolute top-4 right-4 z-20">
      <div class="flex items-center gap-1 px-3 py-1.5 bg-white/95 backdrop-blur-sm rounded-full shadow-lg group-hover:shadow-xl transition-shadow duration-300">
        <i data-lucide="star" class="w-3.5 h-3.5 text-[#D4AF37]"></i>
        <span class="text-sm font-bold text-[#111827]">${r.rating.toFixed(1)}</span>
        <span class="text-xs text-[#4B5563]">(${r.reviews})</span>
      </div>
    </div>
  </div>

  <div class="p-6 relative z-10">
    <h3 class="text-xl font-bold text-[#111827] mb-2 group-hover:text-[#0B1120] transition-all duration-300 group-hover:translate-x-1">
      ${r.name}
    </h3>

    <div class="flex items-center gap-2 text-sm text-[#4B5563] mb-3">
      <i data-lucide="map-pin" class="w-4 h-4 text-[#E6C77F]"></i>
      <span>${r.city} · ${r.district}</span>
    </div>

    <p class="text-sm text-[#4B5563] mb-4">
      ${r.description}
    </p>

    <div class="flex flex-wrap items-center gap-3 mb-4 pb-4 border-b border-[#E5E7EB] text-xs text-[#4B5563]">
      <div class="flex items-center gap-1.5">
        <i data-lucide="users" class="w-4 h-4 text-[#E6C77F]"></i>
        <span>${r.capacityMin}–${r.capacityMax} guests</span>
      </div>
      <div class="w-px h-4 bg-[#E5E7EB]"></div>
      <div class="flex items-center gap-1.5">
        <i data-lucide="dollar-sign" class="w-4 h-4 text-[#E6C77F]"></i>
        <span>From ${formatCurrency(r.pricePerGuest)} /table</span>
      </div>
    </div>

    <div class="flex flex-wrap gap-2 mb-4 text-xs font-medium">
      ${eventTypesHtml}
    </div>

    <div class="flex flex-wrap gap-2 mb-4 text-xs text-[#4B5563]">
      <div class="flex items-center gap-1.5">
        <i data-lucide="clock" class="w-3.5 h-3.5 text-[#E6C77F]"></i>
        <span>Book ≥ ${r.advanceBookingDays} days</span>
      </div>
      <div class="flex items-center gap-1.5">
        <i data-lucide="shield" class="w-3.5 h-3.5 text-[#E6C77F]"></i>
        <span>Free cancel up to ${r.cancelDays} days</span>
      </div>
      <div class="flex items-center gap-1.5">
        <i data-lucide="coins" class="w-3.5 h-3.5 text-[#E6C77F]"></i>
        <span>Deposit ~${r.depositPercent}%</span>
      </div>
    </div>

    <div class="mb-4">
      <span class="inline-block px-3 py-1 text-xs font-semibold rounded-full shadow-sm ${availabilityClass}">
        ${availabilityLabel}
      </span>
    </div>

    <div class="flex items-center gap-3">
      <button type="button" class="px-4 py-2 border border-[#E5E7EB] hover:border-[#E6C77F] rounded-xl text-sm text-[#4B5563] hover:text-[#D4AF37] transition-all hover:scale-110 active:scale-95" data-action="favorite">
        <i data-lucide="heart" class="w-4 h-4"></i>
      </button>
      <a href="restaurant-details.xhtml?restaurantId=${r.id}"
         class="flex-1 px-4 py-2.5 bg-[#0B1120] hover:bg-[#020617] text-white text-sm font-semibold rounded-xl border-b-2 border-[#D4AF37] transition-all hover:scale-[1.02] active:scale-95 hover:shadow-lg hover:shadow-[#D4AF37]/20 inline-flex items-center justify-center">
        View details
      </a>

      <button type="button" class="flex-1 px-4 py-2.5 bg-gradient-to-r from-[#F97316] to-[#EA580C] hover:from-[#EA580C] hover:to-[#F97316] text-white text-sm font-semibold rounded-xl shadow-lg hover:shadow-2xl hover:shadow-[#EAB308]/40 transition-all transform hover:scale-105 active:scale-95 relative overflow-hidden" data-action="book">
        <span class="relative z-10">Book now</span>
        <div class="absolute inset-0 bg-gradient-to-r from-[#EAB308] to-[#F97316] opacity-0 transition-opacity duration-300"></div>
      </button>
    </div>
  </div>
</article>`;
    }

    function render() {
        const filtered = restaurants.filter(matchesFilters);
        const sorted = sortRestaurants(filtered);

        const total = sorted.length;
        const totalPages = Math.max(1, Math.ceil(total / state.perPage));
        if (state.page > totalPages)
            state.page = totalPages;

        const startIndex = (state.page - 1) * state.perPage;
        const endIndex = Math.min(startIndex + state.perPage, total);
        const pageItems = sorted.slice(startIndex, endIndex);

        if (total === 0) {
            summaryEl.textContent = 'No restaurants match your filters.';
        } else {
            summaryEl.textContent = `Showing ${startIndex + 1}–${endIndex} of ${total} restaurants`;
        }

        matchCountEl.textContent = `${total} venues match your filters`;

        gridEl.innerHTML = pageItems.map(renderCard).join('');

        if (state.viewMode === 'grid') {
            gridEl.className = 'mt-6 grid grid-cols-1 md:grid-cols-2 gap-6';
        } else {
            gridEl.className = 'mt-6 flex flex-col gap-6';
        }

        if (window.lucide) {
            window.lucide.createIcons();
        }

        paginationEl.innerHTML = '';
        if (totalPages <= 1)
            return;

        const prevBtn = document.createElement('button');
        prevBtn.type = 'button';
        prevBtn.dataset.page = String(state.page - 1);
        prevBtn.disabled = state.page === 1;
        prevBtn.className =
                'p-3 rounded-full border-2 border-[#E5E7EB] bg-white text-[#4B5563] hover:border-[#D4AF37] hover:text-[#D4AF37] hover:bg-[#E6C77F]/5 hover:shadow-lg disabled:opacity-40 disabled:cursor-not-allowed';
        prevBtn.innerHTML = '<i data-lucide="chevron-left" class="w-5 h-5"></i>';
        paginationEl.appendChild(prevBtn);

        for (let i = 1; i <= totalPages; i++) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.dataset.page = String(i);
            if (i === state.page) {
                btn.className =
                        'min-w-[44px] h-11 px-4 rounded-full font-semibold text-sm bg-gradient-to-br from-[#0B1120] to-[#020617] text-white border-2 border-[#D4AF37] shadow-xl shadow-[#D4AF37]/20';
            } else {
                btn.className =
                        'min-w-[44px] h-11 px-4 rounded-full font-semibold text-sm bg-white text-[#4B5563] border-2 border-[#E5E7EB] hover:border-[#D4AF37] hover:text-[#D4AF37] hover:bg-[#E6C77F]/5 hover:shadow-lg';
            }
            btn.textContent = String(i);
            paginationEl.appendChild(btn);
        }

        const nextBtn = document.createElement('button');
        nextBtn.type = 'button';
        nextBtn.dataset.page = String(state.page + 1);
        nextBtn.disabled = state.page === totalPages;
        nextBtn.className =
                'p-3 rounded-full border-2 border-[#E5E7EB] bg-white text-[#4B5563] hover:border-[#D4AF37] hover:text-[#D4AF37] hover:bg-[#E6C77F]/5 hover:shadow-lg disabled:opacity-40 disabled:cursor-not-allowed';
        nextBtn.innerHTML = '<i data-lucide="chevron-right" class="w-5 h-5"></i>';
        paginationEl.appendChild(nextBtn);

        if (window.lucide) {
            window.lucide.createIcons();
        }
    }

    sortSelect.addEventListener('change', (e) => {
        state.sortBy = e.target.value;
        state.page = 1;
        render();
    });

    viewGridBtn.addEventListener('click', () => {
        state.viewMode = 'grid';
        viewGridBtn.classList.add('bg-[#0B1120]', 'text-white');
        viewListBtn.classList.remove('bg-[#0B1120]', 'text-white');
        render();
    });

    viewListBtn.addEventListener('click', () => {
        state.viewMode = 'list';
        viewListBtn.classList.add('bg-[#0B1120]', 'text-white');
        viewGridBtn.classList.remove('bg-[#0B1120]', 'text-white');
        render();
    });

    quickFilterButtons.forEach((btn) => {
        const tag = btn.dataset.filterQuick;
        if (state.quickTags.has(tag)) {
            btn.classList.add('filter-pill-selected');
        }
        btn.addEventListener('click', () => {
            if (state.quickTags.has(tag)) {
                state.quickTags.delete(tag);
                btn.classList.remove('filter-pill-selected');
            } else {
                state.quickTags.add(tag);
                btn.classList.add('filter-pill-selected');
            }
            state.page = 1;
            render();
        });
    });

    priceRangeInputs.forEach((input) => {
        input.addEventListener('input', () => {
            const value = Number(input.value);
            state.priceMax = value;
            priceRangeInputs.forEach((i) => {
                if (i !== input)
                    i.value = input.value;
            });
            priceLabels.forEach((lab) => {
                lab.textContent = formatCurrency(value);
            });
            state.page = 1;
            render();
        });
    });

    eventTypeButtons.forEach((btn) => {
        const type = btn.dataset.eventType;
        btn.addEventListener('click', () => {
            if (state.eventTypes.has(type)) {
                state.eventTypes.delete(type);
                btn.classList.remove('filter-pill-selected');
            } else {
                state.eventTypes.add(type);
                btn.classList.add('filter-pill-selected');
            }
            state.page = 1;
            render();
        });
    });

    capacityButtons.forEach((btn) => {
        const cap = btn.dataset.capacity;
        btn.addEventListener('click', () => {
            if (state.capacity === cap) {
                state.capacity = '';
                btn.classList.remove('filter-pill-selected');
            } else {
                state.capacity = cap;
                capacityButtons.forEach((b) => b.classList.remove('filter-pill-selected'));
                btn.classList.add('filter-pill-selected');
            }
            state.page = 1;
            render();
        });
    });

    ratingButtons.forEach((btn) => {
        const rating = Number(btn.dataset.rating);
        btn.addEventListener('click', () => {
            if (state.rating === rating) {
                state.rating = 0;
                btn.classList.remove('filter-pill-selected');
            } else {
                state.rating = rating;
                ratingButtons.forEach((b) => b.classList.remove('filter-pill-selected'));
                btn.classList.add('filter-pill-selected');
            }
            state.page = 1;
            render();
        });
    });

    statusButtons.forEach((btn) => {
        const status = btn.dataset.status;
        btn.addEventListener('click', () => {
            if (state.status === status) {
                state.status = '';
                btn.classList.remove('filter-pill-selected');
            } else {
                state.status = status;
                statusButtons.forEach((b) => b.classList.remove('filter-pill-selected'));
                btn.classList.add('filter-pill-selected');
            }
            state.page = 1;
            render();
        });
    });

    citySelect.addEventListener('change', () => {
        state.city = citySelect.value;
        state.page = 1;
        render();
    });
    areaSelect.addEventListener('change', () => {
        state.area = areaSelect.value;
        state.page = 1;
        render();
    });
    eventTypeSelect.addEventListener('change', () => {
        state.searchEventType = eventTypeSelect.value;
        state.page = 1;
        render();
    });
    if (guestsInput) {
        guestsInput.addEventListener('input', () => {
            state.guests = guestsInput.value;
            state.page = 1;
            render();
        });

        if (budgetInput) {
            budgetInput.addEventListener('input', () => {
                const parsed = parseBudgetInput(budgetInput.value);
                if (parsed != null) {
                    state.priceMax = parsed;
                    // Đồng bộ với slider bên trái
                    priceRangeInputs.forEach((i) => (i.value = String(parsed)));
                    priceLabels.forEach((lab) => (lab.textContent = formatCurrency(parsed)));
                    state.page = 1;
                    render();
                }
            });
        }

    }

    searchButton.addEventListener('click', () => {
        if (keywordInput) {
            state.keyword = keywordInput.value.trim();
        }
        if (guestsInput) {
            state.guests = guestsInput.value;
        }
        if (budgetInput) {
            const parsed = parseBudgetInput(budgetInput.value);
            if (parsed != null) {
                state.priceMax = parsed;
                priceRangeInputs.forEach((i) => (i.value = String(parsed)));
                priceLabels.forEach((lab) => (lab.textContent = formatCurrency(parsed)));
            }
        }
        state.page = 1;
        render();
    });


    // Gõ Enter trong ô search cũng apply luôn
    if (keywordInput) {
        keywordInput.addEventListener('input', () => {
            state.keyword = keywordInput.value.trim();
            state.page = 1;
            render();
        });
    }



    function resetFilters() {
        state.quickTags = new Set();
        state.eventTypes = new Set();
        state.capacity = '';
        state.rating = 0;
        state.priceMax = 10000;
        state.city = '';
        state.area = '';
        state.searchEventType = '';
        state.guests = '';
        state.status = '';
        state.keyword = '';
        state.page = 1;

        if (keywordInput) {
            keywordInput.value = '';
        }

        quickFilterButtons.forEach((btn) => btn.classList.remove('filter-pill-selected'));
        eventTypeButtons.forEach((btn) => btn.classList.remove('filter-pill-selected'));
        capacityButtons.forEach((btn) => btn.classList.remove('filter-pill-selected'));
        ratingButtons.forEach((btn) => btn.classList.remove('filter-pill-selected'));
        statusButtons.forEach((btn) => btn.classList.remove('filter-pill-selected'));

        priceRangeInputs.forEach((input) => (input.value = '10000'));
        priceLabels.forEach((lab) => (lab.textContent = formatCurrency(10000)));

        citySelect.value = '';
        areaSelect.value = '';
        eventTypeSelect.value = '';
        guestsInput.value = '';
        if (budgetInput) {
            budgetInput.value = '';
        }
    }

    clearDesktopBtn.addEventListener('click', () => {
        resetFilters();
        render();
    });
    clearMobileBtn.addEventListener('click', () => {
        resetFilters();
        render();
    });

    if (resetSearchBtn) {
        resetSearchBtn.addEventListener('click', () => {
            resetFilters();
            render();
        });
    }


    paginationEl.addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-page]');
        if (!btn)
            return;
        const p = Number(btn.dataset.page);
        if (!Number.isNaN(p) && p >= 1) {
            state.page = p;
            render();
        }
    });

  document.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;

    const action = btn.dataset.action;

    if (action === 'favorite') {
        // lấy id nhà hàng từ card
        const card = btn.closest('article[data-restaurant-id]');
        const restaurantId = card ? card.dataset.restaurantId : null;

        if (!restaurantId) {
            console.warn('Không tìm thấy data-restaurant-id trên card');
            return;
        }

        // lấy component JSF trong form ẩn
        const hiddenInput = document.getElementById('favoriteForm:restaurantId');
        const submitBtn   = document.getElementById('favoriteForm:submitFavorite');

        if (!hiddenInput || !submitBtn) {
            console.error('Không tìm thấy favoriteForm hoặc các phần tử bên trong');
            return;
        }

        // gán giá trị rồi submit form JSF
        hiddenInput.value = restaurantId;
        submitBtn.click();  // gọi action #{restaurantFavoritesBean.addFavoriteById}

    } else if (action === 'book') {
        const card = btn.closest('article[data-restaurant-id]');
        const restaurantId = card ? card.dataset.restaurantId : null;

        if (restaurantId) {
            window.location.href = 'booking.xhtml?restaurantId=' + encodeURIComponent(restaurantId);
        } else {
            window.location.href = 'booking.xhtml';
        }
    }
});


    function openSheet() {
        mobileSheet.classList.remove('hidden');
        mobileSheet.classList.add('flex');
    }
    function closeSheet() {
        mobileSheet.classList.add('hidden');
        mobileSheet.classList.remove('flex');
    }

    if (mobileToggle)
        mobileToggle.addEventListener('click', openSheet);
    if (mobileBackdrop)
        mobileBackdrop.addEventListener('click', closeSheet);
    if (mobileClose)
        mobileClose.addEventListener('click', closeSheet);
    if (mobileApply)
        mobileApply.addEventListener('click', () => {
            closeSheet();
            render();
        });

    render();
});
