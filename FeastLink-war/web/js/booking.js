//<![CDATA[
const BookingUI = (function () {
    const state = {
        currentStep: 1,
        tableCount: 20,

        // giá 1 bàn (sẽ bị override theo package, nên để 1200 default)
        pricePerGuest: 1200,

        // phí service cơ bản, dùng để nhân theo service level
        baseServiceCharge: 3000,
        serviceCharge: 3000,

        addOns: [
            {id: 1, name: 'Extra dessert table', price: 50, unit: 'table', quantity: 0},
            {id: 2, name: 'Premium wine pairing', price: 50, unit: 'table', quantity: 0},
            {id: 3, name: 'Late-night snacks', price: 35, unit: 'table', quantity: 0}
        ],
        depositPercentage: 30,
        discount: 0,
        voucherCode: '',
        eventType: 'wedding',
        serviceLevel: 'premium',
        locationType: 'AT_RESTAURANT',
        tax: 0,
        totalBeforeDiscount: 0,
        totalAmount: 0,
        depositAmount: 0,
        remainingAmount: 0,
        menuPricePerTable: 0,

        paymentMethod: 'VNPAY',   // default
        paymentType: 'deposit'    // bạn đang dùng rồi (deposit/full)
    };

    const PACKAGE_CONFIGS = {
        'Royal Wedding Package': {
            badge: 'VIP',
            guestRange: 'Ideal for 150–300 guests',
            pricePerGuest: 1200
        },
        'Signature Wedding Menu': {
            badge: 'VIP PACKAGE',
            guestRange: 'Ideal for 10–20 guests',
            pricePerGuest: 890
        },
        'Corporate Gala Menu': {
            badge: 'PACKAGE',
            guestRange: 'Ideal for 10–20 guests',
            pricePerGuest: 750
        }
    };

    // Cấu hình giá cho từng service level (theo phí service)
    const SERVICE_LEVEL_CONFIGS = {
        standard: {
            label: 'Standard',
            feeMultiplier: 0.8   // ~80% base service fee
        },
        premium: {
            label: 'Premium',
            feeMultiplier: 1.0   // = base
        },
        vip: {
            label: 'VIP',
            feeMultiplier: 1.25  // cao hơn 25%
        },
        exclusive: {
            label: 'Exclusive',
            feeMultiplier: 1.5   // cao nhất
        }
    };

    // Hiển thị giá service fee cho từng service level trên UI
    function updateServiceLevelPriceLabels() {
        const base = state.baseServiceCharge || state.serviceCharge || 0;

        Object.keys(SERVICE_LEVEL_CONFIGS).forEach(key => {
            const cfg = SERVICE_LEVEL_CONFIGS[key];
            const el = document.getElementById('service-level-price-' + key);
            if (!el) return;

            const price = Math.round(base * cfg.feeMultiplier);
            el.textContent = '$' + formatNumber(price) + ' service fee';
        });
    }

    // Đọc list món (js-menu-dish-price) và tính giá custom menu / 1 bàn
    function initMenuPriceFromDom() {
        const nodes = document.querySelectorAll('.js-menu-dish-price');
        if (!nodes || nodes.length === 0) {
            state.menuPricePerTable = 0;
            return;
        }

        let perGuestTotal = 0;
        nodes.forEach(span => {
            const val = parseFloat(span.getAttribute('data-price') || '0');
            if (!isNaN(val)) {
                perGuestTotal += val;
            }
        });

        // Giả định 10 khách / bàn (đúng với phần Capacity hiện tại)
        const guestsPerTable = 10;
        state.menuPricePerTable = perGuestTotal * guestsPerTable;
    }

    function formatNumber(n) {
        return n.toLocaleString('en-US');
    }

    // =====================
// VOUCHER (client-side)
// =====================
// - Nếu booking.xhtml có nhúng voucher catalog (JSON) => JS tự tính discount ngay khi "Apply".
// - Nếu không có catalog => JS chỉ sync code/total để server (JSF/CDI) validate và áp dụng khi confirm.

function ensureHiddenInput(id, name, formId) {
    const fid = formId || 'bookingForm';
    let form = document.getElementById(fid);
    if (!form) {
        // fallback: lấy form đầu tiên (tránh lỗi nếu id bị JSF prefix)
        form = document.querySelector('form');
    }
    if (!form) return null;

    // nếu đã có input (id chuẩn) thì dùng luôn
    let el = document.getElementById(id);
    if (el) return el;

    // nếu JSF prependId => id dạng "bookingForm:hf-voucher-code"
    el = form.querySelector("[id$=':" + id + "']");
    if (el) return el;

    // nếu không có, tự tạo hidden input không prefix để server đọc theo name
    el = document.createElement('input');
    el.type = 'hidden';
    el.id = id;
    el.name = name || id;
    form.appendChild(el);
    return el;
}

function ensureVoucherHiddenFields() {
    ensureHiddenInput('hf-voucher-code', 'hf-voucher-code');
    ensureHiddenInput('hf-total-before-discount', 'hf-total-before-discount');
    ensureHiddenInput('hf-voucher-discount', 'hf-voucher-discount');
}

function normalizeCode(code) {
    return (code || '').trim().toUpperCase();
}

function getVoucherCatalog() {
    // 1) global list (optional)
    if (window.__VOUCHER_CATALOG__ && Array.isArray(window.__VOUCHER_CATALOG__)) {
        return window.__VOUCHER_CATALOG__;
    }
    // 2) script#voucher-catalog-json (optional)
    const script = document.getElementById('voucher-catalog-json');
    if (script && script.textContent) {
        try {
            const parsed = JSON.parse(script.textContent);
            if (Array.isArray(parsed)) return parsed;
        } catch (e) {
            // ignore
        }
    }
    return null;
}

function findVoucherByCode(code) {
    const catalog = getVoucherCatalog();
    if (!catalog) return null;
    const c = normalizeCode(code);
    if (!c) return null;

    for (let i = 0; i < catalog.length; i++) {
        const v = catalog[i];
        const vc = normalizeCode(v && v.code);
        if (vc && vc === c) return v;
    }
    return null;
}

function toNumber(x) {
    const n = Number(x);
    return isNaN(n) ? 0 : n;
}

function clamp(n, min, max) {
    return Math.max(min, Math.min(max, n));
}

// Tính số tiền giảm giá theo rule của voucher
// voucher = { discountType: 'PERCENT'|'AMOUNT', discountValue, maxDiscount, minOrderAmount }
function calcVoucherDiscountAmount(voucher, totalBeforeDiscount) {
    if (!voucher) return 0;
    const base = toNumber(totalBeforeDiscount);
    if (base <= 0) return 0;

    const minOrder = toNumber(voucher.minOrderAmount);
    if (minOrder > 0 && base < minOrder) {
        return 0;
    }

    const type = normalizeCode(voucher.discountType);
    const value = toNumber(voucher.discountValue);
    const maxDiscount = toNumber(voucher.maxDiscount);

    let discount = 0;
    if (type === 'PERCENT') {
        discount = base * (value / 100);
    } else if (type === 'AMOUNT') {
        discount = value;
    }

    if (maxDiscount > 0) {
        discount = Math.min(discount, maxDiscount);
    }

    discount = clamp(discount, 0, base);
    return Math.round(discount);
}

function syncVoucherHidden() {
    // đảm bảo hidden tồn tại
    ensureVoucherHiddenFields();

    const codeEl = ensureHiddenInput('hf-voucher-code', 'hf-voucher-code');
    const totalBeforeEl = ensureHiddenInput('hf-total-before-discount', 'hf-total-before-discount');
    const discountEl = ensureHiddenInput('hf-voucher-discount', 'hf-voucher-discount');

    const code = normalizeCode(state.voucherCode);
    const totalBefore = state.totalBeforeDiscount || 0;
    const disc = state.discount || 0;

    if (codeEl) codeEl.value = code;
    if (totalBeforeEl) totalBeforeEl.value = totalBefore;
    if (discountEl) discountEl.value = disc;

    // nếu JSF prefix id, cập nhật luôn input có id dạng "xxx:hf-voucher-code"
    const form = document.querySelector('form');
    if (form) {
        const prefCode = form.querySelector("[id$=':hf-voucher-code']");
        const prefTotal = form.querySelector("[id$=':hf-total-before-discount']");
        const prefDisc = form.querySelector("[id$=':hf-voucher-discount']");
        if (prefCode) prefCode.value = code;
        if (prefTotal) prefTotal.value = totalBefore;
        if (prefDisc) prefDisc.value = disc;
    }
}

function setVoucherStatusUI(mode, message) {
    const box = document.getElementById('voucher-success');
    if (!box) return;

    // reset tone classes
    box.classList.remove(
        'border-emerald-200','bg-emerald-50','text-emerald-700',
        'border-amber-200','bg-amber-50','text-amber-700',
        'border-red-200','bg-red-50','text-red-700'
    );

    if (mode === 'success') {
        box.classList.add('border-emerald-200','bg-emerald-50','text-emerald-700');
    } else if (mode === 'warn') {
        box.classList.add('border-amber-200','bg-amber-50','text-amber-700');
    } else {
        box.classList.add('border-red-200','bg-red-50','text-red-700');
    }

    box.classList.remove('hidden');

    const spans = box.querySelectorAll('span');
    if (spans && spans.length > 0) {
        if (spans[0]) spans[0].textContent = (mode === 'success') ? '✓' : (mode === 'warn' ? '!' : '✕');
        if (spans[1]) spans[1].textContent = message || '';
    } else {
        box.textContent = message || '';
    }
}

function hideVoucherStatusUI() {
    const box = document.getElementById('voucher-success');
    if (box) box.classList.add('hidden');
}

function formatMoneyUSD(n) {
    const v = Math.round(toNumber(n));
    return v.toLocaleString(undefined, { maximumFractionDigits: 0 }) + ' USD';
}

function parseDateSafe(val) {
    if (!val) return null;
    const d = (val instanceof Date) ? val : new Date(val);
    return isNaN(d.getTime()) ? null : d;
}

function validateVoucherAndCalc(voucher, totalBeforeDiscount) {
    const base = toNumber(totalBeforeDiscount);
    if (!voucher || base <= 0) {
        return { ok: false, discount: 0, reason: 'NO_BASE' };
    }

    // optional date window
    const now = new Date();
    const start = parseDateSafe(voucher.startAt || voucher.start || voucher.start_date);
    const end = parseDateSafe(voucher.endAt || voucher.end || voucher.end_date);

    if (start && now < start) {
        return { ok: false, discount: 0, reason: 'NOT_STARTED' };
    }
    if (end && now > end) {
        return { ok: false, discount: 0, reason: 'EXPIRED' };
    }

    const minOrder = toNumber(voucher.minOrderAmount);
    if (minOrder > 0 && base < minOrder) {
        return { ok: false, discount: 0, reason: 'MIN_ORDER', minOrder, base };
    }

    const type = normalizeCode(voucher.discountType);
    const value = toNumber(voucher.discountValue);
    const maxDiscount = toNumber(voucher.maxDiscount);

    if (!value || value <= 0) {
        return { ok: false, discount: 0, reason: 'INVALID_VALUE' };
    }

    let discount = 0;

    if (type === 'PERCENT') {
        discount = base * (value / 100);
    } else if (type === 'AMOUNT') {
        discount = value;
    } else {
        return { ok: false, discount: 0, reason: 'UNKNOWN_TYPE' };
    }

    if (maxDiscount > 0) {
        discount = Math.min(discount, maxDiscount);
    }

    discount = clamp(discount, 0, base);
    discount = Math.round(discount);

    if (discount <= 0) {
        return { ok: false, discount: 0, reason: 'NO_DISCOUNT' };
    }

    return { ok: true, discount, reason: 'OK', type, value, maxDiscount, minOrder, base };
}

// ✅ Đồng bộ hidden fields payment cho server đọc (KHÔNG gọi updateSummary bên trong để tránh loop)
    function syncPaymentHidden() {
        const pmHidden = document.getElementById('hf-payment-method');
        const ptHidden = document.getElementById('hf-payment-type');
        const payHidden = document.getElementById('hf-pay-amount');

        const pt = (state.paymentType || 'deposit').toLowerCase();
        const payAmount = (pt === 'full') ? (state.totalAmount || 0) : (state.depositAmount || 0);

        if (pmHidden) pmHidden.value = state.paymentMethod || 'VNPAY';
        if (ptHidden) ptHidden.value = (pt === 'full') ? 'FULL' : 'DEPOSIT';
        if (payHidden) payHidden.value = payAmount;
    }

    function applyPackageFromParam(packageName) {
        const packageCardEl = document.getElementById('package-card');
        const noPackageEl = document.getElementById('no-package-selected');
        const packageNameEl = document.getElementById('package-name');
        const summaryPkgNameEl = document.getElementById('summary-package-name');
        const badgeEl = document.getElementById('package-badge');
        const guestRangeEl = document.getElementById('package-guest-range');
        const priceEl = document.getElementById('package-price');

        // Không có package hoặc không map trong config -> ẩn card, hiện empty-state
        if (!packageName || !PACKAGE_CONFIGS[packageName]) {
            if (packageCardEl) packageCardEl.classList.add('hidden');
            if (noPackageEl) noPackageEl.classList.remove('hidden');
            if (summaryPkgNameEl) summaryPkgNameEl.textContent = 'No package selected';
            return;
        }

        const cfg = PACKAGE_CONFIGS[packageName];

        if (packageCardEl) packageCardEl.classList.remove('hidden');
        if (noPackageEl) noPackageEl.classList.add('hidden');

        if (packageNameEl) packageNameEl.textContent = packageName;
        if (summaryPkgNameEl) summaryPkgNameEl.textContent = packageName;
        if (badgeEl && cfg.badge) badgeEl.textContent = cfg.badge;
        if (guestRangeEl && cfg.guestRange) guestRangeEl.textContent = cfg.guestRange;
        if (priceEl && cfg.pricePerGuest != null) {
            priceEl.textContent = '$' + formatNumber(cfg.pricePerGuest);
        }

        // Cập nhật số dùng để tính tiền (giá 1 bàn)
        state.pricePerGuest = cfg.pricePerGuest;

        // Re-calc với giá mới
        updateCapacity();
        updateSummary();
        syncPaymentHidden();
    }

    function updateCapacity() {
        const totalGuests = state.tableCount * 10;
        const capacityEl = document.getElementById('capacity-status');
        const guestLabel = document.getElementById('guest-count-label');
        if (!capacityEl || !guestLabel) return;

        let color = '#22C55E';
        let message = 'Within capacity';
        if (state.tableCount >= 10 && state.tableCount <= 25) {
            color = '#22C55E';
            message = 'Ideal for 15–25 tables (150–250 guests)';
        } else if (state.tableCount > 25 && state.tableCount <= 40) {
            color = '#FACC15';
            message = 'Near maximum capacity';
        } else if (state.tableCount > 40) {
            color = '#EF4444';
            message = 'This venue supports 10–50 tables (100–500 guests)';
        }

        capacityEl.style.color = color;
        if (capacityEl.querySelector('span')) {
            capacityEl.querySelector('span').textContent = '';
        }
        if (capacityEl.lastChild) {
            capacityEl.lastChild.textContent = message;
        }

        guestLabel.textContent = '(~' + totalGuests + ' guests at 10 per table)';

        // summary table + guests
        const summaryLabel = document.getElementById('summary-table-guest');
        const packageLabel = document.getElementById('summary-package-label');
        if (summaryLabel) {
            summaryLabel.textContent = state.tableCount + ' tables (~' + totalGuests + ' guests)';
        }
        if (packageLabel) {
            packageLabel.textContent =
                    'Package (' + state.tableCount + ' tables, ~' + totalGuests + ' guests)';
        }

        const guestHidden = document.getElementById('hf-guest-count');
        if (guestHidden) guestHidden.value = totalGuests;
    }

    function collectAddonQuantities() {
        const rows = document.querySelectorAll('.addon-row');
        rows.forEach(row => {
            const id = parseInt(row.getAttribute('data-addon-id'), 10);
            const qtyEl = row.querySelector('.addon-qty');
            const qty = parseInt(qtyEl.textContent || '0', 10);
            const addon = state.addOns.find(a => a.id === id);
            if (addon) addon.quantity = qty;
        });
    }

    function updateSummary() {
        collectAddonQuantities();

        const packageSubtotal = state.pricePerGuest * state.tableCount; // giá theo bàn

        // tiền custom menu (tính từ list món đã chọn)
        const menuSubtotal = state.menuPricePerTable * state.tableCount;

        const addOnsSubtotal = state.addOns.reduce(
                (sum, a) => sum + a.price * a.quantity,
                0
        );

        const subtotal = packageSubtotal + menuSubtotal + addOnsSubtotal;

        const tax = Math.round((subtotal + state.serviceCharge) * 0.1);

const totalBeforeDiscount = subtotal + state.serviceCharge + tax;

// discount là số tiền trừ trực tiếp vào tổng
let discount = Math.round(state.discount || 0);
if (discount < 0) discount = 0;
if (discount > totalBeforeDiscount) discount = totalBeforeDiscount;

const totalWithCharges = Math.max(totalBeforeDiscount - discount, 0);
const depositAmount = Math.round(totalWithCharges * (state.depositPercentage / 100));
const remainingAmount = totalWithCharges - depositAmount;

        state.tax = tax;
        state.totalBeforeDiscount = totalBeforeDiscount;
        state.totalAmount = totalWithCharges;
        state.depositAmount = depositAmount;
        state.remainingAmount = remainingAmount;

        // hidden fields
        const totalField = document.getElementById('hf-total-amount');
        if (totalField) totalField.value = totalWithCharges;

        const depositField = document.getElementById('hf-deposit-amount');
        if (depositField) depositField.value = depositAmount;

        const remainingField = document.getElementById('hf-remaining-amount');
        if (remainingField) remainingField.value = remainingAmount;

        // ✅ voucher hidden fields cho server xử lý
        syncVoucherHidden();

        // DOM
        const pkgTotalEl = document.getElementById('summary-package-total');
        if (pkgTotalEl) pkgTotalEl.textContent = '$' + formatNumber(packageSubtotal);

        const menuEl = document.getElementById('summary-menu-total');
        if (menuEl) menuEl.textContent = '$' + formatNumber(menuSubtotal);

        const serviceEl = document.getElementById('summary-service-charge');
        if (serviceEl) serviceEl.textContent = '$' + formatNumber(state.serviceCharge);

        const taxEl = document.getElementById('summary-tax');
        if (taxEl) taxEl.textContent = '$' + formatNumber(tax);

        const discountRow = document.getElementById('summary-discount-row');
        const discountEl = document.getElementById('summary-discount');
        if (discountRow && discountEl) {
            if (discount > 0) {
                discountRow.classList.remove('hidden');
                discountEl.textContent = '-$' + formatNumber(discount);
            } else {
                discountRow.classList.add('hidden');
            }
        }

        // add-ons rows in summary
        const container = document.getElementById('summary-addons-container');
        if (container) {
            container.innerHTML = '';
            state.addOns
                    .filter(a => a.quantity > 0)
                    .forEach(addon => {
                        const row = document.createElement('div');
                        row.className = 'flex justify-between';
                        row.innerHTML =
                                '<span class="text-[#4B5563]">' +
                                addon.name +
                                ' (' +
                                addon.quantity +
                                ')</span>' +
                                '<span class="font-medium text-[#111827]">$' +
                                formatNumber(addon.price * addon.quantity) +
                                '</span>';

                        container.appendChild(row);
                    });
        }

        const totalEl = document.getElementById('summary-total-amount');
        const depositEl = document.getElementById('summary-deposit');
        const remainingEl = document.getElementById('summary-remaining');
        if (totalEl) totalEl.textContent = '$' + formatNumber(totalWithCharges);
        if (depositEl) depositEl.textContent = '$' + formatNumber(depositAmount);
        if (remainingEl) remainingEl.textContent = '$' + formatNumber(remainingAmount);

        // ✅ luôn đồng bộ hidden payment sau khi tính tiền xong
        syncPaymentHidden();
    }

    function setServiceLevel(level) {
        const key = (level || '').toLowerCase();
        const cfg = SERVICE_LEVEL_CONFIGS[key] || SERVICE_LEVEL_CONFIGS.standard;

        // lưu lại dưới dạng lowercase để sau này dễ map xuống DB nếu cần
        state.serviceLevel = key;

        // đẩy service level xuống hidden để server đọc
        const serviceHidden = document.getElementById('hf-service-level');
        if (serviceHidden) {
            serviceHidden.value = key; // standard / premium / vip / exclusive
        }

        // Toggle UI cho các button
        const buttons = document.querySelectorAll('.service-level-btn');
        buttons.forEach(btn => {
            const val = (btn.getAttribute('data-level') || '').toLowerCase();
            if (val === key) {
                btn.className =
                        'service-level-btn px-4 py-3 rounded-xl font-medium text-sm transition-all duration-200 bg-[#020617] text-white border-2 border-[#D4AF37]';
            } else {
                btn.className =
                        'service-level-btn px-4 py-3 rounded-xl font-medium text-sm transition-all duration-200 bg-white text-[#4B5563] border-2 border-[#E5E7EB] hover:border-[#D4AF37]';
            }
        });

        // Cập nhật text ở summary
        const summary = document.getElementById('summary-service-level');
        if (summary) summary.textContent = cfg.label;

        // Tính lại serviceCharge theo level
        const base = state.baseServiceCharge || state.serviceCharge || 0;
        state.serviceCharge = Math.round(base * cfg.feeMultiplier);

        // Re-calc bảng tiền
        updateSummary();
    }

    function setLocationType(type) {
        const btns = document.querySelectorAll('.location-btn');
        btns.forEach(btn => {
            const val = btn.getAttribute('data-location');
            if (val === type) {
                btn.className =
                        'location-btn p-6 rounded-xl border-2 transition-all duration-200 text-left border-[#D4AF37] bg-[#D4AF37]/5';
            } else {
                btn.className =
                        'location-btn p-6 rounded-xl border-2 transition-all duration-200 text-left border-[#E5E7EB] hover:border-[#D4AF37]';
            }
        });
        const outsideWrapper = document.getElementById('outside-address-wrapper');
        if (outsideWrapper) {
            if (type === 'outside') {
                outsideWrapper.classList.remove('hidden');
            } else {
                outsideWrapper.classList.add('hidden');
            }
        }

        state.locationType = type === 'outside' ? 'OUTSIDE' : 'AT_RESTAURANT';

        const locHidden = document.getElementById('hf-location-type');
        if (locHidden) locHidden.value = state.locationType;
    }

    function setPaymentMethod(method) {
        const cards = document.querySelectorAll('.payment-method');
        cards.forEach(card => {
            const val = card.getAttribute('data-method');
            if (val === method) {
                card.className =
                        'payment-method p-6 rounded-xl border-2 transition-all duration-200 text-left relative border-[#D4AF37] bg-[#020617] text-white';
            } else {
                card.className =
                        'payment-method p-6 rounded-xl border-2 transition-all duration-200 text-left relative border-[#E5E7EB] bg-white hover:border-[#D4AF37]';
            }
        });

        // ✅ map UI -> server method
        // venue = trả tại quầy, còn lại coi như VNPay
        const serverMethod = (method === 'venue') ? 'CASH' : 'VNPAY';
        state.paymentMethod = serverMethod;

        const pmHidden = document.getElementById('hf-payment-method');
        if (pmHidden) pmHidden.value = serverMethod;

        syncPaymentHidden();
    }

    function setPaymentType(type) {
        state.paymentType = type;
        const buttons = document.querySelectorAll('.pay-type');
        buttons.forEach(btn => {
            const val = btn.getAttribute('data-type');
            const radioOuter = btn.querySelector('.radio-outer');
            const radioInner = btn.querySelector('.radio-inner');
            if (val === type) {
                btn.className =
                        'pay-type w-full p-6 rounded-xl border-2 transition-all duration-200 text-left border-[#D4AF37] bg-[#D4AF37]/5';
                if (radioOuter) radioOuter.classList.add('border-[#D4AF37]');
                if (radioInner) radioInner.classList.add('bg-[#D4AF37]');
            } else {
                btn.className =
                        'pay-type w-full p-6 rounded-xl border-2 transition-all duration-200 text-left border-[#E5E7EB] hover:border-[#D4AF37]';
                if (radioOuter) radioOuter.classList.remove('border-[#D4AF37]');
                if (radioInner) radioInner.classList.remove('bg-[#D4AF37]');
            }
        });

        // nếu trả full thì depositPercentage = 100, còn lại 30
        state.depositPercentage = type === 'full' ? 100 : 30;

        const ptHidden = document.getElementById('hf-payment-type');
        if (ptHidden) ptHidden.value = (type === 'full') ? 'FULL' : 'DEPOSIT';

        updateSummary();      // tính lại tiền
        syncPaymentHidden();  // đồng bộ hidden payment
    }

    function goToStep(step) {
        state.currentStep = step;

        // Ẩn / hiện 3 section
        ['step-1', 'step-2', 'step-3'].forEach((id, index) => {
            const sec = document.getElementById(id);
            if (!sec) return;

            if (index + 1 === step) {
                sec.classList.remove('hidden');
                sec.classList.remove('opacity-0');
            } else {
                sec.classList.add('hidden');
            }
        });

        // Cập nhật stepper
        const stepper = document.getElementById('stepper');
        if (stepper) {
            const circles = stepper.querySelectorAll('.step-circle');
            const titles = stepper.querySelectorAll('span.mt-3');
            const lines = stepper.querySelectorAll('.step-line');

            circles.forEach((circle, idx) => {
                const index = idx + 1;
                if (index < step) {
                    circle.className =
                            'step-circle w-12 h-12 rounded-full flex items-center justify-center font-semibold text-base transition-all duration-300 relative bg-[#D4AF37] text-[#020617] shadow-lg';
                    circle.innerHTML = '<i data-lucide="check" class="w-5 h-5"></i>';
                } else if (index === step) {
                    circle.className =
                            'step-circle w-12 h-12 rounded-full flex items-center justify-center font-semibold text-base transition-all duration-300 relative bg-[#020617] text-white ring-4 ring-[#D4AF37]/30 shadow-lg';
                    circle.textContent = String(index);
                } else {
                    circle.className =
                            'step-circle w-12 h-12 rounded-full flex items-center justify-center font-semibold text-base transition-all duration-300 relative bg-white text-[#9CA3AF] border-2 border-[#E5E7EB]';
                    circle.textContent = String(index);
                }
            });

            titles.forEach((title, idx) => {
                const index = idx + 1;
                if (index === step) {
                    title.classList.remove('text-[#4B5563]');
                    title.classList.add('text-[#111827]');
                } else {
                    title.classList.remove('text-[#111827]');
                    title.classList.add('text-[#4B5563]');
                }
            });

            lines.forEach((line, idx) => {
                const index = idx + 1;
                if (index < step) {
                    line.classList.remove('step-line-zero');
                    line.classList.add('step-line-full');
                } else {
                    line.classList.remove('step-line-full');
                    line.classList.add('step-line-zero');
                }
            });
        }

        // Cập nhật lại bảng tính tiền
        updateSummary();

        // Re-init icon
        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }
    }

    // ====== CONTACT VALIDATION ======
    function validateContact() {
        const fullNameEl = document.getElementById('contact-fullname');
        const emailEl = document.getElementById('contact-email');
        const phoneEl = document.getElementById('contact-phone');
        const acceptEl = document.getElementById('accept-policies');

        let valid = true;

        function mark(el, ok) {
            if (!el) return;
            el.classList.remove('border-red-500', 'ring-1', 'ring-red-300');
            if (!ok) {
                el.classList.remove('border-[#E5E7EB]');
                el.classList.add('border-red-500', 'ring-1', 'ring-red-300');
                valid = false;
            } else {
                el.classList.add('border-[#E5E7EB]');
            }
        }

        mark(fullNameEl, fullNameEl && fullNameEl.value.trim() !== '');
        mark(emailEl, emailEl && emailEl.value.trim() !== '');
        mark(phoneEl, phoneEl && phoneEl.value.trim() !== '');

        if (acceptEl && !acceptEl.checked) {
            valid = false;
        }

        // Khoá / mở nút Continue to payment
        const step2Btn = document.getElementById('btn-step2-next');
        if (step2Btn) {
            if (valid) {
                step2Btn.disabled = false;
                step2Btn.classList.remove('opacity-60', 'cursor-not-allowed');
            } else {
                step2Btn.disabled = true;
                step2Btn.classList.add('opacity-60', 'cursor-not-allowed');
            }
        }

        return valid;
    }

    // ====== HANDLE CONFIRM BUTTON ======
    function handleConfirmClick() {
        const ok = validateContact();

        if (!ok) {
            // nếu đang ở step 3 thì kéo user về step 2 để sửa contact
            goToStep(2);
            const step2 = document.getElementById('step-2');
            if (step2) {
                step2.scrollIntoView({behavior: 'smooth', block: 'start'});
            }
            alert(
                    'Please fill in your contact details and accept the booking policies before confirming.'
            );
            return false; // chặn form submit
        }

        // ✅ đảm bảo hidden payment luôn đúng trước khi submit JSF
        syncPaymentHidden();

        // ✅ đảm bảo hidden voucher luôn đúng trước khi submit JSF
        syncVoucherHidden();

        // nếu mọi thứ ok: cho submit form để JSF lưu booking
        return true;
    }

    function setupFullMenuToggle() {
        const btn = document.getElementById('view-full-menu-btn');
        const panel = document.getElementById('full-menu-details-panel');
        if (!btn || !panel) return;

        const icon = btn.querySelector('i');

        btn.addEventListener('click', function () {
            const isHidden = panel.classList.contains('hidden');
            if (isHidden) {
                panel.classList.remove('hidden');
                if (icon) {
                    icon.style.transform = 'rotate(180deg)';
                }
            } else {
                panel.classList.add('hidden');
                if (icon) {
                    icon.style.transform = '';
                }
            }
        });
    }

    // ====== INIT ======
    function init() {
        // tạo hidden voucher field nếu booking.xhtml chưa có
        ensureVoucherHiddenFields();

        // init lucide
        if (window.lucide) {
            window.lucide.createIcons();
        }

        const params = new URLSearchParams(window.location.search);
        const restaurantId = params.get('restaurantId');
        const dateParam = params.get('date');
        const slotParam = params.get('slot');
        const packageParam = params.get('package');

        // áp dụng package (tên, badge, guest range, price)
        applyPackageFromParam(packageParam);

        // Format ngày cho hiển thị: 2025-11-19 -> 19 Nov 2025
        function formatDateDisplay(isoDate) {
            if (!isoDate) return '';
            const d = new Date(isoDate);
            if (isNaN(d.getTime())) {
                return isoDate; // nếu parse lỗi thì hiện raw
            }
            const day = String(d.getDate()).padStart(2, '0');
            const monthNames = [
                'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
            ];
            const month = monthNames[d.getMonth()];
            const year = d.getFullYear();
            return day + ' ' + month + ' ' + year;
        }

        // Nếu có date/slot truyền sang thì cập nhật UI
        if (dateParam || slotParam) {
            const eventDatetimeEl = document.getElementById('event-datetime');
            const summaryDateEl = document.getElementById('summary-event-date');
            const summarySlotEl = document.getElementById('summary-time-slot');

            const dateDisplay = dateParam ? formatDateDisplay(dateParam) : null;

            if (eventDatetimeEl) {
                if (dateDisplay && slotParam) {
                    eventDatetimeEl.textContent = dateDisplay + ' – ' + slotParam;
                } else if (dateDisplay) {
                    eventDatetimeEl.textContent = dateDisplay;
                } else if (slotParam) {
                    eventDatetimeEl.textContent = slotParam;
                }
            }

            if (summaryDateEl && dateDisplay) {
                summaryDateEl.textContent = dateDisplay;
            }

            if (summarySlotEl && slotParam) {
                summarySlotEl.textContent = slotParam;
            }
        }

        // table buttons
        const minusBtn = document.getElementById('btn-minus-table');
        const plusBtn = document.getElementById('btn-plus-table');
        const tableInput = document.getElementById('table-count-input');

        if (minusBtn && plusBtn && tableInput) {
            minusBtn.addEventListener('click', function () {
                let val = parseInt(tableInput.value || '1', 10);
                if (val > 1) val--;
                tableInput.value = String(val);
                state.tableCount = val;
                updateCapacity();
                updateSummary();
            });
            plusBtn.addEventListener('click', function () {
                let val = parseInt(tableInput.value || '1', 10);
                val++;
                tableInput.value = String(val);
                state.tableCount = val;
                updateCapacity();
                updateSummary();
            });
            tableInput.addEventListener('input', function () {
                let val = parseInt(tableInput.value || '1', 10);
                if (isNaN(val) || val < 1) val = 1;
                tableInput.value = String(val);
                state.tableCount = val;
                updateCapacity();
                updateSummary();
            });
        }

        // service level buttons
        document.querySelectorAll('.service-level-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const level = this.getAttribute('data-level');
                setServiceLevel(level);
            });
        });

        // location buttons
        document.querySelectorAll('.location-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const type = this.getAttribute('data-location');
                setLocationType(type);
            });
        });

        // addons
        document.querySelectorAll('.addon-row').forEach(row => {
            const minus = row.querySelector('.btn-addon-minus');
            const plus = row.querySelector('.btn-addon-plus');
            const qtyEl = row.querySelector('.addon-qty');

            function update(q) {
                qtyEl.textContent = String(q);
                if (minus) minus.disabled = q === 0;
                updateSummary();
            }

            if (minus) {
                minus.addEventListener('click', function () {
                    let q = parseInt(qtyEl.textContent || '0', 10);
                    if (q > 0) q--;
                    update(q);
                });
            }
            if (plus) {
                plus.addEventListener('click', function () {
                    let q = parseInt(qtyEl.textContent || '0', 10);
                    q++;
                    update(q);
                });
            }
            if (minus) minus.disabled = true;
        });

        // event type -> summary
        const eventTypeSelect = document.getElementById('event-type');
        if (eventTypeSelect) {
            eventTypeSelect.addEventListener('change', function () {
                state.eventType = this.value;

                const summary = document.getElementById('summary-event-type');
                const label = this.options[this.selectedIndex]
                        ? this.options[this.selectedIndex].text
                        : this.value;
                if (summary) summary.textContent = label;

                // đẩy tên loại tiệc (label) xuống hidden để server đọc
                const hidden = document.getElementById('hf-event-type');
                if (hidden) {
                    hidden.value = label; // ví dụ: "Wedding", "Birthday Party"
                }
            });

            const event = new Event('change');
            eventTypeSelect.dispatchEvent(event);
        }

        // tax invoice checkbox
        const needTax = document.getElementById('contact-need-tax');
        if (needTax) {
            needTax.addEventListener('change', function () {
                const box = document.getElementById('contact-need-tax-box');
                const detail = document.getElementById('tax-details');
                if (box) {
                    if (this.checked) {
                        box.classList.remove('bg-white', 'border-[#E5E7EB]');
                        box.classList.add('bg-[#D4AF37]', 'border-[#D4AF37]');
                    } else {
                        box.classList.add('bg-white', 'border-[#E5E7EB]');
                        box.classList.remove('bg-[#D4AF37]', 'border-[#D4AF37]');
                    }
                }
                if (detail) {
                    detail.classList.toggle('hidden', !this.checked);
                }
            });
        }

        // policies checkbox
        const accept = document.getElementById('accept-policies');
        if (accept) {
            accept.addEventListener('change', function () {
                const box = document.getElementById('accept-policies-box');
                if (box) {
                    if (this.checked) {
                        box.classList.remove('bg-white', 'border-[#E5E7EB]');
                        box.classList.add('bg-[#D4AF37]', 'border-[#D4AF37]');
                    } else {
                        box.classList.add('bg-white', 'border-[#E5E7EB]');
                        box.classList.remove('bg-[#D4AF37]', 'border-[#D4AF37]');
                    }
                }
                validateContact();
            });
        }

        // contact validation on input
        ['contact-fullname', 'contact-email', 'contact-phone'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.addEventListener('input', validateContact);
        });

        // step buttons
        const s1Next = document.getElementById('btn-step1-next');
        const s2Next = document.getElementById('btn-step2-next');
        const s2Back = document.getElementById('btn-step2-back');
        const s3Back = document.getElementById('btn-step3-back');

        if (s1Next) {
            s1Next.addEventListener('click', function () {
                goToStep(2);
            });
        }

        if (s2Next) {
            s2Next.addEventListener('click', function () {
                const ok = validateContact();

                if (!ok) {
                    const contactSection = document.getElementById('contact-section');
                    if (contactSection) {
                        contactSection.scrollIntoView({
                            behavior: 'smooth',
                            block: 'start'
                        });
                    }
                    alert(
                            'Please fill in your full name, email, phone number and accept the booking policies before continuing to payment.'
                    );
                    return;
                }

                goToStep(3);
                const step3 = document.getElementById('step-3');
                if (step3) {
                    step3.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            });
        }

        if (s2Back) {
            s2Back.addEventListener('click', function () {
                goToStep(1);
            });
        }
        if (s3Back) {
            s3Back.addEventListener('click', function () {
                goToStep(2);
            });
        }

        // payment method cards
        document.querySelectorAll('.payment-method').forEach(card => {
            card.addEventListener('click', function () {
                const method = this.getAttribute('data-method');
                setPaymentMethod(method);
            });
        });

        // payment type
        document.querySelectorAll('.pay-type').forEach(btn => {
            btn.addEventListener('click', function () {
                const type = this.getAttribute('data-type');
                setPaymentType(type);
            });
        });

        // voucher
const voucherInput = document.getElementById('voucher-input');
const voucherBtn = document.getElementById('btn-apply-voucher');

if (voucherBtn && voucherInput) {
    voucherBtn.addEventListener('click', function () {
const code = normalizeCode(voucherInput.value);
state.voucherCode = code;

// Clear
if (!code) {
    state.discount = 0;
    hideVoucherStatusUI();
    updateSummary();
    return;
}

// Luôn tính trên TOTAL BEFORE DISCOUNT hiện tại
const totalBefore = toNumber(state.totalBeforeDiscount || state.totalAmount || 0);

// Nếu có catalog ở client -> validate + tính theo loại voucher (PERCENT/AMOUNT)
const voucher = findVoucherByCode(code);

if (voucher) {
    const res = validateVoucherAndCalc(voucher, totalBefore);

    if (res.ok) {
        state.discount = res.discount;
        updateSummary();

        if (res.type === 'PERCENT') {
            const extra = (toNumber(res.maxDiscount) > 0) ? ` (max ${formatMoneyUSD(res.maxDiscount)})` : '';
            setVoucherStatusUI('success', `Applied ${res.value}% off${extra} → -${formatMoneyUSD(res.discount)}`);
        } else {
            setVoucherStatusUI('success', `Applied -${formatMoneyUSD(res.discount)} discount`);
        }
        return;
    }

    // Không đủ điều kiện / hết hạn / chưa tới ngày… => KHÔNG ném lỗi, chỉ báo user và không áp dụng
    state.discount = 0;
    // clear hidden voucher so user can still confirm booking without being blocked by server-side validation
    state.voucherCode = '';
    updateSummary();

    setVoucherStatusUI('warn', 'Không đủ điều kiện để sử dụng voucher');
return;
}

// Không có catalog client -> vẫn cho nhập code, server sẽ validate khi confirm
state.discount = 0;
updateSummary();
setVoucherStatusUI('warn', 'Voucher saved. Discount will be validated when you confirm.');
});
        }

        // Change venue
        const changeVenueBtn = document.getElementById('btn-change-venue');
        if (changeVenueBtn) {
            changeVenueBtn.addEventListener('click', function () {
                if (restaurantId) {
                    window.location.href =
                            'restaurant-details.xhtml?restaurantId=' +
                            encodeURIComponent(restaurantId);
                } else {
                    window.location.href = 'restaurants.xhtml';
                }
            });
        }

        // Change date & time: quay lại restaurant-details để chọn lại
        const changeDatetimeBtn = document.getElementById('btn-change-datetime');
        if (changeDatetimeBtn) {
            changeDatetimeBtn.addEventListener('click', function () {
                let targetUrl = 'restaurant-details.xhtml';
                const qs = new URLSearchParams();

                if (restaurantId) {
                    qs.set('restaurantId', restaurantId);
                } else {
                    const hiddenRestaurantId =
                            document.getElementById('hf-restaurant-id');
                    if (hiddenRestaurantId && hiddenRestaurantId.value) {
                        qs.set('restaurantId', hiddenRestaurantId.value);
                    }
                }

                if ([...qs.keys()].length > 0) {
                    targetUrl += '?' + qs.toString();
                }

                targetUrl += '#availability';
                window.location.href = targetUrl;
            });
        }

        // đọc giá custom menu từ DOM
        initMenuPriceFromDom();

        // initial render
        updateCapacity();
        updateSummary();
        validateContact();
        setLocationType('restaurant');
        updateServiceLevelPriceLabels();
        setServiceLevel('premium');
        setPaymentMethod('card');
        setPaymentType('deposit');
        setupFullMenuToggle();
        goToStep(1);
    }

    return {
        init: init,
        goToStep: goToStep,
        handleConfirmClick: handleConfirmClick
    };
})();

document.addEventListener('DOMContentLoaded', function () {
    BookingUI.init();
});
//]]>
